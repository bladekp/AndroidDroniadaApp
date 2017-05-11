package rtandroid.main;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;

import org.w3c.dom.Text;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.zip.CRC32;

import rtandroid.main.serial.SerialInterface;

/**
 * Main app activity, user interface not needed. Just background tasks.
 *
 * Connect to UART from linux's screen: sudo screen /dev/ttyUSB0 9600
 * UART configuration:
 * GND: pin 6
 * TX: pin 8
 * RX: pin 10
 *
 * Looking for beacon UUID:  3a49d7d0-d7cf-4946-8f3f-bd6e74219b5d
 *
 * TODO: after some time error in app appear:
 * 04-22 17:25:00.484 13014-13026/rtandroid.main D/BluetoothAdapter: onBluetoothServiceDown: android.bluetooth.IBluetooth$Stub$Proxy@e3e1a43
 * 04-22 17:25:01.602 13014-13027/rtandroid.main D/BluetoothAdapter: onBluetoothServiceDown: null
 * 04-22 17:25:02.204 13014-13026/rtandroid.main D/BluetoothAdapter: onBluetoothServiceUp: android.bluetooth.IBluetooth$Stub$Proxy@f43c8c0
 * 04-22 17:25:10.347 13014-13027/rtandroid.main D/BluetoothAdapter: onBluetoothServiceDown: android.bluetooth.IBluetooth$Stub$Proxy@f43c8c0
 * 04-22 17:25:10.844 13014-13026/rtandroid.main D/BluetoothAdapter: onBluetoothServiceUp: android.bluetooth.IBluetooth$Stub$Proxy@f1e179f
 *
 * TODO: application stops broadcasting after some time, may it be something like:
 * http://fabcirablog.weebly.com/blog/creating-a-never-ending-background-service-in-android
 */

public class MainActivity extends Activity
{
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int REQUEST_CODE_PERMISSIONS = 100;
    private ProximityManager proximityManager;
    private TableLayout tableLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try { Log.d(TAG, "RTAndroid: " + rtandroid.BuildInfo.getVersion()); }
        catch (Exception e) { Log.e(TAG, "Failed to get version", e); }

        tableLayout = (TableLayout)findViewById(R.id.tableLayout);

        checkPermissions();

        //Initialize and configure proximity manager
        setupProximityManager();

        startScanning();

        SerialInterface.initialize();

    }

    //Since Android Marshmallow starting a Bluetooth Low Energy scan requires permission from location group.
    private void checkPermissions() {
        int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermissionResult) {
            //Permission not granted so we ask for it. Results are handled in onRequestPermissionsResult() callback.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (REQUEST_CODE_PERMISSIONS == requestCode) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Location permissions are mandatory to use BLE features on Android 6.0 or higher", Toast.LENGTH_LONG).show();
        }
    }

    private void setupProximityManager() {
        proximityManager = ProximityManagerFactory.create(this);

        //Configure proximity manager basic options
        proximityManager.configuration()
                //Using ranging for continuous scanning or MONITORING for scanning with intervals
                .scanPeriod(ScanPeriod.RANGING)
                //Using BALANCED for best performance/battery ratio
                .scanMode(ScanMode.LOW_LATENCY)
                //OnDeviceUpdate callback will be received with 5 seconds interval
                .deviceUpdateCallbackInterval(TimeUnit.MILLISECONDS.toMillis(100));

        //Setting up iBeacon and Eddystone listeners
        proximityManager.setIBeaconListener(createIBeaconListener());
    }

    private IBeaconListener createIBeaconListener() {
        return new IBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice beacon, IBeaconRegion region) {
                serialSend(beacon);
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                for (IBeaconDevice beacon : iBeacons){
                    serialSend(beacon);
                }
            }

            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {
            }
        };
    }

    private void serialSend(IBeaconDevice beacon){

        showOnScreen(beacon); //temporary

        Log.i(TAG, "Sending: " + beacon);
        byte[] bufferMajor = ByteBuffer.allocate(4).putInt(beacon.getMajor()).array();
        byte[] bufferMinor = ByteBuffer.allocate(4).putInt(beacon.getMinor()).array();
        byte[] bufferRSSI = ByteBuffer.allocate(4).putInt(beacon.getRssi()).array();
        byte[] buffer = new byte[bufferMajor.length + bufferMinor.length + bufferRSSI.length];

        System.arraycopy(bufferMajor, 0, buffer, 0, bufferMajor.length);
        System.arraycopy(bufferMinor, 0, buffer, bufferMajor.length, bufferMinor.length);
        System.arraycopy(bufferRSSI, 0, buffer, bufferMajor.length + bufferMinor.length, bufferRSSI.length);

        CRC32 crc = new CRC32();
        crc.update(buffer);
        long hash = crc.getValue();
        byte[] bufferCRC = ByteBuffer.allocate(8).putLong(hash).array();
        byte[] outputBuffer = new byte[buffer.length + bufferCRC.length];

        System.arraycopy(buffer, 0, outputBuffer, 0, buffer.length);
        System.arraycopy(bufferCRC, 0, outputBuffer, buffer.length , bufferCRC.length);

        for (int i = 0; i<outputBuffer.length; i++){
            outputBuffer[i] = (byte) Math.abs(outputBuffer[i]);
        }

        String str = new String(outputBuffer);
        //Log.i(TAG, "Sending: " + Arrays.toString(outputBuffer));
        //Log.i(TAG, "Sending: " + str);
        //Log.i(TAG, "Sending: " + Arrays.toString(str.getBytes()));
        SerialInterface.sendData(str);
    }

    private void showOnScreen(IBeaconDevice beacon){
        for(int i = 0, j = tableLayout.getChildCount(); i < j; i++) {
            View view = tableLayout.getChildAt(i);
            if (view instanceof TableRow) {
                TableRow row = (TableRow) view;
                for(int l = 0, k = row.getChildCount(); l < k; l++) {
                    View view2 = row.getChildAt(l);
                    if (view2 instanceof TextView) {
                        TextView tv = (TextView) view2;
                        String text = tv.getText().toString();
                        if(text.startsWith(beacon.getAddress())) {
                            tableLayout.removeViewAt(i);
                        }
                    }
                }
            }
        }
        if (beacon.getAddress() != null) {
            TableRow row = new TableRow(this);
            TextView tv = new TextView(this);
            tv.setText(beacon.getAddress()+ "\t\t" + beacon.getMajor() + "\t\t" + beacon.getMinor() + "\t\t" +  beacon.getRssi());
            row.addView(tv);
            tableLayout.addView(row);
        }
    }

    private void startScanning() {
        //Connect to scanning service and start scanning when ready
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                //Check if proximity manager is already scanning
                if (proximityManager.isScanning()) {
                    Toast.makeText(MainActivity.this, "Already scanning", Toast.LENGTH_SHORT).show();
                    return;
                }
                proximityManager.startScanning();
                Toast.makeText(MainActivity.this, "Scanning started", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void stopScanning() {
        //Stop scanning if scanning is in progress
        if (proximityManager.isScanning()) {
            proximityManager.stopScanning();
            Toast.makeText(this, "Scanning stopped", Toast.LENGTH_SHORT).show();
        }
    }

}
