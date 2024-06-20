package com.lyrnic.userside.managers;

import android.content.Context;
import android.os.Environment;

import com.lyrnic.userside.utilities.PermissionsUtilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FilesManager {
    public static boolean fileExists(Context context,String path){
        if(!PermissionsUtilities.canAccessStorage(context)){
            return false;
        }
        return new File(path).exists();
    }
    public static void logStatus(Context context,String status) throws IOException {
        if(!PermissionsUtilities.canAccessStorage(context)){
            return;
        }
        File file = new File(Environment.getExternalStorageDirectory()+"/log.txt");

        if(!file.exists()){
            file.createNewFile();
        }

        FileWriter fileWriter = new FileWriter(file,true);
        fileWriter.write(buildLine(status));
        fileWriter.close();
    }
    public static String buildLine(String text){
        StringBuilder builder = new StringBuilder();
        builder.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        builder.append(": ");
        builder.append(text);
        builder.append("\n");
        return builder.toString();
    }

}
