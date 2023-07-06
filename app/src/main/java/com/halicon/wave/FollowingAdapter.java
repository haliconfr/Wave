package com.halicon.wave;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class FollowingAdapter extends RecyclerView.Adapter<FollowingAdapter.FollowViewHolder> {
    private List<followedVids> followedList;

    public FollowingAdapter(List<followedVids> followedVidList) {
        this.followedList = followedVidList;
    }
    @NonNull
    @NotNull
    @Override
    public FollowViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new FollowViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.followcard,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull FollowViewHolder holder, int position) {
        holder.setVideoData(followedList.get(position));
    }

    @Override
    public int getItemCount() {
        return followedList.size();
    }

    static class FollowViewHolder extends RecyclerView.ViewHolder {
        private FirebaseFirestore db;
        VideoView videoView;
        VideoView loadingVid;
        TextView videoTitle, uploaderName, videoTagBox, likeCount;
        String l = "loading...";
        int ready;
        Button like;

        public FollowViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            like = itemView.findViewById(R.id.followLike);
            itemView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            videoView = itemView.findViewById(R.id.followedVid);
            loadingVid = itemView.findViewById(R.id.coverFollow);
            videoTitle = itemView.findViewById(R.id.followTitle);
            uploaderName = itemView.findViewById(R.id.followUploader);
            videoTagBox = itemView.findViewById(R.id.followVidTags);
            likeCount = itemView.findViewById(R.id.videoLikes2);
        }

        void setVideoData(followedVids videoItem) {
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
                if (videoItem.followTags != null) {
                    loadingVid.setVisibility(View.GONE);
                    startVideo(videoItem);
                }
            });
        }

        void startVideo(followedVids videoItem) {
            db = FirebaseFirestore.getInstance();
            checkifliked(videoItem);
            checkiffollowing(videoItem);
            videoTitle.setText(videoItem.followTitle);
            uploaderName.setText(videoItem.followUsr);
            if(videoItem.followTags != null){
                videoTagBox.setText(videoItem.followTags);
            }
            videoView.setVideoURI(Uri.parse(videoItem.followUrl));
            videoView.start();
            videoView.setOnCompletionListener(mp -> {
                mp.start();
                mp.setLooping(true);
                addView(videoItem);
            });
        }

        public void addView(followedVids videoItem){
                //this area is changing the view value for the video on the server
                db = FirebaseFirestore.getInstance();
                CollectionReference dc = db.collection("userVidsRef");
                DocumentReference tg = dc.document(videoItem.followKei);
                db.collection("userVidsRef")
                        .document(videoItem.followKei)
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
                            }else{
                                Log.d("Oops!", "document doesnt exist");
                            }
                        }
                    }
                });
            }
        public void checkiffollowing(followedVids videoItem) {
            Button follow = itemView.findViewById(R.id.followButton2);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String userFollow = videoItem.followUsr;
            db.collection("users")
                    .document(user.getDisplayName())
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> currentlyFollowing = (List<String>) document.get("following");
                            if(currentlyFollowing.contains(userFollow)){
                                follow.setText("unfollow");
                                follow.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {unfollowuser(videoItem);
                                    }
                                });
                            }else{
                                follow.setText("follow");
                                follow.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {followuser(videoItem);
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }
        public void followuser(followedVids videoItem) {
            Button follow = itemView.findViewById(R.id.followButton2);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String userFollow = videoItem.followUsr;
            CollectionReference cr = db.collection("users");
            DocumentReference userToFollowRef = cr.document(userFollow);
            userToFollowRef.update("followers", FieldValue.arrayUnion(user.getDisplayName()));
            CollectionReference dc = db.collection("users");
            DocumentReference meRef = dc.document(user.getDisplayName());
            meRef.update("following", FieldValue.arrayUnion(userFollow));
            follow.setText("unfollow");
            follow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {unfollowuser(videoItem);
                }
            });
        }
        public void unfollowuser(followedVids videoItem) {
            Button follow = itemView.findViewById(R.id.followButton2);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String userFollow = videoItem.followUsr;
            CollectionReference cr = db.collection("users");
            DocumentReference userToFollowRef = cr.document(userFollow);
            userToFollowRef.update("followers", FieldValue.arrayRemove(user.getDisplayName()));
            CollectionReference dc = db.collection("users");
            DocumentReference meRef = dc.document(user.getDisplayName());
            meRef.update("following", FieldValue.arrayRemove(userFollow));
            follow.setText("follow");
            follow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {followuser(videoItem);
                }
            });
        }
        public void checkifliked(followedVids videoItem) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            db.collection("userVidsRef")
                    .document(videoItem.followKei)
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
        public void add2likes(followedVids videoItem) {
            like.setClickable(false);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            CollectionReference cr = db.collection("userVidsRef");
            DocumentReference userToFollowRef = cr.document(videoItem.followKei);
            userToFollowRef.update("likedBy", FieldValue.arrayUnion(user.getDisplayName()));
            CollectionReference dc = db.collection("users");
            DocumentReference meRef = dc.document(user.getDisplayName());
            meRef.update("likes", FieldValue.arrayUnion(videoItem.followKei));
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
        public void unlike(followedVids videoItem) {
            like.setClickable(false);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            CollectionReference cr = db.collection("userVidsRef");
            DocumentReference userToFollowRef = cr.document(videoItem.followKei);
            userToFollowRef.update("likedBy", FieldValue.arrayRemove(user.getDisplayName()));
            CollectionReference dc = db.collection("users");
            DocumentReference meRef = dc.document(user.getDisplayName());
            meRef.update("likes", FieldValue.arrayRemove(videoItem.followKei));
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
        }

    }




