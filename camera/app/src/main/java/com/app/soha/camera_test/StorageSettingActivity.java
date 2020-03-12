package com.app.soha.camera_test;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.app.soha.settings.G;

public class StorageSettingActivity extends AppCompatActivity {
    TextView openFileManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_setting);
        G.sharedpreferences = getSharedPreferences(G.preference, Context.MODE_PRIVATE);
        openFileManager = findViewById(R.id.fileManagerBtn);
    }

    public void fileManagerBtn(View view) {
        startOpenDirectory();
    }

    public void startOpenDirectory() {    // Select Storage path for App Data
        Toast.makeText(StorageSettingActivity.this , "Select Storage path for App Data" , Toast.LENGTH_LONG).show();
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 455);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode != RESULT_OK) {
            if (requestCode == 455) {
               return;
            }
        }
        if (requestCode == 455){
            G.pathURI = String.valueOf(resultData.getData());
            G.sharedpreferences = getSharedPreferences(G.preference, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = G.sharedpreferences.edit();
            editor.putString(G.pathURIsave, G.pathURI);
            editor.apply();
            Toast.makeText(StorageSettingActivity.this , "Storage Path Selected" , Toast.LENGTH_SHORT).show();
        }
    }
}