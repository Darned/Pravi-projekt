package com.example.dnedic.arduinobluetoothremote;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {


    public static final String EXTRA_DEVICE_ADRESS = "device_address";
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;

    private ArrayAdapter ScannedDeviceAdapter;

    private Button btn_enableBT;
    private Button btn_disableBT;
    private Button btn_scanfordevices;
    private Button btn_send1;
    private Button btn_send2;
    private Button btn_send3;
    private Button btn_send4;
    private static final String TAG = "bluetooth2";

    private final UUID my_UUID= UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public BluetoothSocket btSocket;
    public ConnectedThread mConnectedThread;


    ListView lv_scannedDevices;
    Set <BluetoothDevice> pairedDevices;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        setupButtons();


    }

    private void setupButtons() {
        btn_enableBT = (Button) findViewById(R.id.btn_enableBT);
        btn_scanfordevices = (Button) findViewById(R.id.btn_scanfordevices);
        btn_disableBT = (Button) findViewById(R.id.btn_disableBT);
        btn_send1=(Button) findViewById(R.id.btn_send1);
        btn_send2=(Button) findViewById(R.id.btn_send2);
        btn_send3=(Button) findViewById(R.id.btn_send3);
        btn_send4=(Button) findViewById(R.id.btn_send4);
        lv_scannedDevices=(ListView) findViewById(R.id.lv_scanneddevices);

        btn_enableBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableBluetooth();
            }
        });

        btn_disableBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableBluetooth();
            }
        });

        btn_scanfordevices.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               scanforDevices();
           }
       });

        btn_send1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write("1");
            }
        });
        btn_send2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write("2");
            }
        });
        btn_send3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write("3");
            }
        });
        btn_send4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write("4");
            }
        });


        lv_scannedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                mBluetoothAdapter.cancelDiscovery();

                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length()-17);

                Log.d("Mainthread","adresa je:"+address);

               BluetoothDevice connect_device=mBluetoothAdapter.getRemoteDevice(address);

                try {
                    btSocket = createBluetoothSocket(connect_device);
                } catch (IOException e) {
                    errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
                }

                // Discovery is resource intensive.  Make sure it isn't going on
                // when you attempt to connect and pass your message.
                mBluetoothAdapter.cancelDiscovery();

                // Establish the connection.  This will block until it connects.
                Log.d(TAG, "...Connecting...");
                try {
                    btSocket.connect();
                    Log.d(TAG, "....Connection ok...");

                } catch (IOException e) {
                    try {
                        btSocket.close();
                    } catch (IOException e2) {
                        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                    }
                }

                // Create a data stream so we can talk to server.
                Log.d(TAG, "...Create Socket...");

                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();
                Toast.makeText(MainActivity.this, "Connection established to "+connect_device.getName(), Toast.LENGTH_LONG).show();
            }

        });
    }

    //Disabling Bluetooth
    private void disableBluetooth() {
        mBluetoothAdapter.disable();
        Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_LONG).show();
    }
    //Enabling Bluetooth

    private void enableBluetooth() {

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
            Toast.makeText(this, "Bluetooth is enabled!", Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(this, "Bluetooth is already on!", Toast.LENGTH_SHORT).show();
        }


    }

    //Scanning for devices
    private void scanforDevices() {
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.startDiscovery();

        ScannedDeviceAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1);

        ScannedDeviceAdapter.clear();
        //Creating Broadcast receiver
        final BroadcastReceiver mReceiver = new BroadcastReceiver() {


            @Override
            public void onReceive(Context context, Intent intent) {


                String action=intent.getAction();
                //When discovery finds a device
                if(BluetoothDevice.ACTION_FOUND.equals(action))
                {
                    //Get BluetoothDevice object from intent
                    BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //Show discovered devices in ListView
                    ScannedDeviceAdapter.add(device.getName() + "\n" + device.getAddress());
                    lv_scannedDevices.setAdapter(ScannedDeviceAdapter);
                }
            }
        };

        //Register the Broadcast Receiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver,filter);
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, my_UUID);
            } catch (Exception e) {
                Log.e("BluetoothSocket", "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(my_UUID);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mBluetoothAdapter.cancelDiscovery();
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_SHORT).show();
        finish();
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

       /*
        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }
        */
        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }
}



