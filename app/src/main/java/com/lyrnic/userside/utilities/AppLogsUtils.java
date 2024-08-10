package com.lyrnic.userside.utilities;

import android.content.Context;
import android.content.SharedPreferences;

public class AppLogsUtils {
    public static void saveAppLastOpenTime(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("app_logs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("last_open_time", System.currentTimeMillis());
        editor.apply();
    }
    public static long getAppLastOpenTime(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("app_logs", Context.MODE_PRIVATE);
        return sharedPreferences.getLong("last_open_time", 0);
    }
    public static boolean isAppInActiveForOneMonth(Context context) {
        long lastOpenTime = getAppLastOpenTime(context);
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastOpenTime;
        long oneMonthInMillis = 1000L * 60 * 60 * 24 * 30; // 30 days in milliseconds
        return timeDifference >= oneMonthInMillis;
    }
}
