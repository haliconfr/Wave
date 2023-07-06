package com.halicon.wave;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.content.Intent;
import android.graphics.Typeface;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FollowingMainApp extends AppCompatActivity {
    ViewPager2 videosViewPage;
    List<followedVids> followedItems;
    FirebaseFirestore db;
    String vidTitle1, vidTags1, vidUsr1, vidUrl1, vidKei;
    List<String> following;
    String tappedUser;
    TextView TrendingFollow;
    Button followButton;
    TextView followingUser;
    TextView error, error2;
    String lnk;
    DocumentSnapshot document;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Typeface type = Typeface.createFromAsset(getAssets(),
                "fonts/Comfortaa-Regular.ttf");
        db = FirebaseFirestore.getInstance();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.following_app);
        error = findViewById(R.id.followingTabError);
        error2 = findViewById(R.id.followingTabError2);
        error.setTypeface(type);
        error2.setTypeface(type);
        videosViewPage = findViewById(R.id.viewPagerFollowing);
        TrendingFollow = findViewById(R.id.trendingFollow);
        TrendingFollow.setClickable(true);
        TrendingFollow.setOnClickListener(v -> {
            Intent intent = new Intent(FollowingMainApp.this, MainApp.class);
            startActivity(intent);
            finish();
        });
        followedItems = new ArrayList<>();
        Button gotoUp = findViewById(R.id.gotoUpload);
        gotoUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FollowingMainApp.this, GetPerms.class);
                startActivity(intent);
                finish();
            }
        });
        setVidStuff();
    }

    public void setVidStuff() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db.collection("users")
                .document(user.getDisplayName())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    document = task.getResult();
                    if (document.exists()) {
                        if(document.get("following") == null){
                            videosViewPage.setVisibility(View.GONE);
                            error.setVisibility(View.VISIBLE);
                        }else{
                            following = (List<String>) document.get("following");
                            getVids();
                        }
                    }
                }
            }
        });
    }

    public void getVids() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("userVidsRef")
                .whereIn("User", following)
                .orderBy("Date", Query.Direction.DESCENDING).startAt(date)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                vidUrl1 = document.getString("Lnk");
                                lnk = document.getString("Lnk");
                                vidUsr1 = document.getString("User");
                                vidTitle1 = document.getString("Title");
                                vidTags1 = document.getString("Tags");
                                vidKei = document.getId();
                                if(lnk.isEmpty()){
                                    error2.setVisibility(View.VISIBLE);
                                }else{
                                    setVideo1();
                                }
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                            error2.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    public void setVideo1() {
        followedVids videoItemPog = new followedVids();
        videoItemPog.followKei = vidKei;
        videoItemPog.followUrl = vidUrl1;
        videoItemPog.followTitle = vidTitle1;
        videoItemPog.followUsr = vidUsr1;
        videoItemPog.followTags = vidTags1;
        Log.d(TAG, vidTags1);
        followedItems.add(videoItemPog);
        videosViewPage.setAdapter(new FollowingAdapter(followedItems));
    }
}
