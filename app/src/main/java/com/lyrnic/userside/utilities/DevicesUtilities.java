package com.lyrnic.userside.utilities;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.listeners.DeviceExistenceListener;
import com.lyrnic.userside.managers.TokenManager;
import com.lyrnic.userside.network.ApiClient;

import org.json.JSONException;
import org.json.JSONObject;

public class DevicesUtilities {
    public static String TAG = DevicesUtilities.class.getSimpleName();
    public static void registerDeviceToken(Context context) {

        checkDeviceExistence(context, (found) -> {
            if (!found) {
                try {
                    saveDevice(createDevice(context));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static String createDevice(Context context) throws JSONException {
        JSONObject device = new JSONObject();

        device.put(Constants.DEVICE_NAME_KEY, Build.MANUFACTURER + " " + Build.MODEL);
        device.put(Constants.DEVICES_ADMIN_KEY,"false");
        device.put(Constants.DEVICE_TOKEN_KEY, TokenManager.getToken(context));
        device.put(Constants.DEVICES_API_KEY,String.valueOf(Build.VERSION.SDK_INT));
        device.put(Constants.DEVICES_CALL_TYPE_KEY, Constants.DEVICES_CALL_TYPE_CREATE_KEY);

        return device.toString();
    }
    public static void checkDeviceExistence(Context context, DeviceExistenceListener deviceExistenceListener) {
        if(TokenManager.getToken(context) == null){
            deviceExistenceListener.onResult(false);
            return;
        }

        ApiClient.checkDeviceExistence(TokenManager.getToken(context), deviceExistenceListener);

    }
    public static void saveDevice(String deviceJson) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean saved = ApiClient.saveDevice(deviceJson);

                if (saved) {
                    Log.d(TAG, "Device saved");
                }
                else{
                    Log.d(TAG, "Device not saved");
                }
            }
        });
        thread.start();
    }
}
