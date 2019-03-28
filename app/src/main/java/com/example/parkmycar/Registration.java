package com.example.parkmycar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class Registration extends AppCompatActivity {

    ImageView ImgUserPhoto;
    static int PReqCode = 1;
    static int REQUESTCODE = 1;
    Uri pickedImgUri;

    private EditText UserName,UserEmail,UserPhoneNo,UserPassword,UserPassword2;
    private ProgressBar loadingProgress;
    private Button regBtn;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        ImgUserPhoto = findViewById(R.id.regUserPhoto);

        ImgUserPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT > 22)
                {
                    checkAndRequestforPermission();
                }

                else
                {
                    openGallery();
                }


            }
        });


        UserName = findViewById(R.id.regName);
        UserEmail = findViewById(R.id.regEmail);
        UserPhoneNo = findViewById(R.id.regPhoneno);
        UserPassword = findViewById(R.id.regPassword);
        UserPassword2 = findViewById(R.id.regPassword2);
        loadingProgress = findViewById(R.id.regprogressBar);
        regBtn = findViewById(R.id.regbtn);
        loadingProgress.setVisibility(View.INVISIBLE);

        mAuth = FirebaseAuth.getInstance();

        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                regBtn.setVisibility(View.INVISIBLE);
                loadingProgress.setVisibility(View.VISIBLE);

                final String email = UserEmail.getText().toString();
                final String name = UserName.getText().toString();
                final String phoneno = UserPhoneNo.getText().toString();
                final String password = UserPassword.getText().toString();
                final String password2 = UserPassword2.getText().toString();

                if (email.isEmpty() || name.isEmpty() || phoneno.isEmpty() || password.isEmpty() || !password.equals(password2))
                {
                    showMessage("Please Verify All Fields");
                    regBtn.setVisibility(View.VISIBLE);
                    loadingProgress.setVisibility(View.INVISIBLE);
                }
                else
                {
                    // if everything is ok and all fields are filled

                    CreateUserAccount(email,name,phoneno,password);
                }
            }
        });
    }

    private void openGallery() {
    // TODO: Open Gallery intent and wait for user to pick an image

    Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
    galleryIntent.setType("image/*");
    startActivityForResult(galleryIntent,REQUESTCODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUESTCODE && data != null)
        {
            // the user has successfully picked an image
            // we need to save its reference to a URI variable
            pickedImgUri = data.getData();
            ImgUserPhoto.setImageURI(pickedImgUri);
        }

    }

    private void checkAndRequestforPermission() {

    if (ContextCompat.checkSelfPermission(Registration.this, Manifest.permission.READ_EXTERNAL_STORAGE)
    != PackageManager.PERMISSION_GRANTED)
    {
        if (ActivityCompat.shouldShowRequestPermissionRationale(Registration.this,Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            Toast.makeText(this, "Please Accept for Required Permission", Toast.LENGTH_SHORT).show();
        }

        else
        {
            ActivityCompat.requestPermissions(Registration.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PReqCode);
        }
    }
    else
    {
        openGallery();
    }
    }

    private void CreateUserAccount(String email, final String name, String phoneno, String password) {
        mAuth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful())
                    {
                        showMessage("Account Created");
                        // after successful account creation we need to update user info
                        updateUserInfo( name ,mAuth.getCurrentUser());
                    }
                    else
                    {
                        showMessage("Account Creation Failed" + task.getException().getMessage());
                        regBtn.setVisibility(View.VISIBLE);
                        loadingProgress.setVisibility(View.INVISIBLE);
                    }
                }
            });
    }
    // Update User data such as name
    private void updateUserInfo(final String name, final FirebaseUser currentUser) {

        StorageReference mStorage = FirebaseStorage.getInstance().getReference().child("users_photos");
        final StorageReference imageFilePath = mStorage.child(pickedImgUri.getLastPathSegment());
        imageFilePath.putFile(pickedImgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                // image uploaded successfully
                // now we can get our image url

                imageFilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        // uri contains image URL

                        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .setPhotoUri(uri)
                                .build();
                        currentUser.updateProfile(profileUpdate)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful())
                                        {
                                            // user info updated successfully
                                            showMessage("Registration Successful");
                                            updateUI();
                                        }
                                    }
                                });
                    }
                });
            }
        });
    }

    private void updateUI() {

        Intent HomeActivity = new Intent(this,MainActivity.class);
        startActivity(HomeActivity);


    }

    // Simple Message to Show Toast Message
    private void showMessage(String message) {

        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }


    public void loginpage(View a)
    {
        Intent PreviousPage = new Intent(this,MainActivity.class);
        startActivity(PreviousPage);
    }
@Override
    public void onBackPressed(){
        super.onBackPressed();
        Intent PreviousPage = new Intent(this,MainActivity.class);
        startActivity(PreviousPage);
    }
}
