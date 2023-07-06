package com.halicon.wave;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideosAdapter extends RecyclerView.Adapter<VideosAdapter.VideoViewHolder> {
    private List<VideoItem> videoItems;

    public VideosAdapter(List<VideoItem> videoItems) {
        this.videoItems = videoItems;
    }

    @NonNull
    @NotNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new VideoViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.card,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull VideoViewHolder holder, int position) {
        holder.setVideoData(videoItems.get(position));
    }

    @Override
    public int getItemCount() {
        return videoItems.size();
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {
        private FirebaseFirestore db;
        VideoView videoView;
        VideoView loadingVid;
        TextView videoTitle, uploaderName, videoTagBox;
        String l = "loading...";
        int ready;
        Button like, follow;
        String userName;
        ImageView likeAnim;
        TextView likeCount;
        CountDownTimer cdt;
        int likeNo;
        String likeImage = "like";
        boolean liked;

        public VideoViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            like = itemView.findViewById(R.id.likebutton);
            itemView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            videoView = itemView.findViewById(R.id.uploadedVid);
            loadingVid = itemView.findViewById(R.id.loadingCover);
            videoTitle = itemView.findViewById(R.id.vidTitle);
            uploaderName = itemView.findViewById(R.id.uploaderName);
            videoTagBox = itemView.findViewById(R.id.tagBox);
            follow = itemView.findViewById(R.id.followButton);
            likeAnim = itemView.findViewById(R.id.likeAnim);
            likeCount = itemView.findViewById(R.id.videoLikes);
        }

        void setVideoData(VideoItem videoItem) {
            videoTitle.setText(l);
            uploaderName.setText(l);
            loadingVid.getLayoutParams().width = 1000;
            loadingVid.getLayoutParams().height = 1000;
            String videoPath = "android.resource://" + "com.raypog.wave" + "/" + R.raw.videoloading;
            Uri uri = Uri.parse(videoPath);
            loadingVid.setVideoURI(uri);
            loadingVid.start();
            ready = 0;
            loadingVid.setOnCompletionListener(mp -> {
                mp.start();
                mp.setLooping(true);
                if (videoItem.videoTags != null) {
                    loadingVid.setVisibility(View.GONE);
                    startVideo(videoItem);
                    checkifliked(videoItem);
                }
            });
        }

        void startVideo(VideoItem videoItem) {
            db = FirebaseFirestore.getInstance();
            checkiffollowing(videoItem);
            videoTitle.setText(videoItem.videoTitle);
            uploaderName.setText(videoItem.videoUsr);
            videoTagBox = itemView.findViewById(R.id.tagText);
            videoTagBox.setText(videoItem.videoTags);
            userName = (String) uploaderName.getText();
            uploaderName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Context context = LoginScreen.AppContext;
                    Intent newintent = new Intent();
                    newintent.setClass(context, userProfile.class);
                    newintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    newintent.putExtra("usernameString", userName);
                    context.startActivity(newintent);
                }
            });
            videoView.setVideoURI(Uri.parse(videoItem.videoUrl));
            videoView.start();
            videoView.setOnCompletionListener(mp -> {
                mp.start();
                mp.setLooping(true);
                addView(videoItem);
            });
        }

        public void addView(VideoItem videoItem) {
            //this area is changing the view value for the video on the server
            CollectionReference dc = db.collection("userVidsRef");
            DocumentReference tg = dc.document(videoItem.vidKei);
            db.collection("userVidsRef")
                    .document(videoItem.vidKei)
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            double currentViews = task.getResult().getDouble("Views");
                            int serverViews = (int) currentViews;
                            String newViewsStr = Integer.toString(serverViews + 1);
                            int newViews = Integer.parseInt(newViewsStr);
                            Map<String, Object> map = new HashMap<>();
                            map.put("Views", newViews);
                            tg.set(map, SetOptions.merge());
                        } else {
                            Log.d("Oops!", "document doesnt exist");
                        }
                    }
                }
            });
        }

        public void checkiffollowing(VideoItem videoItem) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String userFollow = videoItem.videoUsr;
            db.collection("users")
                    .document(user.getDisplayName())
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> currentlyFollowing = (List<String>) document.get("following");
                            if (currentlyFollowing.contains(userFollow)) {
                                follow.setText("unfollow");
                                follow.setOnClickListener(new View.OnClickListener() {
                                    @Override                                    public void onClick(View view) {
                                        unfollowuser(videoItem);
                                    }
                                });
                            } else {
                                follow.setText("follow");
                                follow.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        followuser(videoItem);
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }

        public void checkifliked(VideoItem videoItem) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            db.collection("userVidsRef")
                    .document(videoItem.vidKei)
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> likedBy = (List<String>) document.get("likedBy");
                            if (likedBy != null) {
                                likeCount.setText(String.valueOf(likedBy.size()));
                                if (likedBy.contains(user.getDisplayName())) {
                                    like.setText("unlike");
                                    like.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            unlike(videoItem);
                                        }
                                    });
                                } else {
                                    like.setText("like");
                                    like.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            add2likes(videoItem);
                                        }
                                    });
                                }
                            } else {
                                likeCount.setText("0");
                                like.setText("like");
                                like.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        add2likes(videoItem);
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }

        public void add2likes(VideoItem videoItem) {
            like.setClickable(false);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            CollectionReference cr = db.collection("userVidsRef");
            DocumentReference userToFollowRef = cr.document(videoItem.vidKei);
            userToFollowRef.update("likedBy", FieldValue.arrayUnion(user.getDisplayName()));
            CollectionReference dc = db.collection("users");
            DocumentReference meRef = dc.document(user.getDisplayName());
            meRef.update("likes", FieldValue.arrayUnion(videoItem.vidKei));
            int likes = Integer.parseInt(likeCount.getText().toString());
            int newLikes = likes + 1;
            likeCount.setText(String.valueOf(newLikes));
            like.setText("unlike");
            like.setClickable(true);
            like.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    unlike(videoItem);
                }
            });
        }

        public void unlike(VideoItem videoItem) {
            like.setClickable(false);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            CollectionReference cr = db.collection("userVidsRef");
            DocumentReference userToFollowRef = cr.document(videoItem.vidKei);
            userToFollowRef.update("likedBy", FieldValue.arrayRemove(user.getDisplayName()));
            CollectionReference dc = db.collection("users");
            DocumentReference meRef = dc.document(user.getDisplayName());
            meRef.update("likes", FieldValue.arrayRemove(videoItem.vidKei));
            like.setClickable(true);
            like.setText("like");
            int likes = Integer.parseInt(likeCount.getText().toString());
            int newLikes = likes - 1;
            likeCount.setText(String.valueOf(newLikes));
            like.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    add2likes(videoItem);
                }
            });
        }

        public void followuser(VideoItem videoItem) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String userFollow = videoItem.videoUsr;
            CollectionReference cr = db.collection("users");
            DocumentReference userToFollowRef = cr.document(userFollow);
            userToFollowRef.update("followers", FieldValue.arrayUnion(user.getDisplayName()));
            CollectionReference dc = db.collection("users");
            DocumentReference meRef = dc.document(user.getDisplayName());
            meRef.update("following", FieldValue.arrayUnion(userFollow));
            follow.setText("unfollow");
            follow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    unfollowuser(videoItem);
                }
            });
        }

        public void unfollowuser(VideoItem videoItem) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String userFollow = videoItem.videoUsr;
            CollectionReference cr = db.collection("users");
            DocumentReference userToFollowRef = cr.document(userFollow);
            userToFollowRef.update("followers", FieldValue.arrayRemove(user.getDisplayName()));
            CollectionReference dc = db.collection("users");
            DocumentReference meRef = dc.document(user.getDisplayName());
            meRef.update("following", FieldValue.arrayRemove(userFollow));
            follow.setText("follow");
            follow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    followuser(videoItem);
                }
            });
        }
    }
}


