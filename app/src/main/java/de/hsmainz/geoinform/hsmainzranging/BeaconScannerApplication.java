package de.hsmainz.geoinform.hsmainzranging;


import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import android.app.Application;

import de.hsmainz.geoinform.util.FileHelper;

/**
 * @author jodwyer
 * @author KekS (mailto:keks@keksfabrik.eu), 29.10.2014
 */
public class BeaconScannerApplication extends Application {

    /** global reference to {@link de.hsmainz.geoinform.util.FileHelper} instance. */
    private FileHelper              fileHelper;
    /** just has to exist to save power when in background mode. */
    @SuppressWarnings("unused")
    private BackgroundPowerSaver    backgroundPowerSaver;
    /** global reference to {@link org.altbeacon.beacon.BeaconManager} */
    private BeaconManager           beaconManager;


    @Override
    public void onCreate() {
        super.onCreate();
        fileHelper = new FileHelper(getExternalFilesDir(null));
        // Allow scanning to continue in the background.
        backgroundPowerSaver = new BackgroundPowerSaver(this);
        beaconManager = BeaconManager.getInstanceForApplication(this);
    }

    /**
     * @return  the {@link de.hsmainz.geoinform.util.FileHelper}
     */
    public FileHelper getFileHelper() {
        return this.fileHelper;
    }

    /**
     * @return  the {@link org.altbeacon.beacon.BeaconManager}
     */
    public BeaconManager getBeaconManager() {
        return beaconManager;
    }
}