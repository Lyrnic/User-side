package com.lyrnic.userside.sessions;

import okhttp3.WebSocket;

public class FileManagerSession extends Session{
    public FileManagerSession(String adminToken, WebSocket webSocket, int id) {
        super(adminToken, webSocket, id);
    }
}
