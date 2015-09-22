package com.dat065.projekt.Ansikdroid;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


public class Home extends ActionBarActivity {

    private Button startButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        startButton = (Button) findViewById(R.id.StartButton);
        stopButton = (Button) findViewById(R.id.StopButton);
        stopButton.setEnabled(false);

        startButton.setOnClickListener(new View.OnClickListener(

        ) {
            @Override
            public void onClick(View view) {
                if(!isMyServiceRunning(CameraService.class)){
                    startService(new Intent(Home.this, CameraService.class));
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isMyServiceRunning(CameraService.class)){
                    stopService(new Intent(Home.this, CameraService.class));
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                }
            }
        });

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
