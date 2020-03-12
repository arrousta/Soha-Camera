package com.app.soha.camera_test;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.app.soha.settings.G;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class SplashActivity extends AppCompatActivity {
  public static final int RequestPermissionCode = 159;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);
    G.sharedpreferences = getSharedPreferences(G.preference, Context.MODE_PRIVATE);
    LinearLayout btn_step_next = findViewById(R.id.btn_step_next);

    btn_step_next.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startOpenDirectory();
      }
    });

    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
      // If All permission is enabled successfully then this block will execute.
      if (CheckingPermissionIsEnabledOrNot()) {
        startOpenDirectory();
      }
      // If, If permission is not enabled then else condition will execute.
      else {
        //Calling method to enable permission.
        RequestMultiplePermission();
      }
    } else {
      startOpenDirectory();
    }
  }

  private void openMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
  }

  private void RequestMultiplePermission() {
    // Creating String Array with Permissions.
    ActivityCompat.requestPermissions(this, new String[]
      {
         WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE , CAMERA
      }, RequestPermissionCode);
  }

  // Calling override method.
  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case RequestPermissionCode:
        if (grantResults.length > 0) {
          boolean writeStoragePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
          boolean readStoragePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
          boolean accessCameraPermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;
          if (writeStoragePermission && readStoragePermission && accessCameraPermission) {
            startOpenDirectory();
          } else {
            new AlertDialog.Builder(this)
              .setTitle(R.string.permissionRequired)
              .setMessage(R.string.permissionRequired_message)
              .setPositiveButton(R.string.ask_again, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                  RequestMultiplePermission();
                }
              })
              .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                  finish();
                }
              })
              .create()
              .show();
          }
        }
        break;
    }
  }

  public boolean CheckingPermissionIsEnabledOrNot() {
    int FourthPermissionResult  = ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
    int FifthPermissionResult   = ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);
    int eighthPermissionResult  = ActivityCompat.checkSelfPermission(this, CAMERA);
    return
      FourthPermissionResult == PackageManager.PERMISSION_GRANTED &&
       FifthPermissionResult == PackageManager.PERMISSION_GRANTED &&
       eighthPermissionResult == PackageManager.PERMISSION_GRANTED;
  }

  public void startOpenDirectory() {    // Select Storage path for App Data
    G.pathURI = G.sharedpreferences.getString(G.pathURIsave , "");
    if (G.pathURI.length()==0) {
    new AlertDialog.Builder(this)
            .setTitle(R.string.storageRequired)
            .setMessage(R.string.storagePermissionRequired_message)
            .setPositiveButton(R.string.open_fileManager, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                Log.d("G.pathURI: ", G.pathURI);
                Toast.makeText(SplashActivity.this , "Select Storage path for App Data" , Toast.LENGTH_LONG).show();
                startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 172);
              }
            })
            .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                finish();
              }
            })
            .create()
            .show();
          } else {
        openMainActivity();
    }
  }

  public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
    if (resultCode != RESULT_OK) {
      if (requestCode == 172) {
        startOpenDirectory();
      }
    }
    if (requestCode == 172){
      if (resultCode == RESULT_OK){
          G.pathURI = String.valueOf(resultData.getData());
          G.sharedpreferences = getSharedPreferences(G.preference, Context.MODE_PRIVATE);
          SharedPreferences.Editor editor = G.sharedpreferences.edit();
          editor.putString(G.pathURIsave, G.pathURI);
          editor.apply();
          Toast.makeText(SplashActivity.this , "Storage Path Selected" , Toast.LENGTH_SHORT).show();

          openMainActivity();
      }
    }
  }
}