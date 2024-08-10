package com.lyrnic.userside.network;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lyrnic.userside.listeners.OnChangeListener;
import com.lyrnic.userside.managers.FilesManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ActionsWebSocket extends WebSocketListener {
    public WebSocket webSocket;
    public OkHttpClient client;
    ConnectivityManager connectivityManager;
    boolean isConnected;
    ExecutorService executorService;
    boolean schedulerRunning;
    OnMessageListener onMessageListener;
    OnChangeListener onChangeListener;
    String token;
    private static ActionsWebSocket instance;

    private ActionsWebSocket(String token, ConnectivityManager connectivityManager) {
        this.token = token;
        this.connectivityManager = connectivityManager;
        client = new OkHttpClient();
        executorService = Executors.newSingleThreadExecutor();
    }

    public static ActionsWebSocket getInstance(String token, ConnectivityManager connectivityManager) {
        if (instance == null) {
            instance = new ActionsWebSocket(token, connectivityManager);
            Log.d("ActionsController", "instance created: " + instance);
        }

        return instance;
    }

    public synchronized void connectWebSocket() {
        webSocket = client.newWebSocket(ApiClient.getWebSocketRequest(token), this);
    }

    public synchronized void terminate() {
        if (webSocket != null) {
            if (schedulerRunning) {
                stopScheduler();
            }
            disconnectWebSocket();
        }

        instance = null;
        webSocket = null;
        client.dispatcher().executorService().shutdown();
        client = null;
        connectivityManager = null;
        isConnected = false;
        executorService = null;
        schedulerRunning = false;
    }

    public synchronized void disconnectWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, null);
        }
    }

    private synchronized void reconnectWebSocket() {
        if (isNetworkAvailable() && hasInternetAccess()) {
            connectWebSocket();
        }
        else {
            if(!schedulerRunning){
                startScheduler();
            }
        }
    }

    public void setOnMessageListener(OnMessageListener onMessageListener) {
        this.onMessageListener = onMessageListener;
    }

    private boolean isNetworkAvailable() {
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return networkCapabilities != null &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private boolean hasInternetAccess() {
        boolean hasInternet = false;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL("https://www.google.com");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("HEAD");
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            int responseCode = urlConnection.getResponseCode();
            hasInternet = (200 <= responseCode && responseCode <= 399);
        } catch (IOException ignored) {
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return hasInternet;
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        super.onOpen(webSocket, response);
        log("WebSocket connected");
        isConnected = true;
        stopScheduler();
        if(onChangeListener != null){
            onChangeListener.onConnect();
        }
    }

    @Override
    public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        super.onClosing(webSocket, code, reason);
        log("WebSocket closing");
        webSocket.close(1000, null);
    }

    @Override
    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        super.onClosed(webSocket, code, reason);
        log("WebSocket closed");
        isConnected = false;
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
        log("WebSocket failed: " + t);
        isConnected = false;
        startScheduler();
        if(onChangeListener != null){
            onChangeListener.onDisconnect();
        }

    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        super.onMessage(webSocket, text);
        log("WebSocket message: " + text + "from websocket: " + webSocket);
        if (onMessageListener != null) {
            onMessageListener.onMessage(text);
        }
    }

    private synchronized void startScheduler() {
        if (!schedulerRunning) {
            schedulerRunning = true;
            if(executorService.isShutdown()){
                executorService = Executors.newSingleThreadExecutor();
            }
            executorService.execute(schedulerRunnable);
        }
    }

    public void stopScheduler() {
        if(schedulerRunning){
            schedulerRunning = false;
            executorService.shutdownNow();
        }
    }

    public void log(String text) {
        Log.d("ActionsController", text);
        FilesManager.logStatus(text);
    }

    public void sendMessage(String message) {
        if (webSocket != null && webSocket.send(message)) {
            log("WebSocket sent: " + message);
        }
    }

    Runnable schedulerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isConnected) {
                schedulerRunning = false;
                return;
            }
            log("Checking connection");
            if ((webSocket == null || !webSocket.send("{\"type\": \"ping\"}"))) {
                reconnectWebSocket();
            }

            try {
                Thread.sleep(30000);
                executorService.execute(this);
            } catch (InterruptedException ignored) {
            }
        }
    };

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    public interface OnMessageListener {
        void onMessage(String message);
    }
}

