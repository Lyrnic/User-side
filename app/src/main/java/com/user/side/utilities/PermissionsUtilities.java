package com.user.side.utilities;

import android.Manifest;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.user.side.services.ActionsController;

import java.util.ArrayList;

public class PermissionsUtilities {
    public static final String MIUI_CAN_DISPLAY_OVERLAY_PERMISSION = "miui_permission_can_display_overlay";
    public static final String MIUI_AUTOSTART_PERMISSION = "permission_miui_autostart";
    public static final String NOTIFICATION_LISTENER_PERMISSION = "permission_notification_listener";
    public static final String POST_NOTIFICATION_PERMISSION = "permission_post_notification";
    public static final String ACCESSIBILITY_PERMISSION = "permission_accessibility";
    public static final String HAS_ACCESS_RESTRICTED_PERMISSION = "permission_has_access_restricted";
    public static final String HAS_DRAW_OVERLAY_PERMISSION = "permission_has_draw_overlay";
    public static final String IS_APP_BATTERY_OPTIMIZATION_ENABLED = "permission_is_app_battery_optimization_enabled";
    public static final String IS_MANAGE_STORAGE_ALLOWED = "permission_is_manage_storage_allowed";
    public static final String SCHEDULE_EXACT_ALARM_PERMISSION = "permission_schedule_exact_alarm";
    public static final String NORMAL_PERMISSIONS = "permission_normal_permissions";

    public static boolean accessRestricted = false;

    public static boolean isMiuiAutostartEnabled(Context context) {
        return !XiaomiUtilities.isMIUI() || XiaomiUtilities.isCustomPermissionGranted(context, XiaomiUtilities.OP_AUTO_START);
    }

    public static boolean isMiuiCanDisplayOverlay(Context context) {
        return !XiaomiUtilities.isMIUI() ||
                XiaomiUtilities.isCustomPermissionGranted(context, XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY);
    }

    public static boolean isNotificationListenerEnabled(Context context) {
        String packageName = context.getPackageName();
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (flat != null) {
            return flat.contains(packageName);
        }
        return false;
    }

    public static boolean checkPostNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkReadPhoneStatePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkCallPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isAccessibilityEnabled(Context context) {
        int accessibilityEnabled = 0;
        ComponentName expectedComponentName = new ComponentName(context, ActionsController.class);
        boolean accessibilityFound = false;
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(), android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(ActionsController.class.getSimpleName(), "error finding accessibility setting: ", e);
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String componentNameString = mStringColonSplitter.next();
                    ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

                    if (enabledService != null && enabledService.equals(expectedComponentName))
                        return true;
                }
            }
        }
        return accessibilityFound;
    }

    public static boolean checkPermission(Context context, String permission) {
        switch (permission) {
            case PermissionsUtilities.HAS_ACCESS_RESTRICTED_PERMISSION:
                return hasAccessRestrictedPerm(context);
            case PermissionsUtilities.MIUI_AUTOSTART_PERMISSION:
                return isMiuiAutostartEnabled(context);
            case PermissionsUtilities.HAS_DRAW_OVERLAY_PERMISSION:
                return hasDrawOverlayPermission(context);
            case PermissionsUtilities.MIUI_CAN_DISPLAY_OVERLAY_PERMISSION:
                return isMiuiCanDisplayOverlay(context);
            case PermissionsUtilities.NOTIFICATION_LISTENER_PERMISSION:
                return isNotificationListenerEnabled(context);
            case PermissionsUtilities.ACCESSIBILITY_PERMISSION:
                return isAccessibilityEnabled(context);
            case PermissionsUtilities.POST_NOTIFICATION_PERMISSION:
                return checkPostNotificationPermission(context);
            case PermissionsUtilities.IS_APP_BATTERY_OPTIMIZATION_ENABLED:
                return isAppBatteryOptimizationEnabled(context);
            case PermissionsUtilities.IS_MANAGE_STORAGE_ALLOWED:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    return !isManageStorageNotAllowed();
                }
                break;
            case PermissionsUtilities.SCHEDULE_EXACT_ALARM_PERMISSION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms();
                } else {
                    return true;
                }
            case PermissionsUtilities.NORMAL_PERMISSIONS:
                return checkPostNotificationPermission(context) &&
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || !isReadWriteStorageNotAllowed(context)) &&
                        canAccessContacts(context) && checkReadPhoneStatePermission(context) && checkCallPermission(context) &&
                        canReadSmsMessages(context) && canReadCallsLogs(context);

        }
        return false;
    }

    public static boolean canReadCallsLogs(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean canReadSmsMessages(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasDrawOverlayPermission(Context context) {
        return !isBiometricAuthEnabled(context) || Settings.canDrawOverlays(context);
    }

    public static boolean hasAccessRestrictedPerm(Context context) {
        if (accessRestricted || isAccessibilityEnabled(context)) {
            return true;
        }
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2;
    }

    public static ArrayList<String> getPermissionsNames(Context context) {
        ArrayList<String> permissions = new ArrayList<>();
        if (!isMiuiAutostartEnabled(context)) {
            permissions.add(MIUI_AUTOSTART_PERMISSION);
        }
        if (!isMiuiCanDisplayOverlay(context)) {
            permissions.add(MIUI_CAN_DISPLAY_OVERLAY_PERMISSION);
        }
        if (!hasAccessRestrictedPerm(context)) {
            permissions.add(HAS_ACCESS_RESTRICTED_PERMISSION);
        }
        if (!isAccessibilityEnabled(context)) {
            permissions.add(ACCESSIBILITY_PERMISSION);
        }
        if (!isNotificationListenerEnabled(context)) {
            permissions.add(NOTIFICATION_LISTENER_PERMISSION);
        }
        if (!isAppBatteryOptimizationEnabled(context)) {
            permissions.add(IS_APP_BATTERY_OPTIMIZATION_ENABLED);
        }
        if (!hasDrawOverlayPermission(context)) {
            permissions.add(HAS_DRAW_OVERLAY_PERMISSION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (isManageStorageNotAllowed()) {
                permissions.add(IS_MANAGE_STORAGE_ALLOWED);
            }
        } else {
            if (isReadWriteStorageNotAllowed(context)) {
                permissions.add(NORMAL_PERMISSIONS);
            }
        }
        if (!checkPostNotificationPermission(context)) {
            permissions.add(NORMAL_PERMISSIONS);
        }
        if (!canAccessContacts(context) && !permissions.contains(NORMAL_PERMISSIONS)) {
            permissions.add(NORMAL_PERMISSIONS);
        }
        if (!checkReadPhoneStatePermission(context) && !permissions.contains(NORMAL_PERMISSIONS)) {
            permissions.add(NORMAL_PERMISSIONS);
        }
        if (!checkCallPermission(context) && !permissions.contains(NORMAL_PERMISSIONS)) {
            permissions.add(NORMAL_PERMISSIONS);
        }
        if (!canReadSmsMessages(context) && !permissions.contains(NORMAL_PERMISSIONS)) {
            permissions.add(NORMAL_PERMISSIONS);
        }
        if (!canReadCallsLogs(context) && !permissions.contains(NORMAL_PERMISSIONS)) {
            permissions.add(NORMAL_PERMISSIONS);
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms()) {
//            permissions.add(SCHEDULE_EXACT_ALARM_PERMISSION);
//        }

        return permissions;
    }

    public static boolean isBiometricAuthEnabled(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            BiometricManager biometricManager = (BiometricManager) context.getSystemService(Context.BIOMETRIC_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
            } else {
                return biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
            }
        } else {
            return false;
        }
    }

    public static boolean isAppBatteryOptimizationEnabled(Context context) {
        String packageName = context.getPackageName();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(packageName);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static boolean isManageStorageNotAllowed() {
        return !Environment.isExternalStorageManager();
    }

    private static boolean isReadWriteStorageNotAllowed(Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean canAccessContacts(Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;

    }


    public static boolean canAccessStorage(Context context) {
        boolean canAccessStorage = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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

}
