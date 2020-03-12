package com.app.soha.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;


import com.app.soha.camera_test.MainActivity;
import com.app.soha.camera_test.R;
import com.app.soha.settings.G;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraService extends Service {
    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    protected static final String TAG = "myLog";

    private String mCameraId;
    private static CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private ImageReader imageReader;
    private DocumentFile file;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private Long delaytime;
    Timer timer = new Timer();

    @Override
    public void onCreate() {
        super.onCreate();
        G.sharedpreferences = getSharedPreferences(G.preference, Context.MODE_PRIVATE);
        delaytime = Long.valueOf(G.sharedpreferences.getString(G.CameraDelayTime, G.CameraDelayTime))* 1000;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startInForeground();
        openCamera();
        runnable.run();
        return START_STICKY;
    }

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        public void run() {
            takePicture();
            Log.d(TAG, "Runnable");
            handler.postDelayed(this, delaytime);
        }
    };

    @SuppressLint("ResourceAsColor")
    private void startInForeground() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name4);
            String description = getString(R.string.channel_description4);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("CAMERA", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // For running app in background without destroy By OS , must declier notification and Foreground for App
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "CAMERA");
        notificationBuilder.setSmallIcon(R.drawable.camera);
        int color = ContextCompat.getColor(this, R.color.locationNote);
        notificationBuilder.setColor(color);
        notificationBuilder.setContentTitle("Camera Service");
        notificationBuilder.setContentText("Tap for launch activity");
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        // Set the intent that will fire when the user taps the notification
        notificationBuilder.setContentIntent(pendingIntent);
        Notification notification = notificationBuilder.build();
        startForeground(31, notification);
    }

    public void openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        setUpCameraOutputs();
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            startBackgroundThread();
            if (manager != null) {
                manager.openCamera(mCameraId, mStateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void setUpCameraOutputs() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            assert manager != null;
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                imageReader = ImageReader.newInstance(Integer.parseInt(G.widthPic), Integer.parseInt(G.heightPic), ImageFormat.JPEG,1);
                imageReader.setOnImageAvailableListener(mOnImageAvailableListener, backgroundHandler);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "ImageAvailable");
            backgroundHandler.post(new ImageSaver(reader.acquireNextImage(), file));
        }
    };

    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
            stopBackgroundThread();
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (null != backgroundThread){
            backgroundThread.quitSafely();
        }
        try {
            if (backgroundThread != null) {
                backgroundThread.join();
            }
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createCameraCaptureSession() {
        try {
            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onReady(@NonNull CameraCaptureSession session) {
                            if (null == mCameraDevice) {
                                return;
                            }
                            if (session != null) {
                                mCaptureSession = session;
                            }
                        }

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "Configuration Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void takePicture() {
        file = getOutputMediaFile();
        try {
            if (null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.addTarget(imageReader.getSurface());
            if (G.sharedpreferences.getBoolean(G.orientation, true)) {
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(Integer.parseInt(G.sharedpreferences.getString(G.picRotation, ""))));
            }

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Log.d(TAG, file.toString());
                }
            };

            Log.d(TAG, "takePicture");
            if (mCaptureSession != null) {
                mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class ImageSaver implements Runnable {
        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final DocumentFile mFile;

        ImageSaver(Image image, DocumentFile file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            OutputStream output = null;
            try {
                output = getContentResolver().openOutputStream(mFile.getUri());
                if (output != null) {
                    output.write(bytes);
                }
                Log.d(TAG, "onImageSaved :" + mFile);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private DocumentFile getOutputMediaFile() {
        // Create a media file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return G.cameraFilePath.createFile(".jpg", "IMG_" + timeStamp + ".jpg");
    }

    @Override
    public void onDestroy() {
        closeCamera();
        timer.cancel();
        handler.removeCallbacks(runnable);
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}