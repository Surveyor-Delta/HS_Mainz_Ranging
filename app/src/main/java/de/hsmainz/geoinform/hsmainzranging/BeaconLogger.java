package de.hsmainz.geoinform.hsmainzranging;


import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.os.RemoteException;
import android.widget.Toast;

import com.google.gson.Gson;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.hsmainz.geoinform.util.FileHelper;

/**
 * Adapted from original code written by D Young of Radius Networks.
 *
 * @author dyoung, jodwyer
 * @author KekS' (mailto:keks@keksfabrik.eu), 22.10.2014
 */
public class BeaconLogger extends Application implements BeaconConsumer {

    private static final String     TAG = BeaconLogger.class.toString();
    private static final Gson       gson = new Gson();
    private FileHelper              fileHelper;
    private BeaconManager           beaconManager;
    private BackgroundPowerSaver    backgroundPowerSaver;
    private Region                  region;
    private int                     distance;
    private int                     interval;
    private List<BeaconLogObject> beaconLogList;


    /**
     *
     * @param distance
     * @param interval
     */
    public BeaconLogger(int distance, int interval) {
        verifyBluetooth();

        fileHelper = new FileHelper(getExternalFilesDir(null));
        // Allow scanning to continue in the background.
        backgroundPowerSaver = new BackgroundPowerSaver(this);
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // Add parser for iBeacons;
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);

        region = new Region("myRangingUniqueId", null, null, null);
        this.distance = distance;
        this.interval = interval;
        beaconLogList = new ArrayList<>();
    }

    @Override
    public void onBeaconServiceConnect() {}


    /**
     * start looking for beacons.
     */
    public void startScanning() {
        beaconManager.setBackgroundBetweenScanPeriod(interval);

        //Start scanning again.
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Iterator<Beacon> beaconIterator = beacons.iterator();
                    while (beaconIterator.hasNext()) {
                        Beacon beacon = beaconIterator.next();
                        logBeaconData(beacon);
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            // TODO - OK, what now then?
        }

    }


    /**
     * Stop looking for beacons.
     */
    public void stopScanning() {
        try {
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            // TODO - OK, what now then?
        }
        if (beaconLogList.size() > 0) {
            // Write file
            fileHelper.createFile(gson.toJson(beaconLogList));
            // Display file created message.
            Toast.makeText(getBaseContext(),
                    "File saved to:" + getFilesDir().getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
        } else {
            // We didn't get any data, so there's no point writing an empty file.
            Toast.makeText(getBaseContext(),
                    "No data captured during scan, output file will not be created.",
                    Toast.LENGTH_SHORT).show();
        }
    }


    /**
     *
     * @param   beacon  the {@link org.altbeacon.beacon.Beacon} whose data to log
     */
    private void logBeaconData(Beacon beacon) {
        BeaconLogObject.Identifier id = new BeaconLogObject.Identifier(beacon.getId1().toString(), beacon.getId2().toString(), beacon.getId3().toString());
        BeaconLogObject blo = new BeaconLogObject(id, distance);
        if (!beaconLogList.contains(blo)) {
            beaconLogList.add(new BeaconLogObject(id, distance));
        }
        blo.addMeasurement(new BeaconLogObject.Measurement(beacon.getRssi(), beacon.getTxPower(), beacon.getDistance()));
    }


    /**
     *
     *
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
