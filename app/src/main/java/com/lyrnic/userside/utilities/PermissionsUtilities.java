package com.lyrnic.userside.utilities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.waseemsabir.betterypermissionhelper.BatteryPermissionHelper;

public class PermissionsUtilities {
    public static boolean isAppBatteryOptimizationEnabled(Context context) {
        String packageName = context.getPackageName();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return !pm.isIgnoringBatteryOptimizations(packageName);
    }

    public static boolean isManageStorageNotAllowed() {
        return !Environment.isExternalStorageManager();
    }

    public static boolean isReadWriteStorageNotAllowed(Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }
    public static boolean canAccessContacts(Context context){
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;

    }


    public static boolean canAccessStorage(Context context) {
        boolean canAccessStorage = false;
        if (checkSDK30OrAbove()) {
            if (!isManageStorageNotAllowed()) {
                canAccessStorage = true;
            }
        } else {
            if (!isReadWriteStorageNotAllowed(context)) {
                canAccessStorage = true;
            }
        }
        return canAccessStorage;
    }

    public static boolean checkSDK30OrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }
}
