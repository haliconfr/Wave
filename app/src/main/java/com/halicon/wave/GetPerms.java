package com.halicon.wave;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class GetPerms extends AppCompatActivity {
    int YOUR_REQUEST_CODE = 200;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            Intent intent = new Intent(GetPerms.this, UploadScreen.class);
            startActivity(intent);
            finish();
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, YOUR_REQUEST_CODE);
            }
        }
    }
    public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == YOUR_REQUEST_CODE) {
                setContentView(R.layout.recordview);
                Intent intent = new Intent(GetPerms.this, UploadScreen.class);
                startActivity(intent);
                finish();
            }
        }else{
            setContentView(R.layout.main_app);
            Toast.makeText(GetPerms.this, "Error opening camera!", Toast.LENGTH_LONG).show();
        }
    }
}
