package rtandroid.main.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import rtandroid.main.service.BackgroundScanService;

public class RestartBLEReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(RestartBLEReceiver.class.getSimpleName(), "BLE service Stops! Restarting.");
        BackgroundScanService service = new BackgroundScanService();
        context.startService(new Intent(context, service.getClass()));;
    }
}
