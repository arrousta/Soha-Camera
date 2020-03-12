package com.app.soha.camera_test;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.app.soha.services.CameraService;
import com.app.soha.settings.G;

public class MainActivity extends AppCompatActivity {
    Button btnStartUpdates;
    Button btnStopUpdates;

    public static DocumentFile pickedDir;
    DocumentFile documentFile;
    DocumentFile cameraServiceFile;

    //Define SharedPreferences For Start & Stop Button after mainActivity Destroyed :
    SharedPreferences sharedpreferences;
    public static final String lastEstate = "estate";
    public static final String ButtonEstate = "mRequestingLocationUpdates";
    private boolean mAlreadyStartedService = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnStartUpdates = findViewById(R.id.btn_start_location_updates);
        btnStopUpdates = findViewById(R.id.btn_stop_location_updates);

        btnStartUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationButtonClick();
            }
        });
        btnStopUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocationButtonClick();
            }
        });

        sharedpreferences = getSharedPreferences(lastEstate, Context.MODE_PRIVATE);
        if (!sharedpreferences.getBoolean(ButtonEstate, true)) {
            toggleButtons();
        }

        // picRotation for Set Orientation of Capture Image :
        G.sharedpreferences = getSharedPreferences(G.preference, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = G.sharedpreferences.edit();
        int picRotation = getWindowManager().getDefaultDisplay().getRotation();
        editor.putString(G.picRotation, String.valueOf(picRotation));
        editor.apply();
    }

    public void startLocationButtonClick() {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean(ButtonEstate, false);
        editor.apply();
        toggleButtons();
        startStep1();
    }

    private void startStep1() {
        if (!mAlreadyStartedService) {
            Intent CameraService = new Intent(this, CameraService.class);
            startService(CameraService);
            mAlreadyStartedService = true;
            //Ends................................................
        }
        toggleButtons();
    }

    //stop Location Button Click
    public void stopLocationButtonClick() {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean(ButtonEstate, true);
        editor.apply();

        toggleButtons();
        mAlreadyStartedService = false;
        Intent CameraService = new Intent(this, com.app.soha.services.CameraService.class);
        stopService(CameraService);
        //Ends................................................
    }

    private void toggleButtons() {
        sharedpreferences = getSharedPreferences(lastEstate, Context.MODE_PRIVATE);
        if (sharedpreferences.getBoolean(ButtonEstate, true)) {
            btnStartUpdates.setEnabled(true);
            btnStopUpdates.setEnabled(false);
            btnStopUpdates.setBackgroundResource(R.drawable.boxoff);
            btnStartUpdates.setBackgroundResource(R.drawable.box);
        } else {
            btnStartUpdates.setEnabled(false);
            btnStopUpdates.setEnabled(true);
            btnStopUpdates.setBackgroundResource(R.drawable.box);
            btnStartUpdates.setBackgroundResource(R.drawable.boxoff);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        G.sharedpreferences = getSharedPreferences(G.preference, Context.MODE_PRIVATE);
        G.widthPic = G.sharedpreferences.getString(G.widthR, G.widthR);
        G.heightPic = G.sharedpreferences.getString(G.heightR, G.heightR);
        G.pathURI = G.sharedpreferences.getString(G.pathURIsave, G.pathURIsave);
        if (G.pathURI.length() != 0) {
            Log.d("G.pathURI: ", G.pathURI);
            onUriPermission(G.pathURI);
        }
    }

    public void onUriPermission(String Uri) {
        pickedDir = DocumentFile.fromTreeUri(this, android.net.Uri.parse(Uri));
        grantUriPermission(getPackageName(), android.net.Uri.parse(Uri), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(android.net.Uri.parse(Uri), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        Log.d("pathAddress1: ", String.valueOf(Uri));
        Log.d("pathAddress2: ", String.valueOf(pickedDir));

        documentFile = pickedDir.findFile("SohaCamera");
        if (documentFile == null) {
            documentFile = pickedDir.createDirectory("SohaCamera");  //create a new Folder: SohaCameraTest
            Log.d("documentFile1: ", String.valueOf(documentFile));
        } else {
            Log.d("documentFile2: ", String.valueOf(documentFile));
        }

        cameraServiceFile = documentFile.findFile("Pictures");
        if (cameraServiceFile == null) {
            cameraServiceFile = documentFile.createDirectory("Pictures");
            G.cameraFilePath = cameraServiceFile;
            Log.d("cameraServiceFile1: ", String.valueOf(cameraServiceFile) + "\n" + G.cameraFilePath);
        } else {
            G.cameraFilePath = cameraServiceFile;
            Log.d("cameraServiceFile2: ", cameraServiceFile + "\n" + G.cameraFilePath);
        }

        G.sharedpreferences = getSharedPreferences(G.preference, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = G.sharedpreferences.edit();
        editor.putString(G.pathURIsave, Uri);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_camera_setting) {
            Intent intent = new Intent(MainActivity.this, CameraSettingActivity.class);
            startActivity(intent);
        }
        if (item.getItemId() == R.id.action_storage_setting) {
            Intent intent = new Intent(MainActivity.this, StorageSettingActivity.class);
            startActivity(intent);
        }
        if (item.getItemId() == R.id.action_about) {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}