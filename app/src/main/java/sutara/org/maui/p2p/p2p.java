package sutara.org.maui.p2p;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by User on 04-04-2018.
 */

public class p2p extends Service{

    WifiP2pManager manager;
    Channel channel;

    private final IntentFilter intentFilter = new IntentFilter();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("p2p service","onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("p2p service","onStartCommand");
        Context context = getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (isConnected){
            Log.i("isConnected","Internet is available");
        }else {
            Log.i("isConnected","Internet is not available");
        }

//        WifiManager wifiManager;
//        Context context = getApplicationContext();
//
//        wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
//        wifiManager.setWifiEnabled(true);

//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
////
//        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
//        channel = manager.initialize(this, getMainLooper(), null);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("p2p service","onDestroy");
        super.onDestroy();
    }
}
