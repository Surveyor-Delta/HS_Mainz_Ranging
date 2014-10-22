package de.hsmainz.geoinform.util;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helper class with static methods to be called from the outside.
 *
 * @author  KekS (mailto:keks@keksfabrik.eu), 22.10.2014
 */
public class Utilities {


    //----------------------------------------------------
    //                  Parceling Maps
    //----------------------------------------------------
    /*
        // read map into a HashMap<String,SomeClass>
        myMap = readMap(parcel, SomeClass.class);

        // or
        myMap = new SomeMap<String, SomeClass>;
        readMap(myMap, parcel, SomeClass.class);

        // write map out
        writeMap(myMap, parcel);
     */
    /**
     * Reads a {@link java.util.Map} from a {@link android.os.Parcel} that was stored using a
     * {@link String}[] and a {@link android.os.Bundle}.
     *
     * @param   in      the {@link android.os.Parcel} to retrieve the map from
     * @param   type    the class of the {@link java.util.Map}s Objects
     * @return  a {@link java.util.Map} containing the items retrieved from the parcel
     */
    public static <V extends Parcelable> Map<String, V> readMap(Parcel in, Class<? extends V> type) {
        Map<String, V> map = new HashMap<>();
        if (in != null) {
            String[] keys = in.createStringArray();
            Bundle bundle = in.readBundle(type.getClassLoader());
            for(String key: keys) {
                map.put(key, type.cast(bundle.getParcelable(key)));
            }
        }
        return map;
    }


    /**
     * Reads into an existing Map from a Parcel that was stored using a String array and a Bundle.
     *
     * @param   map     the {@link java.util.Map}&lt;String,V&gt; that will receive the items from the parcel
     * @param   in      the {@link android.os.Parcel} to retrieve the map from
     * @param   type    the class of the {@link java.util.Map}s Objects
     */
    public static <V extends Parcelable> void readMap(Map<String, V> map, Parcel in, Class<V> type) {
        if (map != null) {
            map.clear();
            if (in != null) {
                String[] keys = in.createStringArray();
                Bundle bundle = in.readBundle(type.getClassLoader());
                for (String key: keys) {
                    map.put(key, type.cast(bundle.getParcelable(key)));
                }
            }
        }
    }


    /**
     * Writes a {@link java.util.Map} to a {@link android.os.Parcel} using a String array and a
     * {@link android.os.Bundle}.
     *
     * @param   map     the {@link java.util.Map}&lt;String,V&gt; to store in the parcel
     * @param   out     the {@link android.os.Parcel} to store the map in
     */
    public static void writeMap(Map<String, ? extends Parcelable> map, Parcel out) {
        if (map != null && map.size() > 0) {
            Set<String> keySet = map.keySet();
            Bundle b = new Bundle();
            for(String key: keySet) {
                b.putParcelable(key, map.get(key));
            }
            String[] arr = keySet.toArray(new String[keySet.size()]);
            out.writeStringArray(arr);
            out.writeBundle(b);
        } else {
            out.writeStringArray(new String[0]);
            out.writeBundle(Bundle.EMPTY);
        }
    }
}
