package de.hsmainz.geoinform.hsmainzranging;

import android.bluetooth.BluetoothAdapter;
import android.view.View;
import android.widget.Button;

/**
 * Listener for the start button.
 *
 * Created by Saufaus on 20.10.2014.
 */


//TODO: ListView mit Objekten befüllen

public class StartButtonListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {

        Button b = (Button) v;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (b.getText() == "Start") {
            // BT activated
            if (mBluetoothAdapter.isEnabled()) {

                //Toast.makeText(b.getContext(), "Starting", Toast.LENGTH_SHORT).show();
                b.setText("Stop");

            }

            if (b.getText() == "Stop") {


                b.setText("Start");
            }

        }
    }
}