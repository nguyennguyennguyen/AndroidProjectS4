package com.example.univgo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    private EditText userName, userProfName, userStatus, userCountry, userGender, userRelation, userDOB;
    private  String myRelationshipStatus;

    private Button updateAccountSettingsButton;
    private CircleImageView userProfImage;
    private DatabaseReference settingsUserRef;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private String myProfileImage, myUserName, myProfileName,myProfileStatus, myDOB, myCountry, myGender;
    final static int Gallery_Pick = 1;
    private ProgressDialog loadingBar;
    private StorageReference userProfileImageRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        settingsUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);
        mToolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Paramètres");
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        userProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile images");

        userName = (EditText) findViewById(R.id.settings_username);
        userProfName = (EditText) findViewById(R.id.settings_profile_full_name);
        userStatus = (EditText) findViewById(R.id.settings_status);
        userCountry = (EditText) findViewById(R.id.settings_country);
        userGender = (EditText) findViewById(R.id.settings_gender);
        userRelation = (EditText) findViewById(R.id.settings_relationship_status);
        userDOB = (EditText) findViewById(R.id.settings_dob);
        userProfImage = (CircleImageView) findViewById(R.id.settings_profile_image);
        updateAccountSettingsButton = (Button) findViewById(R.id.update_account_settings_buttons);
        loadingBar = new ProgressDialog(this);

        settingsUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    myProfileImage = dataSnapshot.child("profileimage").getValue().toString();
                    myUserName = dataSnapshot.child("username").getValue().toString();
                    myProfileName = dataSnapshot.child("fullname").getValue().toString();
                    myProfileStatus = dataSnapshot.child("status").getValue().toString();
                    myDOB = dataSnapshot.child("dob").getValue().toString();
                    myCountry = dataSnapshot.child("countryName").getValue().toString();
                    myGender = dataSnapshot.child("gender").getValue().toString();
                    myRelationshipStatus = dataSnapshot.child("relationshipStatus").getValue().toString();

                    Picasso.with(SettingsActivity.this).load(myProfileImage).placeholder(R.drawable.profile).into(userProfImage);

                    userName.setText(myUserName);
                    userProfName.setText(myProfileName);
                    userStatus.setText(myProfileStatus);
                    userCountry.setText(myCountry);
                    userGender.setText(myGender);
                    userRelation.setText(myRelationshipStatus);
                    userDOB.setText(myDOB);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        updateAccountSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ValidateAccountInfo();
            }
        });

        userProfImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, Gallery_Pick);
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Gallery_Pick && resultCode == RESULT_OK &&data != null){
            Uri imageUri = data.getData();
            CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK){

                loadingBar.setTitle("Photo de profil");
                loadingBar.setMessage("Veuillez patienter");
                loadingBar.setCanceledOnTouchOutside(true);
                loadingBar.show();

                Uri resultUri = result.getUri();

                final StorageReference filePath = userProfileImageRef.child(currentUserId + ".jpg");

                filePath.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                final String downloadUrl = uri.toString();
                                settingsUserRef.child("profileimage").setValue(downloadUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            Intent selfIntent = new Intent(SettingsActivity.this, SettingsActivity.class);
                                            startActivity(selfIntent);
                                            Toast.makeText(SettingsActivity.this, "Image Stored", Toast.LENGTH_SHORT).show();
                                            loadingBar.dismiss();
                                        }else{
                                            String message = task.getException().getMessage();
                                            Toast.makeText(SettingsActivity.this, "Error:" + message, Toast.LENGTH_SHORT).show();
                                            loadingBar.dismiss();
                                        }
                                    }
                                })          ;
                            }
                        });
                    }
                });
            }else{
                Toast.makeText(SettingsActivity.this, "Erreur : photo de profil n'était pas taillée", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }
    }
    private void ValidateAccountInfo() {

        String username = userName.getText().toString();
        String profilename = userProfName.getText().toString();
        String status = userStatus.getText().toString();
        String country = userCountry.getText().toString();
        String gender = userGender.getText().toString();
        String relation = userRelation.getText().toString();
        String dob = userDOB.getText().toString();

        if (TextUtils.isEmpty(username)){
            Toast.makeText(this,"Saisissez votre identifiant", Toast.LENGTH_SHORT).show();
        }else if (TextUtils.isEmpty(profilename)){
            Toast.makeText(this,"Saisissez votre nom", Toast.LENGTH_SHORT).show();

        }else if (TextUtils.isEmpty(status)){
            Toast.makeText(this,"Saisissez votre statut", Toast.LENGTH_SHORT).show();

        }else if (TextUtils.isEmpty(country)){
            Toast.makeText(this,"Saisissez votre pays", Toast.LENGTH_SHORT).show();

        }else if (TextUtils.isEmpty(gender)){
            Toast.makeText(this,"Saisissez votre sexe", Toast.LENGTH_SHORT).show();

        }else if (TextUtils.isEmpty(relation)){
            Toast.makeText(this,"Saisissez votre relation", Toast.LENGTH_SHORT).show();

        }else if (TextUtils.isEmpty(dob)){
            Toast.makeText(this,"Saisissez votre date de naissance", Toast.LENGTH_SHORT).show();

        }else {
            loadingBar.setTitle("Photo de profil");
            loadingBar.setMessage("Veuillez patienter");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();

            UpdateAccountInfo(username, profilename, status, dob, country, gender, relation);

        }
    }

    private void UpdateAccountInfo(String username,
                                   String profilename,
                                   String status,
                                   String dob,
                                   String country,
                                   String gender,
                                   String relation) {
        HashMap userMap = new HashMap();
        userMap.put("username", username);
        userMap.put("fullname", profilename);
        userMap.put("status", status);
        userMap.put("dob", dob);
        userMap.put("countryName", country);
        userMap.put("gender", gender);
        userMap.put("relationshipStatus", relation);
        settingsUserRef.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (task.isSuccessful()){
                    SendUserToMainActivity();
                    Toast.makeText(SettingsActivity.this, "Profil mis à jour", Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }else {
                    Toast.makeText(SettingsActivity.this, "Mis à jour du profil échoué", Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }
    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
