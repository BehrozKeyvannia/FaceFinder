package com.dat065.projekt.Ansikdroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;
import org.xmlpull.v1.XmlSerializer;


public class CameraService extends Service
        implements SurfaceHolder.Callback {

    public static final String TAG = CameraService.class.getSimpleName();

    private Camera mCamera;

    // Holds the Face Detection result:
    private Camera.Face[] mFaces;

    private Toast toastNotFound, toastFound;

    private static final int MAX_FACES = 5;

    //Timer
    private long startTimeTotal=0L, stopTimeTotal=0L,  diff=0L, watchTimeTotal = 0L, startTime = 0L, stopTime = 0L, totalOffTime;

    private boolean checkTime = true, notFirstTime = false, lastCheck = false;

    private final String XMLFilename = "/Download/XmlFiles/Result.xml";

    /**
     * Info: ---
     */

    @Override
    public void onCreate() {
        super.onCreate();
        // Create toasts
        toastNotFound = new Toast(getApplicationContext()).makeText(getApplicationContext(), "Can't recognize any faces", Toast.LENGTH_LONG);
        toastNotFound.cancel();
        toastFound = new Toast(getApplicationContext()).makeText(this, "Find faces:", Toast.LENGTH_LONG);
        toastFound.cancel();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                SystemClock.sleep(10000);
                startTimeTotal = System.currentTimeMillis();
                Toast toastStart2 = new Toast(getApplicationContext()).makeText(getApplicationContext(), "The service has started", Toast.LENGTH_SHORT);
                toastStart2.show();
                makeAndAddSurfaceView();
            }
        });
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void makeAndAddSurfaceView() {
        SurfaceView dummyView = new SurfaceView(this);

        SurfaceHolder holder = dummyView.getHolder();

        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = params.y = this.getResources().getDimensionPixelOffset(
                R.dimen.preview_surface_offset);
        wm.addView(dummyView, params);
    }

    private FaceDetectionListener faceDetectionListener = new FaceDetectionListener() {

        @Override
        public void onFaceDetection(Face[] faces, Camera camera) {
            int facesFound = faces.length;
            Log.d("onFaceDetection", "Number of Faces:" + faces.length);
            if (facesFound > 0) {
                notFirstTime = true;
                if(checkTime) {
                    checkTime = false;
                    startTime = System.currentTimeMillis();
                    lastCheck = false;
                }
                toastNotFound.cancel();
                toastFound.setText("Find faces:" + faces.length);
                toastFound.show();
            } else {
                if(notFirstTime && !lastCheck) {
                    stopTime = System.currentTimeMillis();
                    checkTime = true;
                    lastCheck = true;
                    watchTimeTotal += stopTime - startTime;
                }
                toastFound.cancel();
                toastNotFound.show();
            }
        }
    };


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera = Camera.open(1);
        mCamera.setFaceDetectionListener(faceDetectionListener);
        mCamera.startFaceDetection();
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        // We have no surface, return immediately:
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        // Try to stop the current preview:
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore...
        }
        // Get the supported preview sizes:
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = previewSizes.get(0);
        // And set them:
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.setPreviewCallback(null);
        mCamera.setFaceDetectionListener(null);
        mCamera.setErrorCallback(null);
        mCamera.release();
        mCamera = null;
    }


    @Override
    public void onDestroy() {
        calculateTime();
        saveToXMLFiles();
        toastFound.cancel();
        toastNotFound.cancel();
        Toast toastDest = new Toast(getApplicationContext()).makeText(this, "Service has stopped", Toast.LENGTH_SHORT);
        toastDest.show();
        Toast toastXml = new Toast(getApplicationContext()).makeText(this, "Result file can be find in /Downloads/XmlFiles/Result.xml", Toast.LENGTH_LONG);
        toastXml.show();
        mCamera.setPreviewCallback(null);
        mCamera.setFaceDetectionListener(null);
        mCamera.setErrorCallback(null);
        mCamera.release();
        mCamera = null;
        super.onDestroy();
    }

    private void calculateTime(){
        stopTimeTotal = System.currentTimeMillis();
        diff = stopTimeTotal - startTimeTotal;
        if(!lastCheck && notFirstTime) {
            stopTime = System.currentTimeMillis();
            watchTimeTotal += stopTime - startTime;
        }
        totalOffTime = diff - watchTimeTotal;
        Log.i(TAG, "Total elapstime: " + diff + " milliseconds");
        Log.i(TAG, "Total watch time: " + watchTimeTotal + " milliseconds");
        Log.i(TAG, "Total off time: " + totalOffTime + " milliseconds");
    }

    private void saveToXMLFiles() {
        //create a new file called "new.xml" in the SD card
        final File newXmlFile = new File(Environment.getExternalStorageDirectory() + XMLFilename);
        RandomAccessFile randomAccessFile = null;
        final boolean fileExists = newXmlFile.exists();
        String lastLine = null;

        if (fileExists) {
            try {
                randomAccessFile = new RandomAccessFile(newXmlFile, "rw");
                randomAccessFile.seek(0);

                if (null != randomAccessFile) {
                    final Scanner scanner = new Scanner(newXmlFile);
                    int lastLineOffset = 0;
                    int lastLineLength = 0;

                    while (scanner.hasNextLine()) {
                        // +1 is for end line symbol
                        lastLine = scanner.nextLine();
                        lastLineLength = lastLine.length() + 2;
                        lastLineOffset += lastLineLength;
                    }

                    // don't need last </root> line offset
                    lastLineOffset -= lastLineLength;

                    // got to string before last
                    randomAccessFile.seek(lastLineOffset);
                }
            } catch(FileNotFoundException e) {
                Log.e("FileNotFoundException", "can't create FileOutputStream");
            } catch (IOException e) {
                Log.e("IOException", "Failed to find last line");
            }
        } else {
            try {
                newXmlFile.createNewFile();
            } catch(IOException e) {
                Log.e("IOException", "exception in createNewFile() method");
            }

            try {
                randomAccessFile = new RandomAccessFile(newXmlFile, "rw");
            } catch(FileNotFoundException e) {
                Log.e("FileNotFoundException", "can't create FileOutputStream");
            }
        }

        //we create a XmlSerializer in order to write xml data
        XmlSerializer serializer = Xml.newSerializer();

        if (randomAccessFile == null) {
            return;
        }

        try {
            final StringWriter writer = new StringWriter();

            serializer.setOutput(writer);

            if (!fileExists) {
                serializer.startDocument(null, true);
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                serializer.startTag(null, "Results");
            } else {
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            }

            //Timestamp
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentTimeStamp = dateFormat.format(new Date()); // Find todays date

            serializer.startTag(null, "Result");
                serializer.attribute(null, "Time ", currentTimeStamp);
                serializer.startTag(null, "Total_elapstime");
                serializer.text(String.valueOf(diff));
                serializer.endTag(null, "Total_elapstime");
                serializer.startTag(null, "Total_watch_time");
                serializer.text(String.valueOf(watchTimeTotal));
                serializer.endTag(null, "Total_watch_time");
                serializer.startTag(null, "Total_off_time");
                serializer.text(String.valueOf(totalOffTime));
                serializer.endTag(null, "Total_off_time");
            serializer.endTag(null, "Result");

            if (!fileExists) {
                serializer.endTag(null, "Results");
            }

            serializer.flush();

            if (lastLine != null) {
                serializer.endDocument();
                writer.append(lastLine);
            }

            // Add \n just for better output in console
            randomAccessFile.writeBytes(writer.toString() + "\n");
            randomAccessFile.close();

            //Toast.makeText(getApplicationContext(), "Save!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("Exception","error occurred while creating xml file");
            e.printStackTrace();
        }
    }
}