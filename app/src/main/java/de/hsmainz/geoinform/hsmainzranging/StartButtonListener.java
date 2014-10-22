package de.hsmainz.geoinform.hsmainzranging;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * Listener for the start button.
 *
 * Created by Saufaus on 20.10.2014.
 * @author Saufaus
 * @author Jan 'KekS' M. (mailto:keks@keksfabrik.eu)
 */

public class StartButtonListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {


        Button              btn = (Button) v;
        BluetoothAdapter    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btn.getText().equals(R.string.start)) {
            // activate BT
            Log.d(StartButtonListener.class.toString(), "attempting to start BT");
            if (mBluetoothAdapter.isEnabled()) {

                Log.d(StartButtonListener.class.toString(), "START logging");
                // start logging for R.id.inIntervalLength seconds

                //Toast.makeText(btn.getContext(), "Starting", Toast.LENGTH_SHORT).show();

                btn.setText(R.string.stop);
            }
        } else { // btn should say R.string.stop
            Log.d(StartButtonListener.class.toString(), "STOP logging");
            // stop logging

            btn.setText(R.string.start);
        }
    }
}