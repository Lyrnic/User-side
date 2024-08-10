package com.lyrnic.userside.utilities;

import android.app.ActivityManager;
import android.content.Context;

public class ServicesUtils {
    public static boolean isServiceKilled(Context context, Class<?> serviceClass){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return false;
            }
        }
        return true;
    }
}
