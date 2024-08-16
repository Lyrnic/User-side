package com.user.side.broadcasts;

import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;


import com.user.side.activities.MainActivity;
import com.user.side.managers.FilesManager;
import com.user.side.services.WebsocketService;
import com.user.side.utilities.ServicesUtils;

public class AppReviver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity.scheduleReviver(context);FilesManager.logStatus("Checking app states");

        if(ServicesUtils.isServiceKilled(context ,WebsocketService.class)){FilesManager.logStatus("Websocket service killed, starting new one");
            Intent serviceIntent = new Intent(context, WebsocketService.class);
            scheduleIntent(context, serviceIntent, 55);
        }
        ActivityOptions options = ActivityOptions.makeBasic();


    }
    public static void scheduleIntent(Context context ,Intent intent, int id){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            pendingIntent = PendingIntent.getForegroundService(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        }
        else {
            pendingIntent = PendingIntent.getService(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);
    }

}
