package com.halicon.wave;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UploadScreen extends AppCompatActivity {
    private FirebaseFirestore db;
    private String taglist, title, dllinksng, thumbDl;
    private EditText tags, titletext;
    File mediaFile;
    public Button recButton;
    String currentdllink;
    Button post;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_screen);
        tags = findViewById(R.id.tagBox);
        titletext = findViewById(R.id.titletext);
        post = findViewById(R.id.postbutton);
        post.setEnabled(true);
        recButton = findViewById(R.id.recordVid);
        recButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotorec();
            }
        });
        post.setOnClickListener(v1 -> {
            taglist = tags.getText().toString();
            title = titletext.getText().toString();
            try {
                uploadtask();
                post.setBackgroundColor(-7829368);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        File recYet = new File(Environment.getExternalStorageDirectory() + File.separator
                + Environment.DIRECTORY_DCIM + File.separator + "yoop");
        if(recYet.exists()){
            mediaFile = new File(Environment.getExternalStorageDirectory() + File.separator
                    + Environment.DIRECTORY_DCIM + File.separator + "WAVE_output.mp4");
            startpreview();
            recYet.delete();
        }
    }

    public void gotorec() {
        Intent intent = new Intent(UploadScreen.this, RecordVideo.class);
        startActivity(intent);
        finish();
    }

    public void startpreview() {
        VideoView preview = findViewById(R.id.preview);
        preview.setVideoURI(Uri.fromFile(mediaFile));
        preview.requestFocus();
        preview.start();
        preview.getLayoutParams().width = 480;
        preview.getLayoutParams().height = 640;
        preview.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            mp.setVolume(0, 0);
        });
    }

    public void uploadtask() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = user.getDisplayName();
        currentdllink = "userVideos/" + name + "/" + System.currentTimeMillis();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        UploadTask uploadTask = storageRef.child(currentdllink).putFile(Uri.fromFile(mediaFile));
        uploadTask.addOnFailureListener(exception -> Toast.makeText(UploadScreen.this, "Oops! That didn't work. (err.upload)", Toast.LENGTH_LONG).show()).addOnSuccessListener(taskSnapshot -> {
            Toast.makeText(UploadScreen.this, "Video uploaded!", Toast.LENGTH_LONG).show();
            Task<Uri> firebaseUri = taskSnapshot.getStorage().getDownloadUrl();
            firebaseUri.addOnSuccessListener(uri -> {
                dllinksng = uri.toString();
                add2firestore();
            });
        });
    }

    public void add2firestore() {
        String tagssng = taglist;
        String kei = java.util.UUID.randomUUID().toString();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String usernamesng = user.getDisplayName();
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        Map<String, Object> data = new HashMap<>();
        data.put("Tags", tagssng);
        data.put("Lnk", dllinksng);
        data.put("User", usernamesng);
        data.put("Date", date);
        data.put("Title", title);
        data.put("Likes", 0);
        data.put("Views", 0);
        db.collection("userVidsRef").document(kei).set(data, SetOptions.merge());
        CollectionReference dc = db.collection("userVidsRef");
        DocumentReference meRef = dc.document(kei);
        meRef.update("likedBy", FieldValue.arrayUnion(null));
        Intent intent = new Intent(UploadScreen.this, MainApp.class);
        startActivity(intent);
        finish();
    }
}

