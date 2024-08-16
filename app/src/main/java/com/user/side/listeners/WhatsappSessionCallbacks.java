package com.user.side.listeners;

import com.user.side.sessions.WhatsappSession;

public interface WhatsappSessionCallbacks {
    void onSessionCreated(WhatsappSession session);
    void onRequestStartWhatsappWebSession();
    void onReceiveWhatsappLinkCode(String code);
}
