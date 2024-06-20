package com.lyrnic.userside.services;

import android.accessibilityservice.AccessibilityService;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.lyrnic.userside.managers.ActionsManager;
import com.lyrnic.userside.managers.FilesManager;
import com.lyrnic.userside.network.ApiClient;
import com.lyrnic.userside.utilities.PermissionsUtilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ActionsController extends AccessibilityService{
    private final Set<Network> activeNetworks = new HashSet<>();
    ActionsManager actionsManager;
    public static boolean networkConnected;
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d("ActionsController", "onServiceConnected");
        actionsManager = new ActionsManager(this);
        registerNetworkCallbacks();
        keepWebsocketAlive();
    }
    private void registerNetworkCallbacks() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        connectivityManager.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                synchronized (activeNetworks) {
                    if (activeNetworks.isEmpty()) {
                        networkConnected = true;
                        actionsManager.connectToWebsocket();
                    }
                    activeNetworks.add(network);
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                synchronized (activeNetworks) {
                    activeNetworks.remove(network);
                    if (activeNetworks.isEmpty()) {
                        networkConnected = false;
                        actionsManager.webSocket.close(1000, "lost connection");
                    }
                }
            }
        });
    }

    public void keepWebsocketAlive() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                log("checking websocket connection..");
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", "ping");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                if(!actionsManager.webSocket.send(jsonObject.toString())){
                    log("websocket connection closed, reconnecting..");
                    actionsManager.connectToWebsocket();
                }
                else{
                    log("websocket connection alive");
                }
                handler.postDelayed(this, 1000 * 60 * 5);
            }
        }, 1000 * 60 * 5);
    }
    public void log(String text){
        Log.d("ActionsController", text);
        try {
            FilesManager.logStatus(this, text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        log("onDestroy");
    }
}
