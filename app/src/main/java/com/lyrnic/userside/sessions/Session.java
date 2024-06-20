package com.lyrnic.userside.sessions;

import okhttp3.WebSocket;

public class Session {
    private final String adminToken;
    private int id;
    private WebSocket webSocket;
    public Session(String adminToken, WebSocket webSocket, int id) {
        this.adminToken = adminToken;
        this.id = id;
        this.webSocket = webSocket;
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
    public WebSocket getWebSocket() {
        return webSocket;
    }
    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }
    public void onMessageReceived(String message) {

    }
}
