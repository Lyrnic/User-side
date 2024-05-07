package com.lyrnic.userside.listeners;

import com.google.firebase.firestore.DocumentReference;

import org.json.JSONException;

import java.io.IOException;

public interface DeviceExistenceListener {
    void onResult(boolean found);
}
