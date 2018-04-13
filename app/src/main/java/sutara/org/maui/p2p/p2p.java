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
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class p2p extends Service {


    final static String TAG="P2P";

    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_WiFiP2p";
    public static final String SERVICE_REG_TYPE = "_chimple._tcp";

    static WifiP2pDnsSdServiceInfo service;

    static WifiP2pManager manager;
    static WifiP2pManager.Channel channel;

    static WifiP2pServiceRequest req;
    private BroadcastReceiver receiver = null;
    static private WifiP2pDnsSdServiceRequest serviceRequest;

    static WiFiP2pService wifiservice = new WiFiP2pService();
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

        return START_NOT_STICKY;
    }


public static void RegisterAndDiscover(){
    Map<String, String> record = new HashMap<String, String>();
    record.put(TXTRECORD_PROP_AVAILABLE, "visible");

    service = WifiP2pDnsSdServiceInfo.newInstance(
            SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
    manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.i(TAG,"successfully added local service");
//            Toast.makeText(p2p.this, "successfully added local service",
//                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(int i) {
            Log.i(TAG,"Failed to added local service; Error Code:"+i);
//            Toast.makeText(p2p.this, "Failed to added local service; Error Code:"+i,
//                    Toast.LENGTH_SHORT).show();
        }
    });

    DiscoverGroupService();
//    CreateGroup();
}

//private void CreateGroup(){
//        if(manager!=null||channel!=null){
//            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
//                @Override
//                public void onSuccess() {
//                    Log.i("removeGroup","successfully removeGroup");
//                }
//
//                @Override
//                public void onFailure(int i) {
//                    Log.i("removeGroup","Failed removeGroup; Error Code:"+i);
//                }
//            });
//        }
//        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
//            @Override
//            public void onSuccess() {
//                Log.i("Creategroup","created group successfully");
//            }
//
//            @Override
//            public void onFailure(int i) {
//                Log.i("Creategroup","onFailure to create group ");
//            }
//        });
//
//        manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
//            @Override
//            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
//                Log.i("onConnInfoAvailable",wifiP2pInfo.toString());
//            }
//        });
//}

public static void DiscoverGroupService(){

        manager.setDnsSdResponseListeners(channel,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(
                            String instanceName,
                            String registrationType,
                            WifiP2pDevice srcDevice) {
                            Log.i(TAG,"onDnsSdServiceAvailable");
//                        Toast.makeText(p2p.this, "onDnsSdServiceAvailable", Toast.LENGTH_SHORT).show();

                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {

                                wifiservice.device = srcDevice;
                                wifiservice.instanceName = instanceName;
                                wifiservice.serviceRegistrationType = registrationType;
                                WifiP2pConfig config=new WifiP2pConfig();
                                config.deviceAddress=srcDevice.deviceAddress;
                                config.wps.setup=WpsInfo.PBC;
                                // this gives the priority to be a group owner.
//                                config.groupOwnerIntent=15;
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
//                        Toast.makeText(p2p.this, device.deviceName + " is "+ record.get(TXTRECORD_PROP_AVAILABLE), Toast.LENGTH_SHORT).show();
                    }
                });

    serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
    manager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.i(TAG,"Added service discovery request");
//            Toast.makeText(p2p.this, "Added service discovery request", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(int i) {
            Log.i(TAG,"Failed to add Service request. Reason :" + i);
//            Toast.makeText(p2p.this, "Failed to add Service request. Reason :" + i, Toast.LENGTH_SHORT).show();
        }
    });

    manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.i(TAG,"Service discovery initialized");
//            Toast.makeText(p2p.this, "Service discovery initialized", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(int i) {
            Log.i(TAG,"Failure to initialized discovery");
//            Toast.makeText(p2p.this, "Failure to initialized discovery", Toast.LENGTH_SHORT).show();
        }
    });
//    manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
//        @Override
//        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
//            Log.i(TAG," requestConnectionInfo " +wifiP2pInfo.toString());
//            disconnect();
//        }
//    });


}


public static void disconnect(){
    manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.i("cancelConnect","onDestroy");
        }

        @Override
        public void onFailure(int i) {
            Log.i("cancelConnect","onFailure onDestroy");
        }
    });
    //The service information can be cleared and removed.
//    manager.removeLocalService(channel, service, new WifiP2pManager.ActionListener() {
//        @Override
//        public void onSuccess() {
//            Log.i("removeLocalService","onDestroy");
//        }
//
//        @Override
//        public void onFailure(int i) {
//            Log.i("removeLocalService","onFailure onDestroy");
//        }
//    });
    //request has to be cleared or else multiple request will be appearing when we restart the service
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
    //removing the group is good because there will be no issue after service is ended.
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
}



    @Override
    public void onDestroy() {
        Log.i("p2p service","onDestroy");

        if(receiver!=null) {
            unregisterReceiver(receiver);
        }


        //cancelling the connection from other device is important
        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i("cancelConnect","onDestroy");
            }

            @Override
            public void onFailure(int i) {
                Log.i("cancelConnect","onFailure onDestroy");
            }
        });






        //The service information can be cleared and removed.
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
        //request has to be cleared or else multiple request will be appearing when we restart the service
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
        //removing the group is good because there will be no issue after service is ended.
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

    public static void isWifiP2pEnabled(boolean b) {
        WifiP2pDevice device=new WifiP2pDevice();
        Log.i("isWifiP2pEnabled","WiFi P2P:"+b+device);
//        Toast.makeText(null, "isWifiP2pEnabled: WiFi P2P:"+b, Toast.LENGTH_SHORT).show();
    }
}
