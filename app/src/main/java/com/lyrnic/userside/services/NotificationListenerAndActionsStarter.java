package com.lyrnic.userside.services;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.lyrnic.userside.firebase.FirebaseActionsReceiver;

public class NotificationListenerAndActionsStarter extends NotificationListenerService {


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return super.onBind(intent);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();

        Intent intent = new Intent(getApplicationContext(), FirebaseActionsReceiver.class);
        intent.setAction("com.google.firebase.MESSAGING_EVENT");
        startService(intent);

        Log.d("NotificationListenerAndActionsStarter", FirebaseMessaging.getInstance().isAutoInitEnabled() + "");
    }
}