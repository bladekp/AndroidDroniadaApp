package rtandroid.main.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import rtandroid.main.serial.SerialInterface;


public class BackgroundScanService extends Service {

    public static final String TAG = "BackgroundScanService";
    private final Handler handler = new Handler();
    private ProximityManager proximityManager;
    public static TableLayout tableLayout;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        //this.onDestroy();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onCreate();
        setupProximityManager();
        SerialInterface.initialize();
        Log.i(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        stopScanning();
        handler.removeCallbacksAndMessages(null);
        if (proximityManager != null) {
            proximityManager.disconnect();
            proximityManager = null;
        }
        super.onDestroy();
        sendBroadcast(new Intent("rtandroid.main.RestartBLESensor"));
        Log.i(TAG, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startScanning();
        Log.i(TAG, "onStartCommand");
        return START_STICKY;
    }

    private void setupProximityManager() {
        proximityManager = ProximityManagerFactory.create(this);
        //Configure proximity manager basic options
        proximityManager.configuration()
                //Using ranging for continuous scanning or MONITORING for scanning with intervals
                .scanPeriod(ScanPeriod.RANGING)
                //Using BALANCED for best performance/battery ratio
                .scanMode(ScanMode.LOW_LATENCY)
                //OnDeviceUpdate callback will be received with interval
                .deviceUpdateCallbackInterval(TimeUnit.MILLISECONDS.toMillis(500));
        //Setting up iBeacon and Eddystone listeners
        proximityManager.setIBeaconListener(createIBeaconListener());
    }


    private void startScanning() {
        //Connect to scanning service and start scanning when ready
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
                Log.i(TAG, "Scanning started");
            }
        });
    }

    private void stopScanning() {
        //Stop scanning if scanning is in progress
        if (proximityManager != null && proximityManager.isScanning()) {
            proximityManager.stopScanning();
            Log.i(TAG, "Scanning stopped");
        }
    }

    private IBeaconListener createIBeaconListener() {
        return new IBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice iBeacon, IBeaconRegion region) {
                if(iBeacon.getProximityUUID().toString().equals("3a49d7d0-d7cf-4946-8f3f-bd6e74219b5d")) serialSend(iBeacon);
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                for (IBeaconDevice beacon : iBeacons) {
                    if(beacon.getProximityUUID().toString().equals("3a49d7d0-d7cf-4946-8f3f-bd6e74219b5d")) serialSend(beacon);
                }
            }

            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {
                removeBeaconFromTable(iBeacon);
            }
        };
    }

    private void serialSend(IBeaconDevice beacon) {
        showOnScreen(beacon); //temporary
        //Log.i(TAG, "Sending: " + beacon);

        byte[] bufferMajor = ByteBuffer.allocate(1).put((byte) beacon.getMajor()).array();
        byte[] bufferMinor = ByteBuffer.allocate(1).put((byte)beacon.getMinor()).array();
        byte[] bufferRSSI = ByteBuffer.allocate(1).put((byte)Math.abs(beacon.getRssi())).array();
        byte[] buffer = new byte[bufferMajor.length + bufferMinor.length + bufferRSSI.length];

        System.arraycopy(bufferMajor, 0, buffer, 0, bufferMajor.length);
        System.arraycopy(bufferMinor, 0, buffer, bufferMajor.length, bufferMinor.length);
        System.arraycopy(bufferRSSI, 0, buffer, bufferMajor.length + bufferMinor.length, bufferRSSI.length);

        //TODO crc
        /*CRC32 crc = new CRC32();
        crc.update(buffer);
        long hash = crc.getValue();
        byte[] bufferCRC = ByteBuffer.allocate(8).putLong(hash).array();*/
        byte[] outputBuffer = new byte[2 + buffer.length ];//+ bufferCRC.length];

        //two bytes starting sequence
        outputBuffer[0] = (byte)0x05;
        outputBuffer[1] = (byte)0x05;
        System.arraycopy(buffer, 0, outputBuffer, 2, buffer.length);
        //System.arraycopy(bufferCRC, 0, outputBuffer, buffer.length + 2, bufferCRC.length);

        String str = new String(outputBuffer);
        SerialInterface.sendData(str);
    }


    //shows beacon info on screen - temporary
    private void showOnScreen(IBeaconDevice beacon) {

        removeBeaconFromTable(beacon);

        if (beacon.getAddress() != null) {
            TableRow row = new TableRow(this);
            TextView tv = new TextView(this);
            tv.setText(beacon.getAddress() + "\t\t" + beacon.getMajor() + "\t\t" + beacon.getMinor() + "\t\t" + beacon.getRssi());
            row.addView(tv);
            tableLayout.addView(row);
        }
    }

    //removes beacon from list - temporary
    private void removeBeaconFromTable(IBeaconDevice beacon) {
        for (int i = 0, j = tableLayout.getChildCount(); i < j; i++) {
            View view = tableLayout.getChildAt(i);
            if (view instanceof TableRow) {
                TableRow row = (TableRow) view;
                for (int l = 0, k = row.getChildCount(); l < k; l++) {
                    View view2 = row.getChildAt(l);
                    if (view2 instanceof TextView) {
                        TextView tv = (TextView) view2;
                        String text = tv.getText().toString();
                        if (text.startsWith(beacon.getAddress())) {
                            tableLayout.removeViewAt(i);
                        }
                    }
                }
            }
        }
    }

}
