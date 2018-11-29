package com.salesforce.samples.smartsyncexplorer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

    boolean status;
    ConnectivityReceiverListener connectivityReceiverListener;
    public NetworkChangeReceiver(ConnectivityReceiverListener listener) {
        connectivityReceiverListener = listener;
    }

    public NetworkChangeReceiver(){

    }


    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {

       // System.out.println("NetworkChangeReceiver   onReceive" + connectivityReceiverListener);
        //if (connectivityReceiverListener != null) {
        System.out.println("NetworkChangeReceiver   onReceive" + connectivityReceiverListener);
            try {
                connectivityReceiverListener.onNetworkConnectionChanged(isOnline(context));
            } catch (Exception e) {
                e.printStackTrace();
            }
        //}
    }

    public boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            //should check null because in airplane mode it will be null
            //return (netInfo != null && netInfo.isConnected());
            status = netInfo != null && netInfo.isConnected();
            return status;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }


    public interface ConnectivityReceiverListener {
        void onNetworkConnectionChanged(boolean isConnected);
    }
}
