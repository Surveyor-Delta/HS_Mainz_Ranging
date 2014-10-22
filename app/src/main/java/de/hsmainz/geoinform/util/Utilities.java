package de.hsmainz.geoinform.util;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Jan 'KekS' M. (mailto:keks@keksfabrik.eu), 22.10.2014.
 */
public class Utilities {

    /**
     * Reads a Map from a Parcel that was stored using a String array and a Bundle.
     *
     * @param   in      the Parcel to retrieve the map from
     * @param   type    the class used for the value objects in the map, equivalent to V.class before type erasure
     * @return  a map containing the items retrieved from the parcel
     */
    public static <V extends Parcelable> Map<String,V> readMap(Parcel in, Class<? extends V> type) {

        Map<String,V> map = new HashMap<String,V>();
        if(in != null) {
            String[] keys = in.createStringArray();
            Bundle bundle = in.readBundle(type.getClassLoader());
            for(String key : keys)
                map.put(key, type.cast(bundle.getParcelable(key)));
        }
        return map;
    }


    /**
     * Reads into an existing Map from a Parcel that was stored using a String array and a Bundle.
     *
     * @param   map     the Map<String,V> that will receive the items from the parcel
     * @param   in      the Parcel to retrieve the map from
     * @param   type    the class used for the value objects in the map, equivalent to V.class before type erasure
     */
    public static <V extends Parcelable> void readMap(Map<String,V> map, Parcel in, Class<V> type) {

        if(map != null) {
            map.clear();
            if(in != null) {
                String[] keys = in.createStringArray();
                Bundle bundle = in.readBundle(type.getClassLoader());
                for(String key : keys)
                    map.put(key, type.cast(bundle.getParcelable(key)));
            }
        }
    }


    /**
     * Writes a Map to a Parcel using a String array and a Bundle.
     *
     * @param   map     the Map<String,V> to store in the parcel
     * @param   out     the Parcel to store the map in
     */
    public static void writeMap(Map<String,? extends Parcelable> map, Parcel out) {

        if(map != null && map.size() > 0) {
            Set<String> keySet = map.keySet();
            Bundle b = new Bundle();
            for(String key : keySet)
                b.putParcelable(key, map.get(key));
            String[] array = keySet.toArray(new String[keySet.size()]);
            out.writeStringArray(array);
            out.writeBundle(b);
        }
        else {
            //String[] array = Collections.<String>emptySet().toArray(new String[0]);
            // you can use a static instance of String[0] here instead
            out.writeStringArray(new String[0]);
            out.writeBundle(Bundle.EMPTY);
        }
    }
}
