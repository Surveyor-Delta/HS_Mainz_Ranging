package de.hsmainz.geoinform.hsmainzranging;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to (de-)serialize logged Beacon Measurements
 *
 * @author  KekS (mailto:keks@keksfabrik.eu), 22.10.2014.
 */
public class BeaconLogObject implements Parcelable, Serializable {
    protected String            uuid;
    protected String            major;
    protected String            minor;
    protected int               distance;
    protected List<Measurement> measurements;


    /**
     * Default Constructor for a BeaconLogObject
     *
     * @param   uuid        the beacon's uuid
     * @param   major       the beacon's major version
     * @param   minor       the beacon's minor version
     * @param   distance    the nominal distance of this measurement
     */
    public BeaconLogObject(
        String uuid,
        String major,
        String minor,
        int distance
    ) {
        this.uuid = uuid;
        this.major = major;
        this.minor = minor;
        this.distance = distance;
        this.measurements = new ArrayList<>();
    }


    /**
     * Overridden method to un-{@link android.os.Parcel}
     * {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject}s
     *
     * @param   in      the {@link android.os.Parcel}
     */
    public BeaconLogObject(Parcel in) {
        this.uuid = in.readString();
        this.major = in.readString();
        this.minor = in.readString();
        this.distance = Integer.parseInt(in.readString());
        int size = Integer.parseInt(in.readString());
        this.measurements = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            measurements.add(new Measurement(
                    Long.parseLong(in.readString()),
                    Integer.parseInt(in.readString()),
                    Integer.parseInt(in.readString()),
                    Integer.parseInt(in.readString())
            ));
        }
    }


    /**
     * Add a {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject.Measurement} to this
     * {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject}s list
     * {@link #measurements} by generating it internally through it's Constructor
     * {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject.Measurement#Measurement(long, int, int, double)}
     * by chaining through {@link #addMeasurement(long, int, int, double)} at the current time
     * ({@link System#currentTimeMillis()})
     *
     * @param   rssi            received signal strength indicator
     * @param   txPower         transmit power
     * @param   calcDistance    calculated distance
     */
    public void addMeasurement(int rssi, int txPower, double calcDistance) {
        addMeasurement(new Measurement(System.currentTimeMillis(), rssi, txPower, calcDistance));
    }


    /**
     * Add a {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject.Measurement} to this
     * {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject}s list
     * {@link #measurements} by generating it internally through it's Constructor
     * {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject.Measurement#Measurement(long, int, int, double)}
     *
     * @param   timestamp       timestamp of the Measurement
     * @param   rssi            received signal strength indicator
     * @param   txPower         transmit power
     * @param   calcDistance    calculated distance
     */
    public void addMeasurement(long timestamp, int rssi, int txPower, double calcDistance) {
        addMeasurement(new Measurement(timestamp, rssi, txPower, calcDistance));
    }


    /**
     * Add a {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject.Measurement} to this
     * {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject}s list
     *
     * @param   measurement     a {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject.Measurement}
     */
    public void addMeasurement(Measurement measurement) {
        measurements.add(measurement);
    }


    /**
     * Overridden method from {@link android.os.Parcelable}
     *
     * @return  0 (lol)
     */
    @Override
    public int describeContents() {
        return 0;
    }


    /**
     * Overridden method from {@link android.os.Parcelable}
     *
     * @param   dest        the {@link android.os.Parcel} to write to
     * @param   flags       some flags (whatever)
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        List<String> data = new ArrayList<>();
        data.add(this.uuid);
        data.add(this.major);
        data.add(this.minor);
        data.add("" + this.measurements.size());
        for (Measurement m : measurements) {
            data.add(""+m.rssi);
            data.add(""+m.txPower);
            data.add(""+m.timestamp);
            data.add(""+m.calcDistance);
        }
        dest.writeStringArray(data.toArray(new String[data.size()]));
    }


    /**
     * Overridden {@link android.os.Parcelable.Creator}.
     */
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public BeaconLogObject createFromParcel(Parcel in) {
            return new BeaconLogObject(in);
        }

        public BeaconLogObject[] newArray(int size) {
            return new BeaconLogObject[size];
        }
    };


    /**
     * Class to (de-)serialize Measurements for a {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject}.
     *
     * @author Jan 'KekS' M. (mailto:keks@keksfabrik.eu), 22.10.2014.
     */
    public class Measurement {
        protected int       rssi;
        protected int       txPower;
        protected long      timestamp;
        protected double    calcDistance;

        /**
         * Default Constructor for a Measurement includes:
         *
         * @param   timestamp       timestamp of the Measurement
         * @param   rssi            received signal strength indicator
         * @param   txPower         transmit power
         * @param   calcDistance    calculated distance
         */
        public Measurement(long timestamp, int rssi, int txPower, double calcDistance) {
            this.timestamp = timestamp;
            this.rssi = rssi;
            this.txPower = txPower;
            this.calcDistance = calcDistance;
        }
    }
}
