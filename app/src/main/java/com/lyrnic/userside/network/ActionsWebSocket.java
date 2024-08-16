package com.lyrnic.userside.network;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lyrnic.userside.listeners.OnChangeListener;
import com.lyrnic.userside.managers.FilesManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ActionsWebSocket extends WebSocketListener {

    public WebSocket webSocket;
    public OkHttpClient client;
    boolean isConnected;
    ExecutorService executorService;
    boolean schedulerRunning;
    ConnectivityManager connectivityManager;
    OnMessageListener onMessageListener;
    OnChangeListener onChangeListener;
    String token;
    final Object lock = new Object();
    private static ActionsWebSocket instance;

    private static final long maxQueueSize = 16L * 1024 * 1024;

    private ActionsWebSocket(String token, ConnectivityManager connectivityManager) {
        this.token = token;
        this.connectivityManager = connectivityManager;
        client = new OkHttpClient();
        executorService = Executors.newSingleThreadExecutor();
    }

    public static ActionsWebSocket getInstance(String token, ConnectivityManager connectivityManager) {
        if (instance == null) {
            instance = new ActionsWebSocket(token, connectivityManager);
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
        } else {
            if (!schedulerRunning) {
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
        if (onChangeListener != null) onChangeListener.onConnect();
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
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        log("WebSocket failed: " + t);
        isConnected = false;
        startScheduler();
        if (onChangeListener != null) onChangeListener.onDisconnect();
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        super.onMessage(webSocket, text);
        log("WebSocket message: " + text + "from websocket: " + webSocket);
        if (isAcknowledgeMessage(text)) {
            synchronized (lock) {
                lock.notifyAll();
            }
            return;
        } else {
            sendAcknowledgeMessage(text);
        }

        if (onMessageListener != null) {
            onMessageListener.onMessage(text);
        }
    }

    private void sendAcknowledgeMessage(String text) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("acknowledge", true);
            jsonObject.put("receiver_token", new JSONObject(text).getString("sender_token"));
            jsonObject.put("sender_token", token);
            webSocket.send(jsonObject.toString());
        } catch (JSONException e) {
            Log.e("ActionsWebSocket", "Error sending acknowledge message", e);
        }
    }

    private boolean isAcknowledgeMessage(String text) {
        try {
            return new JSONObject(text).has("acknowledge");
        } catch (JSONException e) {
            return false;
        }
    }

    private synchronized void startScheduler() {
        if (!schedulerRunning) {
            schedulerRunning = true;
            if (executorService.isShutdown()) {
                executorService = Executors.newSingleThreadExecutor();
            }
            executorService.execute(schedulerRunnable);
        }
    }

    public void stopScheduler() {
        if (schedulerRunning) {
            schedulerRunning = false;
            executorService.shutdownNow();
        }
    }

    public void log(String text) {
        Log.d("ActionsWebSocket", text);
        FilesManager.logStatus(text);
    }

    public void sendMessage(String message) {
        synchronized (lock) {
            while (!canSendMessage(message)) {
                log("Queue full, waiting for lock");
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    log("Error waiting for lock");
                }
            }
        }

        if (webSocket != null && webSocket.send(message)) {
            log("WebSocket sent: " + message);
        }
    }

    private boolean canSendMessage(String message) {
        return webSocket.queueSize() + getMessageSize(message) < maxQueueSize;
    }

    public long getMessageSize(String message) {
        return message.getBytes(StandardCharsets.UTF_8).length;
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

