package com.lyrnic.userside.listeners;

import com.lyrnic.userside.sessions.Session;

public interface OnSessionCloseListener {
    void onClose(Session session);
}
