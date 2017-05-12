package rtandroid.main;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.Toast;

import rtandroid.main.service.BackgroundScanService;

/**
 * Main app activity, user interface not needed. Just background tasks.
 * <p>
 * Connect to UART from linux's screen: sudo screen /dev/ttyUSB0 9600
 * UART configuration:
 * GND: pin 6
 * TX: pin 8
 * RX: pin 10
 * <p>
 * Looking for beacon UUID:  3a49d7d0-d7cf-4946-8f3f-bd6e74219b5d
 * <p>
 * TODO: after some time error in app appear:
 * 04-22 17:25:00.484 13014-13026/rtandroid.main D/BluetoothAdapter: onBluetoothServiceDown: android.bluetooth.IBluetooth$Stub$Proxy@e3e1a43
 * 04-22 17:25:01.602 13014-13027/rtandroid.main D/BluetoothAdapter: onBluetoothServiceDown: null
 * 04-22 17:25:02.204 13014-13026/rtandroid.main D/BluetoothAdapter: onBluetoothServiceUp: android.bluetooth.IBluetooth$Stub$Proxy@f43c8c0
 * 04-22 17:25:10.347 13014-13027/rtandroid.main D/BluetoothAdapter: onBluetoothServiceDown: android.bluetooth.IBluetooth$Stub$Proxy@f43c8c0
 * 04-22 17:25:10.844 13014-13026/rtandroid.main D/BluetoothAdapter: onBluetoothServiceUp: android.bluetooth.IBluetooth$Stub$Proxy@f1e179f
 * <p>
 * TODO: application stops broadcasting after some time, may it be something like:
 * http://fabcirablog.weebly.com/blog/creating-a-never-ending-background-service-in-android
 * \
 *
 * TODO: wyłapać onBluetoothServiceUp i zresetować BacgdroundScanService
 */

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int REQUEST_CODE_PERMISSIONS = 100;
    private BackgroundScanService bleService;
    private Intent bleIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        BackgroundScanService.tableLayout = (TableLayout)findViewById(R.id.tableLayout);
        bleService = new BackgroundScanService();
        bleIntent = new Intent(this, bleService.getClass());
        startService(bleIntent);
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

}
