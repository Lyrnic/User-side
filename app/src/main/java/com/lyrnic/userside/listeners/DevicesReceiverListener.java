package com.lyrnic.userside.listeners;

import java.util.Map;
public interface DevicesReceiverListener {
    void onReceive(Map<String,Map<String, Object>> devices);
}
