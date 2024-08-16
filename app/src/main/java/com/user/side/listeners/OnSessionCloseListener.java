package com.user.side.listeners;

import com.user.side.sessions.Session;

public interface OnSessionCloseListener {
    void onClose(Session session);
}
