package de.hsmainz.geoinform.hsmainzranging;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Adapted from original code written by D Young of Radius Networks.
 *
 * @author dyoung, jodwyer
 * @author KekS (mailto:keks@keksfabrik.eu), 22.10.2014
 */
public class BeaconLogger extends Activity implements BeaconConsumer {

    private static final String             TAG = "BeaconLogger";
    private static final Gson               gson = new Gson();
    private int                             distance;   // distance of measurement
    private long                            interval;   // length of the interval
    private final long                      logScanLengthMillis = 2000L;    // 2s
    private final long                      logScanWaitMillis = 100L;       // 100ms
    private final long                      scanLengthMillis = 10000L;      // 10s
    private final long                      scanWaitMillis = 1000L;         // 1s
    private boolean                         isLogging = false;
    private boolean                         distIsSet = false;
    private boolean                         intervalIsSet = false;
    private Region                          region;
    private List<BeaconLogObject>           beaconLogList;
    private ArrayAdapter<BeaconLogObject>   adapter;
    private ListView                        beaconList;
    private EditText                        editDist;
    private EditText                        editInterval;
    private Button                          buttonStart;
    private BeaconScannerApplication        app;

// -------------------------- overrides --------------------------

    /**
     * sets up listeners and starts bluetooth
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyBluetooth();
        Log.d(TAG, "BeaconLogger created.");
        Log.v(TAG, "BLE is " + (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ? "" : "not ") + "enabled.");

        this.app = (BeaconScannerApplication) this.getApplication();
        // Connecting beaconList
        beaconList = (ListView) findViewById(R.id.listBeacons);
        beaconLogList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.list_layout, beaconLogList);
        beaconList.setAdapter(adapter);

        // Initialising 'Start' button + adding Listener
        buttonStart = (Button) findViewById(R.id.btnStart);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLogging) {
                    Log.v(TAG, "START logging");
                    // start logging for R.id.inIntervalLength seconds
                    startLogging();
                    //Toast.makeText(btn.getContext(), "Starting", Toast.LENGTH_SHORT).show();
                    buttonStart.setEnabled(false);
                    new CountDownTimer(interval, 1000) {

                        public void onTick(long millisUntilFinished) {
                            // this should work (R.string.* returns an int otherwise)
                            String limeLeft = getResources().getString(R.string.time_left);
                            buttonStart.setText((millisUntilFinished / 1000) + "s " + timeLeft);
                        }

                        public void onFinish() {
                            buttonStart.setText(R.string.again);
                            stopLogging();
                            buttonStart.setEnabled(true);
                        }
                    }.start();
                    buttonStart.setText(R.string.stop);
                } else { // btn should say R.string.stop
                    Log.v(TAG, "STOP logging");
                    // stop logging
                    stopLogging();
                    buttonStart.setText(R.string.start);
                }
            }
        });

        editDist = (EditText) findViewById(R.id.inDistance);
        editDist.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "dist entered: " + v.getText().toString() + " (" + (actionId == EditorInfo.IME_ACTION_DONE && v.getText().length() >= 1) + ")");
                if (actionId == EditorInfo.IME_ACTION_DONE && v.getText().length() >= 1) {
                    // ugly hack to get the keyboard to disappear
                    v.setEnabled(false);
                    v.setEnabled(true);
                    distIsSet = true;
                    if (intervalIsSet) {
                        buttonStart.setEnabled(true);
                        distance = Integer.parseInt(v.getText().toString());
                    }
                } else {
                    distIsSet = false;
                    buttonStart.setEnabled(false);
                }
                return distIsSet;
            }
        });

        editInterval = (EditText) findViewById(R.id.inIntervalLength);
        editInterval.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "interval entered: " + v.getText().toString() + " (" + (actionId == EditorInfo.IME_ACTION_DONE && v.getText().length() >= 1) + ")");
                if (actionId == EditorInfo.IME_ACTION_DONE && v.getText().length() >= 1) {
                    // ugly hack to get the keyboard to disappear
                    v.setEnabled(false);
                    v.setEnabled(true);
                    intervalIsSet = true;
                    if (distIsSet) {
                        buttonStart.setEnabled(true);
                        interval = Integer.parseInt(v.getText().toString())*1000l;
                    }
                } else {
                    intervalIsSet = false;
                    buttonStart.setEnabled(false);
                }
                return intervalIsSet;
            }
        });
        // Add parser for iBeacons;
        app.getBeaconManager().getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        app.getBeaconManager().bind(this);
        region = new Region("myRangingUniqueId", null, null, null);
    }

    /**
     * app put to background
     */
    @Override
    public void onStop() {
        super.onStop();
        if (isLogging) {
            stopLogging();
            isLogging = true;
        }
        stopScanning();
    }

    /**
     * app back from background
     */
    @Override
    public void onRestart() {
        super.onRestart();
        if (isLogging)
            startLogging();
        startScanning();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * app shutdown
     */
    @Override
    protected void onDestroy () {
        super.onDestroy();
        if (isLogging)
            stopLogging();
        stopScanning();
        app.getBeaconManager().unbind(this);
    }

    /**
     * beaconmanager callback, calls startScanning()
     */
    @Override
    public void onBeaconServiceConnect() {
        Log.v(TAG, "callback to onBeaconServiceConnect arrived");
        startScanning();
    }

// -------------------------- end of overrides --------------------------

    /**
     * start looking for beacons.
     */
    public void startScanning() {
        Log.v(TAG, "START scanning");
        app.getBeaconManager().setForegroundScanPeriod(scanLengthMillis);
        app.getBeaconManager().setForegroundBetweenScanPeriod(scanWaitMillis);
        app.getBeaconManager().setBackgroundScanPeriod(scanLengthMillis);
        app.getBeaconManager().setBackgroundBetweenScanPeriod(scanWaitMillis);

        //Start scanning again.
        app.getBeaconManager().setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Iterator<Beacon> beaconIterator = beacons.iterator();
                    while (beaconIterator.hasNext()) {
                        Beacon beacon = beaconIterator.next();
                        Log.v(TAG, "found beacon " + beacon.getDistance()+"m away: " + beacon.toString());
                        if (isLogging) {
                            logBeaconData(beacon);
                        }
                        addAvailableBeacon(beacon);
                    }
                }
            }
        });
        try {
            app.getBeaconManager().startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            Log.w(TAG, "problem with startRangingBeaconsInRegion", e);
        }
        Log.v(TAG, "started scanning");
    }

    /**
     * Stop looking for beacons.
     */
    public void stopScanning() {
        Log.v(TAG, "STOP scanning");
        try {
            app.getBeaconManager().stopRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            Log.w(TAG, "problem with stopRangingBeaconsInRegion", e);
        }
        Log.v(TAG, "stopped scanning");
    }


    /**
     * start logging
     */
    public void startLogging() {
        Log.v(TAG, "START logging");
        verifyBluetooth();
        app.getBeaconManager().setForegroundScanPeriod(logScanLengthMillis);
        app.getBeaconManager().setForegroundBetweenScanPeriod(logScanWaitMillis);
        app.getBeaconManager().setBackgroundScanPeriod(logScanLengthMillis);
        app.getBeaconManager().setBackgroundBetweenScanPeriod(logScanWaitMillis);
        try {
            app.getBeaconManager().updateScanPeriods();
        } catch (RemoteException e) {
            Log.w(TAG, "problem with updateScanPeriods", e);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                beaconLogList.clear();
                adapter.notifyDataSetChanged();
            }
        });
        isLogging = true;
        Log.v(TAG, "started logging");
    }

    /**
     * stop logging, write {@link #beaconLogList} to file and empty {@link #beaconLogList}
     */
    public void stopLogging() {
        Log.v(TAG, "STOP logging");
        app.getBeaconManager().setForegroundScanPeriod(scanLengthMillis);
        app.getBeaconManager().setForegroundBetweenScanPeriod(scanWaitMillis);
        app.getBeaconManager().setBackgroundScanPeriod(scanLengthMillis);
        app.getBeaconManager().setBackgroundBetweenScanPeriod(scanWaitMillis);
        try {
            app.getBeaconManager().updateScanPeriods();
        } catch (RemoteException e) {
            Log.w(TAG, "problem with updateScanPeriods", e);
        }
        isLogging = false;
        if (beaconLogList.size() > 0) {
            // Write file
            app.getFileHelper().createFile(gson.toJson(beaconLogList), distance);
            // Display file created message.
            Toast.makeText(getBaseContext(),
                    "File saved to:" + getFilesDir().getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    beaconLogList.clear();
                    adapter.notifyDataSetChanged();
                }
            });
        } else {
            // We didn't get any data, so there's no point writing an empty file.
            Toast.makeText(getBaseContext(),
                    "No data captured during scan, output file will not be created.",
                    Toast.LENGTH_SHORT).show();
        }
        Log.v(TAG, "stopped logging");
    }

    /**
     * Add available beacons to the displayed ListView
     * @param   beacon  the beacon to display
     */
    public void addAvailableBeacon(Beacon beacon) {
        BeaconLogObject.Identifier id = new BeaconLogObject.Identifier(beacon.getId1().toString(), beacon.getId2().toString(), beacon.getId3().toString());
        final BeaconLogObject blo = new BeaconLogObject(id, distance);
        Log.v(TAG, "addAvailableBeacon called for " + blo);
        if (!beaconLogList.contains(blo)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    beaconLogList.add(blo);
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    /**
     *
     * @param   beacon  the {@link org.altbeacon.beacon.Beacon} whose data to log
     */
    private void logBeaconData(Beacon beacon) {
        BeaconLogObject.Identifier id = new BeaconLogObject.Identifier(beacon.getId1().toString(), beacon.getId2().toString(), beacon.getId3().toString());
        final BeaconLogObject blo = new BeaconLogObject(id, distance);
        Log.v(TAG, "writing to logfile: " + blo);
        final Runnable run = new Runnable() {
            @Override
            public void run() {
                synchronized(this) {
                    beaconLogList.add(blo);
                    adapter.notifyDataSetChanged();
                    this.notify();
                }
            }
        };
        synchronized (run) {
            if (!beaconLogList.contains(blo)) {
                runOnUiThread(run);
                try {
                    run.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            beaconLogList.get(beaconLogList.indexOf(blo))
                    .addMeasurement(
                            new BeaconLogObject.Measurement(
                                    beacon.getRssi(),
                                    beacon.getTxPower(),
                                    beacon.getDistance()
                            )
                    );
        }
    }

    /**
     * make sure bluetooth is available or show a {@link android.widget.Toast} and shut down
     */
    private void verifyBluetooth() {
        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        System.exit(0);
                    }
                });
                builder.show();
            } else {
                Log.d(TAG, "BT verified");
            }
        }
        catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    System.exit(0);
                }

            });
            builder.show();

        }

    }

}
