package com.example.newdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiManager;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.IdentityChangedListener;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareSession;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet6Address;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private final String THE_MAC = "THEMAC";
    private final int MAC_ADDRESS_MESSAGE = 55;
    private final static int FINE_lOCATION_PERMISSION_CODE = 1;
    private Context context;
    private WifiManager wifiManager;
    private WifiAwareManager wifiAwareManager;
    private WifiAwareSession wifiAwareSession;
    private ConnectivityManager connectivityManager;
    private NetworkSpecifier networkSpecifier;
    private PublishDiscoverySession publishDiscoverySession;
    private SubscribeDiscoverySession subscribeDiscoverySession;
    private PeerHandle peerHandle;
    private WifiRttManager wifiRttManager;
    private byte[] myMac;
    private byte[] otherMac;
    private IntentFilter intentFilter;
    private BroadcastReceiver receiver;
    //private final int IP_ADDRESS_MESSAGE = 33;
    //private final int MESSAGE = 7;
    //private Inet6Address ipv6;
    //private ServerSocket serverSocket;
    //private final byte[] serviceInfo = "android".getBytes();
   // private byte[] portOnSystem;
    private int portToUse;
    private byte[] myIP;
    private byte[] otherIP;
    private byte[] msgtosend;
    private List<byte[]> discoveredPeers;
    private String myStatus = "infected";
    private TextView textView1, textView2, txtSession, txtPublish, txtSubscribe, rttTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        wifiAwareManager = null;
        wifiAwareSession = null;
        connectivityManager = null;
        networkSpecifier = null;
        publishDiscoverySession = null;
        subscribeDiscoverySession = null;
        peerHandle = null;

        textView1 = findViewById(R.id.txt);
        textView2 = findViewById(R.id.txt1);
        txtSession = findViewById(R.id.txt2);
        txtPublish = findViewById(R.id.txtPublish);
        txtSubscribe = findViewById(R.id.txtSubscribe);
        context = MainActivity.this;

        wifiManager = (WifiManager)
                context.getSystemService(Context.WIFI_SERVICE);

        checkPermissions();

        if (wifiAwareIsSupported()) {
            wifiAwareManager = (WifiAwareManager) context.getSystemService(WIFI_AWARE_SERVICE);
            textView1.setText("wifiAware initiate successfully");
            if (rTTIsSupported()) {
                wifiRttManager = (WifiRttManager) context.getSystemService(context.WIFI_RTT_RANGING_SERVICE);
                rttTextView.setText("RTT Enabled");
            } else {
                rttTextView.setText("RTT is not Supported!");
            }
            assignIntentFilterToItsActions();
            attachToAwareSession();
            Toast.makeText(context, "Attach finish", Toast.LENGTH_SHORT).show();
            Toast.makeText(context, wifiAwareManager.toString(), Toast.LENGTH_SHORT).show();

        } else {
            textView1.setText("wifiAware is....................................... not supported!");
        }

        if (rTTIsSupported()) {

            RangingRequest.Builder builder = new RangingRequest.Builder();
            builder.addWifiAwarePeer(peerHandle);
            RangingRequest request = builder.build();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermission();
                return;
            }
            wifiRttManager.startRanging(request, new DirectExecutor(), new RangingResultCallback() {
                @Override
                public void onRangingFailure(int i) {

                }

                @Override
                public void onRangingResults(@NonNull List<RangingResult> list) {

                }
            });
        } else {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        context.unregisterReceiver(receiver);
        closeSession();
    }

    @Override
    @TargetApi(29)
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            assignIntentFilterToItsActions();
        }


        if (wifiAwareManager.isAvailable()) {
            attachToAwareSession();
        }
    }

    private boolean wifiAwareIsSupported() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE);
    }

    private void assignIntentFilterToItsActions() {

        intentFilter = new IntentFilter();
        //receiver = new WifiAwareBroadCastReceiver(wifiAwareManager, MainActivity.this);

        intentFilter.addAction(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
        intentFilter.addAction(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (wifiAwareManager.isAvailable()) {
                    attachToAwareSession();
                } else {
                    Toast.makeText(context, "Check wifi connection or allow access location", Toast.LENGTH_SHORT).show();
                    attachToAwareSession();
                }

                if (wifiRttManager.isAvailable()) {


                } else {
                    Toast.makeText(context, "RTT Not available", Toast.LENGTH_SHORT).show();
                }
            }
        };
        context.registerReceiver(receiver, intentFilter);
        textView2.setText("intent filter assigned successfully");
    }
  
    public boolean wifiAwareIsAvailable() {
        if (wifiAwareManager.isAvailable()) {

            return true;
        } else {

            checkPermissions();
            return false;
        }

    }

    public void checkPermissions() {

        if (!wifiManager.isWifiEnabled()) {
            showWifiEnableAlertDialog();
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this
                , Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();

        }
    }

    /*private void requestWifiPermission() {
        if (!wifiManager.isWifiEnabled()) {
            showWifiEnableAlertDialog();
        }
    }*/

    private void requestLocationPermission() {

        //check first for availability of this feature in the device
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION)) {


            //request for permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                showLocationDialog();
            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_lOCATION_PERMISSION_CODE);


            }

        } else {
            //if feature not available
            Toast.makeText(getApplicationContext(), "your phone does'nt support this action", Toast.LENGTH_LONG).show();
        }
    }

    private void showWifiEnableAlertDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Open WLAN")
                .setMessage("WLAN is used to connect to your friend ")
                .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        wifiManager.setWifiEnabled(true);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).create().show();
    }

    private void showLocationDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Access Location Needed")
                .setMessage("Application needed access location to discover other devices")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        ActivityCompat.requestPermissions(MainActivity.this
                                , new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}
                                , FINE_lOCATION_PERMISSION_CODE);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                }).create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == FINE_lOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Accessing location allowed", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Accessing location denied", Toast.LENGTH_LONG).show();

            }
        }
    }

    @TargetApi(26)
    public void attachToAwareSession() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            txtSession.setText("sdk version error");
            return;
        }

        // Only once
        if (wifiAwareSession != null) {
            txtSession.setText(" wifiaware session != null");
            return;
        }

        if (wifiAwareManager == null || !wifiAwareManager.isAvailable()) {
            txtSession.setText("wifiaware is not available");

            return;
        }

        wifiAwareManager.attach(new AttachCallback() {
            @Override
            public void onAttached(WifiAwareSession session) {
                super.onAttached(session);
                txtSession.setText("session before: " + wifiAwareSession + "\n ");
                wifiAwareSession = session;
                wifiAwareSession.close();
                txtSession.append("session after assign: " + wifiAwareSession + "\n");
                txtSession.append("session after assign: " + wifiAwareSession + "\n");
                publishService();
                subscribeToService();
            }

            @Override
            public void onAttachFailed() {
                super.onAttachFailed();
                txtSession.setText("session failed: " + wifiAwareSession);
            }
        }, new IdentityChangedListener() {
            @Override
            public void onIdentityChanged(byte[] mac) {
                super.onIdentityChanged(mac);
                setMacAddress(mac);
            }
        }, null);
    }

    @TargetApi(26)
    private void publishService() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        Log.d("nanPUBLISH", "building publish session");
        PublishConfig config = new PublishConfig.Builder()
                .setServiceName(THE_MAC)
                .setRangingEnabled(true)
                .build();
        txtPublish.setText("before publish: " + wifiAwareSession + "\n");

        if (wifiAwareSession != null) {

            wifiAwareSession.publish(config, new DiscoverySessionCallback() {
                @Override
                public void onPublishStarted(@NonNull PublishDiscoverySession session) {
                    super.onPublishStarted(session);

                    publishDiscoverySession = session;
                    if (publishDiscoverySession != null && peerHandle != null) {
                        publishDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, myMac);

                        Log.d("nanPUBLISH", "onPublishStarted sending mac");

                    }
                }

                @Override
                public void onMessageReceived(PeerHandle peerHandle_, byte[] message) {
                    super.onMessageReceived(peerHandle, message);
                    Log.d("nanPUBLISH", "received message");
                    if (message.length == 2) {
                        portToUse = byteToPortInt(message);
                        Log.d("received", "will use port number " + portToUse);
                    } else if (message.length == 6) {
                        setOtherMacAddress(message);
                        Toast.makeText(MainActivity.this, "mac received", Toast.LENGTH_LONG).show();
                    } else if (message.length == 16) {
                        setOtherIPAddress(message);
                        Toast.makeText(MainActivity.this, "ip received", Toast.LENGTH_LONG).show();
                    } else if (message.length > 16) {
                        setMessage(message);
                        Toast.makeText(MainActivity.this, "message received", Toast.LENGTH_LONG).show();
                    }

                    peerHandle = peerHandle_;

                    if (publishDiscoverySession != null && peerHandle != null) {
                        publishDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, myMac);
                        Log.d("nanPUBLISH", "onMessageReceived sending mac");

                    }
                }

                @Override
                public void onServiceDiscovered(PeerHandle peerHandle, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                    super.onServiceDiscovered(peerHandle, serviceSpecificInfo, matchFilter);
                }

                @Override
                public void onServiceDiscoveredWithinRange(PeerHandle peerHandle, byte[] serviceSpecificInfo, List<byte[]> matchFilter, int distanceMm) {
                    super.onServiceDiscoveredWithinRange(peerHandle, serviceSpecificInfo, matchFilter, distanceMm);
                }

            }, null);
        } else {
            Toast.makeText(context, "on publish..wifiAware session is null", Toast.LENGTH_SHORT).show();
        }
        //-------------------------------------------------------------------------------------------- -----
    }

    //-------------------------------------------------------------------------------------------- +++++

    @TargetApi(26)
    private void subscribeToService() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        Log.d("nanSUBSCRIBE", "building subscribe session");
        SubscribeConfig config = new SubscribeConfig.Builder()
                .setServiceName(THE_MAC)
                .build();
        Log.d("nanSUBSCRIBE", "build finish");
        txtSession.append("before subscribe: " + wifiAwareSession);
        if (wifiAwareSession != null) {

            wifiAwareSession.subscribe(config, new DiscoverySessionCallback() {
                @Override
                public void onServiceDiscovered(PeerHandle peerHandle_, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                    super.onServiceDiscovered(peerHandle_, serviceSpecificInfo, matchFilter);

                    peerHandle = peerHandle_;
                    discoveredPeers = matchFilter;

                    if (subscribeDiscoverySession != null && peerHandle != null) {
                        subscribeDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, myMac);
                        Log.d("nanSUBSCRIBE", "onServiceDiscovered send mac");


                    }
                }

                @Override
                public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
                    super.onSubscribeStarted(session);

                    subscribeDiscoverySession = session;

                    if (subscribeDiscoverySession != null && peerHandle != null) {
                        subscribeDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, myMac);
                        Log.d("nanSUBSCRIBE", "onServiceStarted send mac");

                    }
                }

                @Override
                public void onMessageSendSucceeded(int messageId) {
                    super.onMessageSendSucceeded(messageId);

                }


                @Override
                public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                    super.onMessageReceived(peerHandle, message);
                    Log.d("nanSUBSCRIBE", "received message");
                    Toast.makeText(MainActivity.this, "received", Toast.LENGTH_LONG).show();
                    if (message.length == 2) {
                        portToUse = byteToPortInt(message);
                        Log.d("received", "will use port number " + portToUse);
                    } else if (message.length == 6) {
                        setOtherMacAddress(message);
                        Toast.makeText(MainActivity.this, "mac received", Toast.LENGTH_LONG).show();
                    } else if (message.length == 16) {
                        setOtherIPAddress(message);
                        Toast.makeText(MainActivity.this, "ip received", Toast.LENGTH_LONG).show();
                    } else if (message.length > 16) {
                        setMessage(message);
                        Toast.makeText(MainActivity.this, "message received", Toast.LENGTH_LONG).show();
                    }
                }
            }, null);
        } else {
            Toast.makeText(context, "subscribe...wifiAware session is null", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeSession() {

        if (publishDiscoverySession != null) {
            publishDiscoverySession.close();
            publishDiscoverySession = null;
        }

        if (subscribeDiscoverySession != null) {
            subscribeDiscoverySession.close();
            subscribeDiscoverySession = null;
        }

        if (wifiAwareSession != null) {
            wifiAwareSession.close();
            wifiAwareSession = null;
        }
    }

    private void setMacAddress(byte[] mac) {
        myMac = mac;
        String macAddress = String.format("%02x:%02x:%02x:%02x:%02x:%02x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

    }

    private void setOtherMacAddress(byte[] mac) {
        otherMac = mac;
        String macAddress = String.format("%02x:%02x:%02x:%02x:%02x:%02x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

    }

    //-------------------------------------------------------------------------------------------- +++++

    private void setOtherIPAddress(byte[] ip) {
        otherIP = ip;
        try {
            String ipAddr = Inet6Address.getByAddress(otherIP).toString();
        } catch (UnknownHostException e) {
            Log.d("myTag", "socket exception " + e.toString());
        }
    }

    private void setMessage(byte[] msg) {
        String outmsg = new String(msg).replace("messageToBeSent: ", "");

    }

    public int byteToPortInt(byte[] bytes) {
        return ((bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF));
    }

    public byte[] portToBytes(int port) {
        byte[] data = new byte[2];
        data[0] = (byte) (port & 0xFF);
        data[1] = (byte) ((port >> 8) & 0xFF);
        return data;
    }

//=============================================================================================================================

    private boolean rTTIsSupported() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT);
    }

    class DirectExecutor implements Executor {
        public void execute(Runnable r) {
            r.run();
        }
    }
}