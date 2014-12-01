package de.hsmainz.geoinform.hsmainzranging;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.distance.AndroidModel;

import java.io.Serializable;
import java.lang.reflect.Field;
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
public class    BeaconLogger
    extends     Activity
    implements  BeaconConsumer,
                SensorEventListener {

    private static final String             TAG = "BeaconLogger";
    private static final Gson               gson = new Gson();
    private double                          distance;   // distance of measurement
    private long                            interval;   // length of the interval
    private long                            logScanLengthMillis = 1100L;    // 1.1s
    private final long                      logScanWaitMillis = 0L;         // right away
    private final long                      scanLengthMillis = 10000L;      // 10s
    private final long                      scanWaitMillis = 1000L;         // 1s
    private boolean                         isLogging = false;
    private boolean                         distIsSet = false;
    private boolean                         intervalIsSet = false;
    private Region                          region;
    private List<BeaconLogObject>           beaconLogList;
    private ArrayAdapter<BeaconLogObject>   adapter;
    private SensorManager                   sm;
    private Sensor                          mGyroscope;
    private ListView                        beaconList;
    private SeekBar                         seekBar;
    private TextView                        millis;
    private EditText                        editDist;
    private EditText                        editInterval;
    private Button                          buttonStart;
    private BeaconScannerApplication        app;
    private List<float[]>                   gs;

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

        sm = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sl = sm.getSensorList(Sensor.TYPE_ALL);
        mGyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        for (Sensor sensor: sl) {
            Log.i(TAG, sensor.getType() + "\t" + sensor.getName() + " (" + sensor.getVendor() + ") v" + sensor.getVersion());
        }
        gs = new ArrayList<>();
        sm.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        this.app = (BeaconScannerApplication) this.getApplication();
        // Connecting beaconList
        beaconList = (ListView) findViewById(R.id.listBeacons);
        beaconLogList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.list_layout, beaconLogList);
        beaconList.setAdapter(adapter);
        millis = (TextView) findViewById(R.id.lblMillis);
        seekBar = (SeekBar) findViewById(R.id.scanLengthBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                millis.setText(Integer.valueOf(progresValue + 100) + getResources().getString(R.string.millis));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                logScanLengthMillis = seekBar.getProgress() + 100;
            }
        });

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
                            String timeLeft = getResources().getString(R.string.time_left);
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
                if (v.getText().length() >= 1) {
                    // ugly hack to get the keyboard to disappear
                    v.setEnabled(false);
                    v.setEnabled(true);
                    distIsSet = true;
                    distance = Double.parseDouble(v.getText().toString());
                    if (intervalIsSet) {
                        buttonStart.setEnabled(true);
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
                if (v.getText().length() >= 1) {
                    // ugly hack to get the keyboard to disappear
                    v.setEnabled(false);
                    v.setEnabled(true);
                    intervalIsSet = true;
                    interval = Integer.parseInt(v.getText().toString())*1000l;
                    if (distIsSet) {
                        buttonStart.setEnabled(true);
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
     * {@inheritDoc}
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        gs.add(event.values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
        sm.unregisterListener(this);
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
        seekBar.setEnabled(false);
        editDist.setEnabled(false);
        editInterval.setEnabled(false);
        gs.clear();
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
        seekBar.setEnabled(true);
        editDist.setEnabled(true);
        editInterval.setEnabled(true);
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
            for (BeaconLogObject blo: beaconLogList) {
                blo.done();
            }
            DeSerializerClass dsc = new DeSerializerClass(beaconLogList, distance);
            dsc.calcOrientation(gs);
            gs.clear();
            app.getFileHelper().createFile(gson.toJson(dsc), distance);
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
                    ((Object) this).notify();
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
                                    getRunningRssi(beacon),
                                    beacon.getTxPower()
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

    private class DeSerializerClass implements Serializable {
        AndroidModel            model = AndroidModel.forThisDevice();
        List<BeaconLogObject>   loggedBeacons;
        double                  distance;
        Orientation             averageOrientation;

        public DeSerializerClass(List<BeaconLogObject> bl, double distance) {
            this.loggedBeacons = bl;
            this.distance = distance;
        }

        public void calcOrientation(List<float[]> g) {
            averageOrientation = new Orientation();
            int count = 0;
            for (float[] f : g) {
                if (f.length == 3) {
                    count++;
                    averageOrientation.azimuth += f[0];
                    averageOrientation.pitch += f[1];
                    averageOrientation.roll += f[2];
                }
            }
            if (count > 1) {
                averageOrientation.azimuth /= count;
                averageOrientation.pitch /= count;
                averageOrientation.roll /= count;
            }
            averageOrientation.azimuth = Math.toDegrees(averageOrientation.azimuth);
            averageOrientation.pitch = Math.toDegrees(averageOrientation.pitch);
            averageOrientation.roll = Math.toDegrees(averageOrientation.roll);
            Log.i(TAG,
                    "Average Rotations: RotX = " + averageOrientation.roll
                    + "°, RotY = " + averageOrientation.pitch
                    + "°, RotZ = " + averageOrientation.azimuth + "°");
        }

        private class Orientation {
            /** rotation around Z axis */ double azimuth = 0.0;
            /** rotation around Y axis */ double pitch = 0.0;
            /** rotation around X axis */ double roll = 0.0;
        }
    }

    /**
     * Uses reflection to get {@link org.altbeacon.beacon.Beacon#mRunningAverageRssi} (double)
     * instead of Rssi (int)
     *
     * @param   beacon  the beacon to get the runningaverageRssi
     * @return  {@link org.altbeacon.beacon.Beacon#mRunningAverageRssi} of the beacon or
     *          {@link org.altbeacon.beacon.Beacon#getRssi()} if refection failed
     */
    private double getRunningRssi(Beacon beacon) {
        try {
            Class<?> clazz = ((Object) beacon).getClass();
            Field field = clazz.getDeclaredField("mRunningAverageRssi");
            field.setAccessible(true);
            Double rssi = (Double) field.get(beacon);
            Log.i(TAG, "mRunningAverageRssi = " + rssi);
            return rssi != null && rssi != Double.NaN && rssi > 0.0d ? rssi : 1.0 * beacon.getRssi();
        } catch (Exception ex) {
            return 1.0 * beacon.getRssi();
        }
    }
}
