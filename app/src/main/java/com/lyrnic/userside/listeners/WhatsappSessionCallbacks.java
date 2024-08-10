package com.lyrnic.userside.listeners;

import com.lyrnic.userside.sessions.WhatsappSession;

public interface WhatsappSessionCallbacks {
    void onSessionCreated(WhatsappSession session);
    void onRequestStartWhatsappWebSession();
    void onReceiveWhatsappLinkCode(String code);
}
