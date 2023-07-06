package com.halicon.wave;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecordVideo extends AppCompatActivity implements SurfaceHolder.Callback, Handler.Callback {
    static final String TAG = "CamTest";
    static final int MY_PERMISSIONS_REQUEST_CAMERA = 1242;
    private static final int MSG_CAMERA_OPENED = 1;
    private static final int MSG_SURFACE_READY = 2;
    private final Handler mHandler = new Handler(this);
    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    CameraManager mCameraManager;
    String[] mCameraIDsList;
    CameraDevice.StateCallback mCameraStateCB;
    CameraDevice mCameraDevice;
    CameraCaptureSession mCaptureSession;
    boolean mSurfaceCreated = true;
    boolean mIsCameraConfigured = false;
    private Surface mCameraSurface = null;
    ProgressBar prog;
    MediaRecorder recorder;
    boolean started;
    File mediaFile;
    ImageButton Rec;
    SurfaceView prev;
    Camera cam;
    SurfaceTexture sft;
    Surface sf;
    Boolean done;
    int tenSec= 9*1000;
    long timeRemaining;
    Double currentRecTime;
    public int TotalRecordedTime = 0;
    CountDownTimer cdt;
    int videoNo;
    ImageView recBg;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        done = false;
        recorder = new MediaRecorder();
        sft = new SurfaceTexture(0);
        sf = new Surface(sft);
        recorder.setPreviewDisplay(sf);
        recorder.reset();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recordview);
        currentRecTime = 0.0;
        prog = findViewById(R.id.recordProgress);
        cam = openBackFacingCamera();
        if (cam == null) {
            try {
                throw new Exception("cam is null");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        started = false;
        recBg = findViewById(R.id.recBg);
        Rec = findViewById(R.id.btn_recording);
        prev = findViewById(R.id.cameraPreview);
        Rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!started) {
                    try {
                        Rec.animate().scaleY(1.2F).setDuration(100);
                        Rec.animate().scaleX(1.2F).setDuration(100);
                        recBg.animate().scaleY(1.6F).setDuration(500);
                        recBg.animate().scaleX(1.6F).setDuration(500).setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animator) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animator) {
                                recBg.animate().scaleY(1.5F).setDuration(300).setListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animator) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animator) {

                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animator) {

                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animator) {

                                    }
                                });
                                recBg.animate().scaleX(1.5F).setDuration(300);
                                Rec.animate().scaleY(1F).setDuration(100);
                                Rec.animate().scaleX(1F).setDuration(100);
                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animator) {

                            }
                        });
                        setupRecorder();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Rec.animate().scaleY(1F).setDuration(100);
                    Rec.animate().scaleX(1F).setDuration(100);
                    recBg.animate().scaleY(1F).setDuration(500);
                    recBg.animate().scaleX(1F).setDuration(500);
                    stoprecording();
                }
            }
        });
        this.mSurfaceView = prev;
        this.mSurfaceHolder = this.mSurfaceView.getHolder();
        this.mSurfaceHolder.addCallback(this);
        this.mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

        try {
            mCameraIDsList = this.mCameraManager.getCameraIdList();
            for (String id : mCameraIDsList) {
                Log.v(TAG, "CameraID: " + id);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mCameraStateCB = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                Toast.makeText(getApplicationContext(), "onOpened", Toast.LENGTH_SHORT).show();

                mCameraDevice = camera;
                mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                Toast.makeText(getApplicationContext(), "onDisconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Toast.makeText(getApplicationContext(), "onError", Toast.LENGTH_SHORT).show();
            }
        };
        recorder=new MediaRecorder();
        mediaFile = new File(Environment.getExternalStorageDirectory() + File.separator
                + Environment.DIRECTORY_DCIM + File.separator + "WAVE_output.mp4");
        if (!mediaFile.exists()) {
            try {
                mediaFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            mediaFile.delete();
            try {
                mediaFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cam.stopPreview();
        cam.unlock();
        recorder.setCamera(cam);
        recorder.setOrientationHint(90);
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        camcorderProfile.videoFrameWidth = 640;
        camcorderProfile.videoFrameHeight = 480;
        camcorderProfile.videoFrameRate = 30;
        camcorderProfile.videoBitRate = 2500000;
        camcorderProfile.videoCodec = MediaRecorder.VideoEncoder.H264;
        camcorderProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
        recorder.setProfile(camcorderProfile);
        recorder.setOutputFile(mediaFile + "");
        recorder.setPreviewDisplay(sf);
    }

    public void setupRecorder(){
        if(!done){
            try {
                recorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            recorder.start();
            started = true;
            Log.d(TAG, "recorder has started");
            startProg();
            done = true;
        }else{
            recorder.resume();
            started = true;
            startProg();
        }

    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onStart() {
        super.onStart();
        try {
            mCameraManager.openCamera(mCameraIDsList[1], mCameraStateCB, new Handler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (mCaptureSession != null) {
                mCaptureSession.stopRepeating();
                mCaptureSession.close();
                mCaptureSession = null;
            }

            mIsCameraConfigured = false;
        } catch (final CameraAccessException e) {
            //skill issue who cares
            e.printStackTrace();
        } catch (final IllegalStateException e2) {
            //skill issue who cares
            e2.printStackTrace();
        } finally {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
                mCaptureSession = null;
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_CAMERA_OPENED:
            case MSG_SURFACE_READY:
                if (mSurfaceCreated && (mCameraDevice != null)
                        && !mIsCameraConfigured) {
                    configureCamera();
                }
                break;
        }

        return true;
    }

    private void configureCamera() {
        List<Surface> sfl = new ArrayList<Surface>();

        sfl.add(mCameraSurface);
        try {
            mCameraDevice.createCaptureSession(sfl,
                    new RecordVideo.CaptureSessionListener(), null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mIsCameraConfigured = true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                try {
                    mCameraManager.openCamera(mCameraIDsList[1], mCameraStateCB, new Handler());
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCameraSurface = holder.getSurface();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCameraSurface = holder.getSurface();
        mSurfaceCreated = true;
        mHandler.sendEmptyMessage(MSG_SURFACE_READY);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        sf.release();
        sft.release();
        mSurfaceCreated = false;
    }

    private class CaptureSessionListener extends
            CameraCaptureSession.StateCallback {
        @Override
        public void onConfigureFailed(final CameraCaptureSession session) {
            Log.d(TAG, "CaptureSessionConfigure failed");
        }

        @Override
        public void onConfigured(final CameraCaptureSession session) {
            Log.d(TAG, "CaptureSessionConfigure onConfigured");
            mCaptureSession = session;

            try {
                CaptureRequest.Builder previewRequestBuilder = mCameraDevice
                        .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(mCameraSurface);
                mCaptureSession.setRepeatingRequest(previewRequestBuilder.build(),
                        null, null);
            } catch (CameraAccessException e) {
                Log.d(TAG, "setting up preview failed");
                e.printStackTrace();
            }
        }
    }

    public android.hardware.Camera openBackFacingCamera() {
        int cameraCount;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        Log.i(TAG, "camera count" + cameraCount);
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);

            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Log.i(TAG, "camera front open");
                try {
                    Camera c = Camera.open(camIdx);
                    return c;
                } catch (RuntimeException e) {
                    Log.i(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }

        }
        return null;
    }

    public void stoprecording() {
        cdt.cancel();
        recorder.pause();
        started = false;
        Log.d(TAG, "recorder has stopped");
        videoNo = videoNo + 1;
    }

    public void startProg(){
        timeRemaining = 10000;
        cdt = new CountDownTimer(timeRemaining, 50) {
            public void onTick(long millisUntilFinished) {
                TotalRecordedTime+=50;
                prog.setProgress(Math.min(100,(int) (((float) TotalRecordedTime / (float) tenSec) * 100)));
                Boolean finished = false;
                if(prog.getProgress() == 90){
                    if(!finished){
                        cdt.cancel();
                        recorder.stop();
                        recorder.reset();
                        recorder.release();
                        cam.lock();
                        cam.release();
                        File recYet = new File(Environment.getExternalStorageDirectory() + File.separator
                                + Environment.DIRECTORY_DCIM + File.separator + "yoop");
                        try {
                            recYet.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(RecordVideo.this, UploadScreen.class);
                        startActivity(intent);
                    }
                }
            }

            public void onFinish() {

            }
        };
        cdt.start();
    }
}