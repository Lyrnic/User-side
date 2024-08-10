package com.lyrnic.userside.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lyrnic.userside.listeners.DeviceExistenceListener;
import com.lyrnic.userside.managers.TokenManager;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    public static String API_URL = "https://devices.bormaa.com/api/";
    public static void checkDeviceExistence(String token, DeviceExistenceListener deviceExistenceListener) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(API_URL + "getDevice/"+ "false/" + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response){
                deviceExistenceListener.onResult(response.isSuccessful());
                response.close();
            }
        });
    }
    public static boolean saveDevice(String deviceJson) {
        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(deviceJson, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(API_URL + "device_control")
                .post(body)
                .build();

        boolean success;

        try {
            Response response = client.newCall(request).execute();

            success = response.isSuccessful();

            response.close();
        } catch (IOException e) {
            Log.e(ApiClient.class.getSimpleName(), "error while saving device: ", e);
            success = false;
        }

        return success;
    }

    public static Request getWebSocketRequest(String token){
        return new Request.Builder()
                .url("wss://websocket.bormaa.com/" + "?token=" + token + "&" + "admin=false")
                .build();
    }


}
