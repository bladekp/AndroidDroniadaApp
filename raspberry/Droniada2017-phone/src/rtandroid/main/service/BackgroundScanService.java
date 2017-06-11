package rtandroid.main.service;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class BackgroundScanService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private long UPDATE_INTERVAL = 5000;
    private long FASTEST_INTERVAL = 1000;
    public static final String TAG = "BackgroundScanService";
    private final Handler handler = new Handler();
    private ProximityManager proximityManager;
    public static TableLayout tableLayout;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LatLng latLng;

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
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
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
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        // only stop if it's connected, otherwise we crash
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        Log.i(TAG, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startScanning();
        mGoogleApiClient.connect();
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
                if (iBeacon.getProximityUUID().toString().equals("3a49d7d0-d7cf-4946-8f3f-bd6e74219b5d"))
                    serialSend(iBeacon);
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                for (IBeaconDevice beacon : iBeacons) {
                    if (beacon.getProximityUUID().toString().equals("3a49d7d0-d7cf-4946-8f3f-bd6e74219b5d"))
                        serialSend(beacon);
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
        Log.i(TAG, "Sending: " + beacon);
        if (latLng != null) {
            Log.i(TAG, "current location: " + latLng.toString());
        }
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

    @Override
    public void onConnected(Bundle dataBundle) {
        // Get last known recent location.
        Location mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        // Note that this can be NULL if last location isn't already known.
        if (mCurrentLocation != null) {
            // Print current location if not null
            Log.d("DEBUG", "current location: " + mCurrentLocation.toString());
            latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        }
        // Begin polling for new location updates.
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
    }
}
