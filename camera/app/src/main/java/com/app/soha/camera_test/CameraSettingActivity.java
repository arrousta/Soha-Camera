package com.app.soha.camera_test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.app.soha.settings.G;


public class CameraSettingActivity extends AppCompatActivity {
    static String time = "1";
    TextInputEditText edit_time;
    TextView resolution;
    TextView orientation;
    AlertDialog alertDialog;
    CharSequence[] values = {" 320 * 240"," 640 * 480","1280 * 720","1920 * 1080"};
    CharSequence[] values1 = {" Vertical "," Horizontal "};

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_setting);

        edit_time   = findViewById(R.id.edit_delaytime);
        resolution  = findViewById(R.id.resolutiontxt);
        orientation = findViewById(R.id.orientationtxt);

        G.sharedpreferences = getSharedPreferences(G.preference, Context.MODE_PRIVATE);
        resolution.setText(G.sharedpreferences.getString(G.widthR, G.widthR)+"*"+G.sharedpreferences.getString(G.heightR, G.heightR));
        if (G.sharedpreferences.getBoolean(G.orientation, true)) {
            orientation.setText(values1[0]);
        }else {
            orientation.setText(values1[1]);
        }
        edit_time.setText(G.sharedpreferences.getString(G.CameraDelayTime, G.CameraDelayTime));
    }

    public void setResolution(View view) {
        CreateAlertDialogWithRadioButtonGroup();
    }

    public void setOrientation(View view) {
        AlertDialogOrientation();
    }

    public void CreateAlertDialogWithRadioButtonGroup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CameraSettingActivity.this);
        builder.setTitle("Select Picture Size :");
        builder.setSingleChoiceItems(values, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0:
                        G.widthPic  = "320";
                        G.heightPic = "240";
                        resolution.setText(values[0]);
                        break;
                    case 1:
                        G.widthPic  = "640";
                        G.heightPic = "480";
                        resolution.setText(values[1]);
                        break;
                    case 2:
                        G.widthPic  = "1280";
                        G.heightPic = "720";
                        resolution.setText(values[2]);
                        break;
                    case 3:
                        G.widthPic  = "1920";
                        G.heightPic = "1080";
                        resolution.setText(values[3]);
                        break;
                }
                SharedPreferences.Editor editor = G.sharedpreferences.edit();
                editor.putString(G.widthR,  G.widthPic);
                editor.putString(G.heightR, G.heightPic);
                editor.apply();
                alertDialog.dismiss();
            }
        });
        alertDialog = builder.create();
        alertDialog.show();
    }
    public void AlertDialogOrientation() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(CameraSettingActivity.this);
        builder1.setTitle("Select Picture orientation :");
        builder1.setSingleChoiceItems(values1, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                SharedPreferences.Editor editor = G.sharedpreferences.edit();
                switch (item) {
                    case 0:
                        editor.putBoolean(G.orientation, true);
                        orientation.setText(values1[0]);
                        break;
                    case 1:
                        editor.putBoolean(G.orientation, false);
                        orientation.setText(values1[1]);
                        break;
                }
                editor.apply();
                alertDialog.dismiss();
            }
        });
        alertDialog = builder1.create();
        alertDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("tag","onPause");
        time = edit_time.getText().toString();
        SharedPreferences.Editor editor = G.sharedpreferences.edit();

        if (time.length() != 0) {
            editor.putString(G.CameraDelayTime, time);
            editor.apply();
            Toast.makeText(getApplicationContext(), "Data Saved", Toast.LENGTH_SHORT).show();
        }
        else {
            editor.putString(G.CameraDelayTime, "1");
            editor.commit();
        }
    }
}
