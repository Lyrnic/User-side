package com.lyrnic.userside.managers;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.lyrnic.userside.utilities.PermissionsUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FilesManager {
    public static boolean fileExists(String path) {
        try {
            return new File(path).exists();
        } catch (Exception e) {
            Log.e("FilesManager", "can't write log", e);
        }
        return false;
    }

    public static void logStatus(String status) {
        try {
            File file = new File(Environment.getExternalStorageDirectory() + "/user-log.txt");

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(file, true);
            fileWriter.write(buildLine(status));
            fileWriter.close();
        } catch (Exception e) {
            Log.e("FilesManager", "can't write log", e);
        }

    }

    public static String buildLine(String text) {
        StringBuilder builder = new StringBuilder();
        builder.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        builder.append(": ");
        builder.append(text);
        builder.append("\n");
        return builder.toString();
    }


    public static boolean removeFile(File file) throws Exception{

        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                removeFile(child);
            }
        }

        return file.delete();
    }

    public static boolean renameFile(String filePath1, String newName) throws Exception {

        File file = new File(filePath1);
        if (file.exists()) {
            File newFile = new File(file.getParent(), newName);
            return file.renameTo(newFile);
        }

        return false;
    }
}
