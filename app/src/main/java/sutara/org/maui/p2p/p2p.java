package sutara.org.maui.p2p;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.content.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class p2p extends Service {

    final static String TAG="P2P";

    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_WiFiP2p";
    public static final String SERVICE_REG_TYPE = "_chimple._tcp";

    WifiP2pDnsSdServiceInfo service;

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;
    private WifiP2pDnsSdServiceRequest serviceRequest;

    WiFiP2pService wifiservice = new WiFiP2pService();
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
        if(manager!=null||channel!=null){
            manager=null;
            channel=null;
        }
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        receiver = new WiFiBroadcastReceiver(manager, channel, null);
        registerReceiver(receiver, intentFilter);

        RegisterAndDiscover();

        return START_STICKY;
    }


private void RegisterAndDiscover(){
    Map<String, String> record = new HashMap<String, String>();
    record.put(TXTRECORD_PROP_AVAILABLE, "visible");

    service = WifiP2pDnsSdServiceInfo.newInstance(
            SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
    manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.i(TAG,"successfully added local service");
        }

        @Override
        public void onFailure(int i) {
            Log.i(TAG,"Failed to added local service; Error Code:"+i);
        }
    });
    CreateAndDiscoverGroup();
}

private void CreateAndDiscoverGroup(){

        manager.setDnsSdResponseListeners(channel,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(
                            String instanceName,
                            String registrationType,
                            WifiP2pDevice srcDevice) {
                            Log.i(TAG,"onDnsSdServiceAvailable");
                            if (wifiservice==null||wifiservice.device!=srcDevice){
                                wifiservice.device = srcDevice;
                                wifiservice.instanceName = instanceName;
                                wifiservice.serviceRegistrationType = registrationType;
                                WifiP2pConfig config=new WifiP2pConfig();
                                config.deviceAddress=srcDevice.deviceAddress;
                                config.wps.setup=WpsInfo.PBC;
                                manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.i("Connection","successfully connected");
                                    }

                                    @Override
                                    public void onFailure(int i) {
                                        Log.i("connection","Failed to connect");
                                    }
                                });
                            }
                    }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String DomainName,
                            Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(TAG,device.deviceName + " is "+ record.get(TXTRECORD_PROP_AVAILABLE));
                    }
                });

    serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
    manager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.i(TAG,"Added service discovery request");
        }

        @Override
        public void onFailure(int i) {
            Log.i(TAG,"Failed to add Service request. Reason :" + i);
        }
    });

    manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.i(TAG,"Service discovery initialized");
        }

        @Override
        public void onFailure(int i) {
            Log.i(TAG,"Failure to initialized discovery");
        }
    });
    manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            Log.i(TAG," requestConnectionInfo " +wifiP2pInfo.toString());
            disconnect();
        }
    });


}


private void disconnect(){
        try{
            TimeUnit.SECONDS.sleep(5);
            Log.i("Disconnection","Try method");
            manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG,"cancel Connect Service");
                }

                @Override
                public void onFailure(int i) {
                    Log.i(TAG,"cancel Connect Failure");
                }
            });
//            unregisterReceiver(receiver);
        }catch (Exception e){
            Log.i("Disconnection","Failed to disconnect: "+e);
        }
}



    @Override
    public void onDestroy() {
        Log.i("p2p service","onDestroy");
        if(receiver!=null) {
            unregisterReceiver(receiver);
        }
        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i("clearLocalServices","onDestroy");
            }

            @Override
            public void onFailure(int i) {
                Log.i("clearLocalServices","onFailure onDestroy");
            }
        });
        manager.removeLocalService(channel, service, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i("removeLocalService","onDestroy");
            }

            @Override
            public void onFailure(int i) {
                Log.i("removeLocalService","onFailure onDestroy");
            }
        });
        manager.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i("clearServiceRequests","onDestroy");
            }

            @Override
            public void onFailure(int i) {
                Log.i("clearServiceRequests","onFailure onDestroy");
            }
        });
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i("removeGroup","onDestroy");
            }

            @Override
            public void onFailure(int i) {
                Log.i("removeGroup","onFailure onDestroy");
            }
        });
        super.onDestroy();
    }
}
