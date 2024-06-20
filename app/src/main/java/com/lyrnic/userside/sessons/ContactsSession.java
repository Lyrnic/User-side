package com.lyrnic.userside.sessons;

import android.content.Context;

import okhttp3.WebSocket;

public class ContactsSession extends Session{
    Context context;
    public ContactsSession(String adminToken, WebSocket webSocket, Context context, int id) {
        super(adminToken, webSocket, id);
        this.context = context;
    }
}
