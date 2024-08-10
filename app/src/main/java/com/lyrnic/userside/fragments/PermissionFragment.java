package com.lyrnic.userside.fragments;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lyrnic.userside.R;
import com.lyrnic.userside.utilities.PermissionsTextUtils;
import com.lyrnic.userside.utilities.PermissionsUtilities;
import com.lyrnic.userside.utilities.XiaomiUtilities;

import java.io.IOException;
import java.util.ArrayList;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class PermissionFragment extends Fragment {

    TextView permissionDescription;
    GifImageView permissionGif;
    Button enableButton;

    public PermissionFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.permission_request_layout, container, false);
        permissionDescription = view.findViewById(R.id.permission_description);
        permissionGif = view.findViewById(R.id.permission_gif);
        enableButton = view.findViewById(R.id.enable_button);

        final MediaController mc = new MediaController(getContext());

        switch (getArguments().getString("permission")) {

            case PermissionsUtilities.HAS_ACCESS_RESTRICTED_PERMISSION:
                permissionDescription.setText(PermissionsTextUtils.Companion.getRestrictedSettingsText(getContext()));

                enableButton.setOnClickListener(v -> {
                    PermissionsUtilities.accessRestricted = true;
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                    startActivity(intent);
                });
                break;
            case PermissionsUtilities.MIUI_CAN_DISPLAY_OVERLAY_PERMISSION:
                permissionDescription.setText(PermissionsTextUtils.Companion.getMiuiDisplayPopUpPermissionText(getContext()));

                enableButton.setOnClickListener(v -> {
                    startActivity(XiaomiUtilities.getPermissionManagerIntent(getContext()));
                });
                break;
            case PermissionsUtilities.NOTIFICATION_LISTENER_PERMISSION:
                permissionDescription.setText(PermissionsTextUtils.Companion.getNotificationListenerPermissionText(getContext()));
                try {
                    GifDrawable gifFromAssets = new GifDrawable(getContext().getResources(), R.drawable.notification);
                    permissionGif.setImageDrawable(gifFromAssets);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                enableButton.setOnClickListener(v -> {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                });
                break;
            case PermissionsUtilities.ACCESSIBILITY_PERMISSION:
                permissionDescription.setText(PermissionsTextUtils.Companion.getAccessibilityText(getContext()));

                enableButton.setOnClickListener(v -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);

                });
                break;
            case PermissionsUtilities.IS_APP_BATTERY_OPTIMIZATION_ENABLED:
                permissionDescription.setText(PermissionsTextUtils.Companion.getBatteryOptimizationPermissionText(getContext()));

                enableButton.setOnClickListener(v -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                    startActivity(intent);
                });
                break;
            case PermissionsUtilities.IS_MANAGE_STORAGE_ALLOWED:
                permissionDescription.setText(PermissionsTextUtils.Companion.getManageStoragePermissionText(getContext()));

                enableButton.setOnClickListener(v -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                });
                break;
            case PermissionsUtilities.SCHEDULE_EXACT_ALARM_PERMISSION:
                permissionDescription.setText(R.string.schedule_alarm_permission_message);

                enableButton.setOnClickListener(v -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                });
                break;
            case PermissionsUtilities.HAS_DRAW_OVERLAY_PERMISSION:
                permissionDescription.setText(PermissionsTextUtils.Companion.getDrawOverlayPermissionText(getContext()));

                enableButton.setOnClickListener(v -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                    startActivity(intent);
                });
                break;
            case PermissionsUtilities.MIUI_AUTOSTART_PERMISSION:
                permissionDescription.setText(PermissionsTextUtils.Companion.getMiuiAutoStartPermissionText(getContext()));

                enableButton.setOnClickListener(v -> {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                    startActivity(intent);
                });
                break;
            case PermissionsUtilities.NORMAL_PERMISSIONS:
                permissionDescription.setText(R.string.allow_normal_permissions_message);

                enableButton.setOnClickListener(v -> {
                    ArrayList<String> permissions = new ArrayList<>();
                    if (!PermissionsUtilities.canAccessContacts(getContext())) {
                        permissions.add(Manifest.permission.READ_CONTACTS);
                    }
                    if (!PermissionsUtilities.canAccessStorage(getContext()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                    if (!PermissionsUtilities.checkPostNotificationPermission(getContext())) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS);
                    }
                    if (!PermissionsUtilities.checkReadPhoneStatePermission(getContext())) {
                        permissions.add(Manifest.permission.READ_PHONE_STATE);
                    }
                    if (!PermissionsUtilities.checkCallPermission(getContext())) {
                        permissions.add(Manifest.permission.CALL_PHONE);
                    }
                    if (!PermissionsUtilities.canReadSmsMessages(getContext())) {
                        permissions.add(Manifest.permission.READ_SMS);
                    }
                    if (!PermissionsUtilities.canReadCallsLogs(getContext())) {
                        permissions.add(Manifest.permission.READ_CALL_LOG);
                    }

                    String[] permissionsArray = new String[permissions.size()];
                    permissions.toArray(permissionsArray);
                    requestPermissions(permissionsArray, 1);
                });
                break;

        }

        mc.setMediaPlayer((GifDrawable) permissionGif.getDrawable());
        mc.show();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PermissionsUtilities.checkPermission(getContext(), getArguments().getString("permission"))) {
            enableButton.setEnabled(false);
        }
    }

    public static PermissionFragment getPermissionFragment(String permission) {
        Bundle bundle = new Bundle();
        bundle.putString("permission", permission);
        PermissionFragment permissionFragment = new PermissionFragment();
        permissionFragment.setArguments(bundle);
        return permissionFragment;
    }
}
