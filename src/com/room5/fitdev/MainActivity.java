package com.room5.fitdev;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;

public class MainActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private ActivityRecognitionClient arclient;
    private PendingIntent pIntent;
    private BroadcastReceiver receiver;
    private TextView tvActivity;
    private Switch mToggleEnabled;
    private boolean mIsEnabled = false;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvActivity = (TextView) findViewById(R.id.tvActivity);
        mToggleEnabled = (Switch) findViewById(R.id.enabler_switch);
        mToggleEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mIsEnabled = isChecked;
            }
        });

        int resp = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resp == ConnectionResult.SUCCESS) {
            // Connect to the ActivityRecognitionService
            arclient = new ActivityRecognitionClient(this, this, this);
            arclient.connect();

        } else {
            Toast.makeText(this, "Please install Google Play Service.",
                    Toast.LENGTH_SHORT).show();
        }

        // Create the BroadcastReceiver that will handle activity data from the service
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Handle activity recognition intents
                if (intent.getAction().equals(
                        "com.room5.fitdev.ACTIVITY_RECOGNITION_DATA")) {

                    // If the user has enabled the ability to start other phone
                    // services for running or cycling, check the activity type and confidence.
                    if (mIsEnabled) {
                        int type = intent.getIntExtra("ActivityType", -1);
                        int confidence = intent.getIntExtra("Confidence", 0);
                        String v = "Activity: " + intent.getStringExtra("Activity")
                                + " " + "Confidence: " + confidence;
                        setStatus(v);
                        if ((type == DetectedActivity.RUNNING || type == DetectedActivity.ON_BICYCLE)
                                && confidence > 30) {
                            turnOnBluetooth();
                        }
                    }
                }

                // Handle bluetooth intents
                if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        setStatus("Bluetooth on");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        setStatus("Turning Bluetooth on...");
                        break;
                    default:
                        setStatus("Bluetooth state = " + state);
                        break;
                    }
                } else if (intent.getAction().equals(
                        BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_CONNECTION_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                    case BluetoothAdapter.STATE_CONNECTING:
                        setStatus("Bluetooth connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        setStatus("Bluetooth connected");
                        // Start Pandora once we're connected to a Bluetooth
                        // device
                        PackageManager pm = getPackageManager();
                        try {
                            String packageName = "com.pandora.android";
                            Intent launchIntent = pm
                                    .getLaunchIntentForPackage(packageName);
                            startActivity(launchIntent);
                        } catch (Exception e1) {
                            Log.e("ecooksey",
                                    "Caught exception launching Pandora: "
                                            + e1.getMessage());
                        }
                        try {
                            String packageName = "com.fitnesskeeper.runkeeper.pro";
                            Intent launchIntent = pm
                                    .getLaunchIntentForPackage(packageName);
                            startActivity(launchIntent);
                        } catch (Exception e1) {
                            Log.e("ecooksey",
                                    "Caught exception launching Runkeeper: "
                                            + e1.getMessage());
                        }
                        break;
                    default:
                        setStatus("state = " + state);
                        break;
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.room5.fitdev.ACTIVITY_RECOGNITION_DATA");
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(receiver, filter);

    }
    
    private void setStatus(String str) {
        Log.d("ecooksey", "setting status: " + str);
        String newString = (String) tvActivity.getText();
        newString += "\n" + str;
        tvActivity.setText(newString);
    }

    /**
     * Helper method to turn on bluetooth
     */
    private void turnOnBluetooth() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (arclient != null) {
            arclient.removeActivityUpdates(pIntent);
            arclient.disconnect();
        }
        unregisterReceiver(receiver);
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.d("ecooksey", "in onConnected");
        Intent intent = new Intent(this, ActivityRecognitionService.class);
        pIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        arclient.requestActivityUpdates(1000, pIntent);
    }

    @Override
    public void onDisconnected() {
    }

}
