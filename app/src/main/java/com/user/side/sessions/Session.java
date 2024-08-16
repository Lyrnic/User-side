package com.user.side.sessions;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.user.side.constants.Actions;
import com.user.side.constants.Constants;
import com.user.side.listeners.OnRequestSendMessageListener;
import com.user.side.listeners.OnSessionCloseListener;

import org.json.JSONException;
import org.json.JSONObject;

public class Session {
    private String adminToken;
    private int id;
    OnRequestSendMessageListener onRequestSendMessageListener;
    OnSessionCloseListener onSessionCloseListener;
    Context context;
    public Session(Context context, String adminToken, OnRequestSendMessageListener onRequestSendMessageListener, int id) {
        this.adminToken = adminToken;
        this.id = id;
        this.onRequestSendMessageListener = onRequestSendMessageListener;
        this.context = context;
    }

    public void setOnSessionCloseListener(OnSessionCloseListener onSessionCloseListener) {
        this.onSessionCloseListener = onSessionCloseListener;
    }

    public Context getContext() {
        return context;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getAdminToken() {
        return adminToken;
    }
    public void onMessageReceived(String message) {
        Log.d("SessionLogs", getId() + " session received message: " + message);
        try{
            JSONObject jsonObject = new JSONObject(message);
            String action = jsonObject.getString(Constants.ACTION_KEY);
            if(action.equals(Actions.ACTION_CLOSE_SESSION)){
                close();
            }
        }
        catch (JSONException ignored){
        }
    }
    public void sendMessage(String message) {
        try {
           JSONObject jsonObject= new JSONObject(message);
           jsonObject.put(Constants.RECEIVER_TOKEN_KEY, getAdminToken());
           jsonObject.put(Constants.SESSION_ID_KEY, getId());
           message = jsonObject.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        if(onRequestSendMessageListener != null){
            onRequestSendMessageListener.onRequestSendMessage(message);
        }

    }
    public void close() {
        context = null;
        onRequestSendMessageListener = null;
        adminToken = null;
        onSessionCloseListener.onClose(this);
        onSessionCloseListener = null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof Session){
            return ((Session) obj).id == id;

        }
        return super.equals(obj);
    }
}
