package com.example.newdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.aware.WifiAwareManager;

public class WifiAwareBroadCastReceiver extends BroadcastReceiver {

    private WifiAwareManager mWifiAwareManager;
    private MainActivity activity;

    public WifiAwareBroadCastReceiver(WifiAwareManager wifiAwareManager , MainActivity activity) {
        this.mWifiAwareManager = wifiAwareManager;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (mWifiAwareManager.isAvailable()){
            activity.attachToAwareSession();

        }else{


        }

    }
}
