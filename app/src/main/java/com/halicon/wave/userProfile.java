package com.halicon.wave;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class userProfile extends AppCompatActivity {
    String usersPfp, usersBio;
    FirebaseFirestore db;
    String username, vidUrl;
    ImageButton editprofile;
    Button savebutton, back;
    String currentDlLink;
    Uri pfpUri;
    TextView followers, following, error;
    EditText bio;
    GridView userVideos;
    File outputFile;
    Boolean enableback;
    List<userVids> videoItems;
    public static final int PICK_IMAGE = 1;
    protected void onCreate(Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        super.onCreate(savedInstanceState);
        videoItems = new ArrayList<>();
        userVideos = findViewById(R.id.uservideos);
        setContentView(R.layout.userprofile);
        followers = findViewById(R.id.userProfFollowers2);
        following = findViewById(R.id.userProfFollowing);
        error = findViewById(R.id.novidserror);
        error.setVisibility(View.GONE);
        Intent iin= getIntent();
        Bundle b = iin.getExtras();
        String j =(String) b.get("usernameString");
        Log.d("usr", j);
        username = j;
        profClick();
        setVidStuff();
    }
    public void profClick() {
        String userToGet = username;
                db.collection("users")
                        .document(userToGet)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    usersPfp = document.getString("pfp");
                                    usersBio = document.getString("bio");
                                    if(document.get("followers") != null){
                                        List<String> followersList = (List<String>) document.get("followers");
                                        followers.setText(String.valueOf(followersList.size()));
                                    }
                                    if(document.get("following") != null) {
                                        List<String> followingList = (List<String>) document.get("following");
                                        following.setText(String.valueOf(followingList.size()));
                                    }
                                    if(usersBio == null){
                                        usersBio = "this user hasn't written their bio yet!";
                                    }
                                    if(usersPfp != null) {
                                            new DownloadImageTask(findViewById(R.id.userPfpProf2))
                                                    .execute(usersPfp);
                                    }
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    TextView foundUsrName = findViewById(R.id.profileName2);
                                    TextView foundUsrBio = findViewById(R.id.userProfBio2);
                                    editprofile = findViewById(R.id.editProfile);
                                    if(user.getDisplayName().equals(username)){
                                        editprofile.setVisibility(View.VISIBLE);
                                        editprofile.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                editProfile();
                                            }
                                        });
                                    }
                                    foundUsrBio.setText(usersBio);
                                    foundUsrName.setText("@" + userToGet);
                                }
                            }
                        });
    }
    public void editProfile(){
        setContentView(R.layout.editprofile);
        savebutton = findViewById(R.id.savebutton);
        bio = findViewById(R.id.editBio);
        back = findViewById(R.id.backbutton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editBack();
            }
        });
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(bio.getText() != null){
                    savebutton.setClickable(true);
                }else{
                    savebutton.setClickable(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
        bio.addTextChangedListener(textWatcher);
        bio.setText(usersBio);
        savebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveChanges();
            }
        });
        savebutton.setClickable(false);
        ImageButton changePfp = findViewById(R.id.changePfp);
        if(usersPfp != null){
            new DownloadImageTask((ImageView) findViewById(R.id.changePfp))
                    .execute(usersPfp);
        }
        changePfp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGetContent.launch("image/*");
            }
        });
    }
    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    ImageButton pfpChange = findViewById(R.id.changePfp);
                    pfpChange.setImageURI(uri);
                    pfpUri = uri;
                    savebutton.setClickable(true);
                }
            });
    public void saveChanges(){
        enableback = false;
        back.setClickable(false);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(pfpUri != null){
            try {
                Context context = LoginScreen.AppContext;
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), pfpUri);
                File outputDir = context.getCacheDir();
                outputFile = File.createTempFile("prefix", ".extension", outputDir);
                FileOutputStream fos = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 1, fos);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            currentDlLink = "userPfps/" + user.getDisplayName() + "/" + "profilepicture";
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            UploadTask uploadTask = storageRef.child(currentDlLink).putFile(Uri.fromFile(outputFile));
            editprofile.setImageURI(Uri.fromFile(outputFile));
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                Task<Uri> firebaseUri = taskSnapshot.getStorage().getDownloadUrl();
                firebaseUri.addOnSuccessListener(uri -> {
                    currentDlLink = uri.toString();
                    Map<String, Object> data = new HashMap<>();
                    data.put("pfp", currentDlLink);
                    db.collection("users").document(user.getDisplayName()).set(data, SetOptions.merge());
                    enableback = true;
                    back.setClickable(true);
                });
            });
        }
        if(!bio.getText().toString().equals(usersBio)){
            if(!bio.getText().toString().isEmpty()){
                CollectionReference dc = db.collection("users");
                DocumentReference meRef = dc.document(user.getDisplayName());
                Map<String, Object> map = new HashMap<>();
                map.put("bio", bio.getText().toString());
                meRef.set(map, SetOptions.merge());
            }
        }
    }
    public void onBackPressed() {
        if(enableback){
            editBack();
        }
    }
    public void editBack(){
        Context context = LoginScreen.AppContext;
        Intent newintent = new Intent();
        newintent.setClass(context, userProfile.class);
        newintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newintent.putExtra("usernameString", username);
        context.startActivity(newintent);
    }
    public void setVidStuff() {
        db.collection("userVidsRef")
                .whereEqualTo("User", username)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                vidUrl = document.getString("Lnk");
                                setVideo1();
                            }
                        } else {
                            error.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }
    public void setVideo1() {
        userVids videoItemPog = new userVids();
        videoItemPog.videoUrl = vidUrl;
        videoItems.add(videoItemPog);
    }
}
class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;

    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        bmImage.setImageBitmap(result);
    }
}
