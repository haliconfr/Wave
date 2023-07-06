package com.halicon.wave;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

public class MainApp extends AppCompatActivity {
    ViewPager2 videosViewPage;
    List<VideoItem> videoItems;
    FirebaseFirestore db;
    String vidTitle1, vidTags1, vidUsr1, vidUrl1, vidKei;
    TextView FollowingApp;
    Button followButton;
    Button profile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        setContentView(R.layout.main_app);
        videosViewPage = findViewById(R.id.viewPagerTrend);
        FollowingApp = findViewById(R.id.followingMain);
        followButton = findViewById(R.id.followButton);
        profile = findViewById(R.id.gotoProfile);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = LoginScreen.AppContext;
                Intent newintent = new Intent();
                newintent.setClass(context,userProfile.class);
                newintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                newintent.putExtra("usernameString", user.getDisplayName());
                context.startActivity(newintent);context.startActivity(newintent);
            }
        });
        FollowingApp.setClickable(true);
        FollowingApp.setOnClickListener(v -> {
            Intent intent = new Intent(MainApp.this, FollowingMainApp.class);
            startActivity(intent);
            finish();
        });
        videoItems = new ArrayList<>();
        Button gotoUp = findViewById(R.id.gotoUpload);
        gotoUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainApp.this, GetPerms.class);
                startActivity(intent);
                finish();
            }
        });
        setVidStuff();
    }

    public void setVidStuff() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("userVidsRef")
                .orderBy("Likes", Query.Direction.DESCENDING)
                .orderBy("Date", Query.Direction.DESCENDING).startAt(date)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                vidUrl1 = document.getString("Lnk");
                                vidUsr1 = document.getString("User");
                                vidTitle1 = document.getString("Title");
                                vidTags1 = document.getString("Tags");
                                vidKei = document.getId();
                                setVideo1();
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public void setVideo1() {
        VideoItem videoItemPog = new VideoItem();
        videoItemPog.vidKei = vidKei;
        videoItemPog.videoUrl = vidUrl1;
        videoItemPog.videoTitle = vidTitle1;
        videoItemPog.videoUsr = vidUsr1;
        videoItemPog.videoTags = vidTags1;
        Log.d(TAG, vidTags1);
        videoItems.add(videoItemPog);
        videosViewPage.setAdapter(new VideosAdapter(videoItems));
    }
}
