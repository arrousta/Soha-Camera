package com.app.soha.settings;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.provider.DocumentFile;

public class G extends Application {

    public static SharedPreferences sharedpreferences;
    public static final String preference = "setting";
    // Camera Setting Values
    public static final String widthR = "640";
    public static final String heightR= "480";
    public static final String picRotation = " ";
    public static final String orientation = "false";
    public static final String CameraDelayTime = "1";
    public static String widthPic = "640";
    public static String heightPic= "480";
    // App Data Path Values
    public static final String pathURIsave = null;
    public static DocumentFile cameraFilePath = null;
    public static DocumentFile logFilePath = null;
    public static String pathURI = "";

    @Override
    public void onCreate() {
        super.onCreate();
        sharedpreferences = getSharedPreferences(preference, Context.MODE_PRIVATE);
    }
}
