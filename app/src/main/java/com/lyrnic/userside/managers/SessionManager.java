package com.lyrnic.userside.managers;

import android.content.Context;

import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.sessons.ContactsSession;
import com.lyrnic.userside.sessons.FileManagerSession;
import com.lyrnic.userside.sessons.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.WebSocket;

public class SessionManager {
    public ArrayList<Session> sessions = new ArrayList<>();
    public int createSession(String sessionType, String adminToken, WebSocket webSocket, Object... params){
        boolean isSessionCreated = false;
        switch (sessionType) {
            case Constants.CONTACTS_SESSION_KEY:
                createContactsSession(adminToken, webSocket, (Context) params[0]);
                isSessionCreated = true;
                break;
            case Constants.FILE_MANAGER_SESSION_KEY:
                createFileManagerSession(adminToken, webSocket);
                isSessionCreated = true;
                break;
        }
        return isSessionCreated ? sessions.size() - 1 : -1;
    }
    private void createFileManagerSession(String adminToken, WebSocket webSocket) {
        FileManagerSession session =  new FileManagerSession(adminToken, webSocket, sessions.size());
        sessions.add(session);
    }
    private void createContactsSession(String adminToken, WebSocket webSocket, Context context) {
        ContactsSession session =  new ContactsSession(adminToken, webSocket, context, sessions.size());
        sessions.add(session);
    }

    public void sendToSession(String message) throws JSONException {
        JSONObject json = new JSONObject(message);

        int sessionId = json.getInt(Constants.SESSION_ID_KEY);

        sessions.get(sessionId).onMessageReceived(message);
    }
}
