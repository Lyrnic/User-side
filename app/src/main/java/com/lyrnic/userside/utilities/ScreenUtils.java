package com.lyrnic.userside.utilities;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

public class ScreenUtils {
    public static boolean isScreenOnAndUnlocked(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        boolean isScreenOn = powerManager.isInteractive(); // Returns true if the screen is on (interactive)
        boolean isUnlocked = !keyguardManager.isKeyguardLocked(); // Returns true if the keyguard is not locked (unlocked)

        return isScreenOn && isUnlocked;
    }
    public static IntentFilter getIntentFilterForScreenBroadcast(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        return intentFilter;
    }
}
