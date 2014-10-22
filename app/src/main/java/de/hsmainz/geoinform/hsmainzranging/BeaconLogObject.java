package de.hsmainz.geoinform.hsmainzranging;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to (de-)serialize logged Beacon Measurements
 *
 * @author  KekS (mailto:keks@keksfabrik.eu), 22.10.2014.
 */
public class BeaconLogObject implements Parcelable, Serializable, Comparable<BeaconLogObject> {

    private Identifier        beaconId;
    private int               distance;
    private List<Measurement> measurements;


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
        this(new Identifier(uuid, major, minor), distance);
    }


    /**
     * Default Constructor for a BeaconLogObject
     *
     * @param   beaconId    the beacon identified by this {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject.Identifier}
     * @param   distance    the nominal distance of this measurement
     */
    public BeaconLogObject(
            Identifier beaconId,
            int distance
    ) {
        this.beaconId = beaconId;
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
        this.beaconId = new Identifier(in.readString(), in.readString(), in.readString());
        this.distance = Integer.parseInt(in.readString());
        int size = Integer.parseInt(in.readString());
        this.measurements = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            getMeasurements().add(new Measurement(
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
        getMeasurements().add(measurement);
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
        data.add(this.beaconId.getUuid());
        data.add(this.beaconId.getMajor());
        data.add(this.beaconId.getMinor());
        data.add("" + this.getMeasurements().size());
        for (Measurement m : getMeasurements()) {
            data.add(""+ m.getRssi());
            data.add(""+ m.getTxPower());
            data.add(""+ m.getTimestamp());
            data.add(""+ m.getCalcDistance());
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
     * Getter for {@link #distance}
     *
     * @return  the fixed distance this logging happend at
     */
    public int getDistance() {
        return distance;
    }

    /**
     * Getter for {@link #measurements}
     *
     * @return  a {@link java.util.List} of all {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject.Measurement}s so far
     */
    public List<Measurement> getMeasurements() {
        return measurements;
    }

    /**
     * Compares this object to the specified object to determine their relative
     * order.
     *
     * @param   another     the object to compare to this instance.
     * @return  a negative integer if this instance is less than {@code another};
     *          a positive integer if this instance is greater than
     *          {@code another}; 0 if this instance has the same order as
     *          {@code another}.
     * @throws  ClassCastException  if {@code another} cannot be converted into something
     *                              comparable to {@code this} instance.
     */
    @Override
    public int compareTo(@NotNull BeaconLogObject another) {
        return this.beaconId.compareTo(another.beaconId)*1000
                + this.distance - another.getDistance();
    }


    /**
     * Uniquely identified Beacon.
     *
     * @author  KekS (mailto:keks@keksfabrik.eu), 22.10.2014.
     */
    public static class Identifier implements Comparable<Identifier> {

        private String uuid;
        private String major;
        private String minor;

        /**
         * Constructor for an individually defined Beacon.
         *
         * @param   uuid    the beacon's uuid
         * @param   major   the beacon's major version
         * @param   minor   the beacon's minor version
         */
        public Identifier(String uuid, String major, String minor) {
            this.uuid = uuid;
            this.major = major;
            this.minor = minor;
        }

        /**
         * Getter for {@link #uuid}
         *
         * @return  the universally unique identifier
         */
        public String getUuid() {
            return uuid;
        }

        /**
         * Getter for {@link #major}
         *
         * @return  the major version
         */
        public String getMajor() {
            return major;
        }

        /**
         * Getter for {@link #minor}
         *
         * @return  the major
         */
        public String getMinor() {
            return minor;
        }

        /**
         * Compares this object to the specified object to determine their relative
         * order.
         *
         * @param   other   the object to compare to this instance.
         * @return  a negative integer if this instance is less than {@code another};
         *          a positive integer if this instance is greater than
         *          {@code another}; 0 if this instance has the same order as
         *          {@code another}.
         * @throws  ClassCastException  if {@code another} cannot be converted into something
         *                              comparable to {@code this} instance.
         */
        @Override
        public int compareTo(@NotNull Identifier other) {
            return  this.uuid.compareTo(other.getUuid())*1000
                    + this.getMajor().compareTo(other.getMajor())*100
                    + this.getMinor().compareTo(other.getMinor());
        }
    }

    /**
     * Class to (de-)serialize Measurements for a {@link de.hsmainz.geoinform.hsmainzranging.BeaconLogObject}.
     *
     * @author  KekS (mailto:keks@keksfabrik.eu), 22.10.2014.
     */
    public static class Measurement {

        private int       rssi;
        private int       txPower;
        private long      timestamp;
        private double    calcDistance;

        /**
         * Default Constructor for a Measurement without external timestamp.
         *
         * @param   rssi            received signal strength indicator
         * @param   txPower         transmit power
         * @param   calcDistance    calculated distance
         */
        public Measurement(int rssi, int txPower, double calcDistance) {
            this(System.currentTimeMillis(), rssi, txPower, calcDistance);
        }

        /**
         * Default Constructor for a Measurement with external timestamp.
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

        /**
         * Getter for {@link #rssi}
         *
         * @return  received signal strength indicator
         */
        public int getRssi() {
            return rssi;
        }

        /**
         * Getter for {@link #txPower}
         *
         * @return  transmit power
         */
        public int getTxPower() {
            return txPower;
        }

        /**
         * Getter for {@link #timestamp}
         *
         * @return  the timestamp
         */
        public long getTimestamp() {
            return timestamp;
        }

        /**
         * Getter for {@link #getCalcDistance()}
         *
         * @return  the calculated proximity of a beacon
         */
        public double getCalcDistance() {
            return calcDistance;
        }

        /**
         * Converts the Radius Networks beacon proximity value passed in and returns an
         * appropriate human readable String.
         *
         * @return  human readable String representing the proximity of this measurement
         */
        public String getProximityString() {
            return getProximityString(calcDistance);
        }

        /**
         * Converts the Radius Networks beacon proximity value passed in and returns an
         * appropriate human readable String.
         * @param   proximity   double value expressing proximity in metres from Radius Networks beacon class
         * @return  human readable String representing that proximity
         */
        public static String getProximityString(double proximity) {
            String proximityString;
            if (proximity == -1.0) {
                // -1.0 is passed back by the SDK to indicate an unknown distance
                proximityString = "Unknown";
            } else if (proximity < 0.5) {
                proximityString = "Immediate";
            } else if (proximity < 2.0) {
                proximityString = "Near";
            } else {
                proximityString = "Far";
            }
            return proximityString;
        }
    }
}
