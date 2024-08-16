package com.user.side.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import android.view.WindowManager;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.user.side.R;
import com.user.side.activities.MainActivity;
import com.user.side.managers.ActionsManager;
import com.user.side.managers.FilesManager;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class WebsocketService extends Service {
    volatile ActionsManager actionsManager;
    public static WindowManager windowManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();FilesManager.logStatus("websocket service started");
        MainActivity.scheduleReviver(this);
        if (actionsManager == null) {
            new Thread(this::initActionsManager).start();
        }
        startForeground(1, createNotification());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    public synchronized void initActionsManager() {
        if (actionsManager == null) {
            actionsManager = new ActionsManager(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::initActionsManager).start();
        return START_STICKY;
    }

    public Notification createNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("System", "System", NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("System");
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        remoteViews.setTextViewText(R.id.title, new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis()));

        return new NotificationCompat.Builder(this, "System")
                .setCustomContentView(remoteViews)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_stat_notifications_none)
                .setOnlyAlertOnce(true)
                .build();
    }

    public void restart(Intent intent) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(this, 6456, intent, PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();FilesManager.logStatus("websocket service stopped, restarting");
        if (actionsManager != null) {
            actionsManager.getWebSocket().terminate();
            actionsManager = null;
        }
        restart(new Intent(this, WebsocketService.class));
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);FilesManager.logStatus("websocket service killed, restarting");
        if (actionsManager != null) {
            actionsManager.getWebSocket().terminate();
            actionsManager = null;
        }
        restart(new Intent(this, WebsocketService.class));
    }
}
