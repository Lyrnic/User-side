package com.lyrnic.userside.managers;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class TokenManager {
    private static final String PREFS_NAME = "token_prefs";
    private static final String KEY_TOKEN = "device_token";

    public static boolean hasToken(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).contains(KEY_TOKEN);
    }

    public static void generateToken(Context context) {
        String token = UUID.randomUUID().toString();
        saveToken(context, token);
    }

    public static String getToken(Context context) {
        if (!hasToken(context)) {
            generateToken(context);
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, null);
    }

    private static void saveToken(Context context, String token) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }
}
