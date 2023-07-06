package com.halicon.wave;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.net.Uri;
import android.widget.VideoView;


import java.util.HashMap;
import java.util.Map;

public class LoginScreen extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText email;
    private EditText password;
    private EditText username;
    public String pfpLink;
    public static Context AppContext;

    StorageReference storageRef = FirebaseStorage.getInstance().getReference();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContext = getApplicationContext();
        setContentView(R.layout.login_screen);
        mAuth = FirebaseAuth.getInstance();
        VideoView videoView = findViewById(R.id.background_view);
        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.wavesloading;
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);
        videoView.start();
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
        });
        email = findViewById(R.id.txtEmail);
        password = findViewById(R.id.txtPass);
        Button register = findViewById(R.id.createacc);
        username = findViewById(R.id.txtUsername);
        email.setText("maxtearney12@gmail.com");
        password.setText("Battlemaster11");
        register.setOnClickListener(v -> {
            String getUsername = username.getText().toString();
            String getEmail = email.getText().toString();
            String getPassword = password.getText().toString();
            if (getEmail.isEmpty()) {
                Toast.makeText(LoginScreen.this, "Email cannot be empty!", Toast.LENGTH_LONG).show();
            } else {
                if (getPassword.equals("")) {
                    Toast.makeText(LoginScreen.this, "Password cannot be empty!", Toast.LENGTH_LONG).show();
                } else {
                    if (getUsername.equals("")) {
                        Toast.makeText(LoginScreen.this, "Username cannot be empty!", Toast.LENGTH_LONG).show();
                    } else {
                        mAuth.createUserWithEmailAndPassword(getEmail, getPassword)
                                .addOnSuccessListener(authResult -> {
                                    db = FirebaseFirestore.getInstance();
                                    Toast.makeText(LoginScreen.this, "Account Created!", Toast.LENGTH_SHORT).show();
                                    setUsername();
                                })
                                .addOnFailureListener(e -> Toast.makeText(LoginScreen.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
        Button login = findViewById(R.id.signin);
        login.setOnClickListener(v1 -> {
            String getEmail = email.getText().toString();
            String getPassword = password.getText().toString();
            if (getEmail.isEmpty()) {
                Toast.makeText(LoginScreen.this, "Email cannot be empty!", Toast.LENGTH_LONG).show();
            } else {
                if (getPassword.equals("")) {
                    Toast.makeText(LoginScreen.this, "Password cannot be empty!", Toast.LENGTH_LONG).show();
                } else {
                    mAuth.signInWithEmailAndPassword(getEmail, getPassword)
                            .addOnSuccessListener(authResult -> {
                                user = FirebaseAuth.getInstance().getCurrentUser();
                                String name = user.getDisplayName();
                                Toast.makeText(LoginScreen.this, "Login to " + name + " Successful!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(LoginScreen.this, MainApp.class);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(LoginScreen.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
                }
        });
    }

    private void setUsername() {
                String getUsername = username.getText().toString();
                CollectionReference dc = db.collection("users");
                DocumentReference tg = dc.document(getUsername);
                Map<String, Object> data = new HashMap<>();
                data.put("following", null);
                tg.set(data);
                CollectionReference cr = db.collection("users");
                DocumentReference meRef = cr.document(getUsername);
                meRef.update("following", FieldValue.arrayUnion("Wave"));
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(getUsername)
                        .build();
                user.updateProfile(profileUpdates)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginScreen.this, "User Profile " + user.getDisplayName() + " Successfully Created!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(LoginScreen.this, MainApp.class);
                                startActivity(intent);
                                finish();
                            }
                        });
            }
}