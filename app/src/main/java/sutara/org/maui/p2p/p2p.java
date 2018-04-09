package sutara.org.maui.p2p;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;


public class p2p extends Service{

    public static final String TAG = "WiFiP2PService";
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_wifidemotest";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    private WifiP2pDnsSdServiceRequest serviceRequest;

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
        //checking if the internet access available or not
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (isConnected){
            Log.i("Internet Connection","Internet is available");
            //checking if the internet access if available from mobile data or wifi
            boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
            if (!isWiFi){
                Log.i("Mobile Data Internet","Mobile Network available");

            }else{
                Log.i("WIFI Internet","wifi Internet is available");
            }
        }else {
            Log.i("Internet Connection","Internet is not available");
            WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.isWifiEnabled()){
                Log.i("Internet Connection","Turning on WiFi");
                wifiManager.setWifiEnabled(true);
            }

            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

            manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            channel = manager.initialize(this, getMainLooper(), null);
            startRegistrationAndDiscovery();


        }
        return START_STICKY;
    }

    void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.i("startRegAndDiscovery","Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                Log.i("startRegAndDiscovery","Failed to add a service");
            }
        });

        discoverService();

    }

    private void discoverService() {

        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */

        manager.setDnsSdResponseListeners(channel,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(final String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?

                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {

                                WiFiP2pService service = new WiFiP2pService();
                                service.device = srcDevice;
                                service.instanceName = instanceName;
                                service.serviceRegistrationType = registrationType;
                                Log.d(TAG, "instanceName" + instanceName+"srcDevice"+srcDevice+"registrationType"+registrationType);
//                            try {
//                                wait(1000);
                                WifiP2pConfig config = new WifiP2pConfig();
                                config.deviceAddress = service.device.deviceAddress;
                                config.wps.setup = WpsInfo.PBC;
                                if (serviceRequest != null)
                                    manager.removeServiceRequest(channel, serviceRequest,
                                            new WifiP2pManager.ActionListener() {

                                                @Override
                                                public void onSuccess() {
                                                    Log.i("discoverService","Connect P2p is successfull");
                                                }

                                                @Override
                                                public void onFailure(int arg0) {
                                                    Log.i("discoverService","Connect p2p failed");
                                                }
                                            });

                                manager.connect(channel, config, new WifiP2pManager.ActionListener() {

                                    @Override
                                    public void onSuccess() {
                                        Log.i("discoverService","Connecting to service to -" +instanceName);

                                    }

                                    @Override
                                    public void onFailure(int errorCode) {
                                        Log.i("discoverService","Failed connecting to service");
                                    }
                                });

//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
                        }

                    }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(TAG,
                                device.deviceName + " is "
                                        + record.get(TXTRECORD_PROP_AVAILABLE));
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel, serviceRequest,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.i(TAG,"Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        Log.i(TAG,"Failed adding service discovery request");
                    }
                });
        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.i(TAG,"Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                Log.i(TAG,"Service discovery failed");

            }
        });
    }



    @Override
    public void onDestroy() {
        Log.i("p2p service","onDestroy");
        super.onDestroy();
    }
}
