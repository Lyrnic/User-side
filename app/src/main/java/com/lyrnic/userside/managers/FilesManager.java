package com.lyrnic.userside.managers;

import android.content.Context;

import com.lyrnic.userside.utilities.PermissionsUtilities;

import java.io.File;

public class FilesManager {
    public static boolean fileExists(Context context,String path){
        if(!PermissionsUtilities.canAccessStorage(context)){
            return false;
        }
        return new File(path).exists();
    }

}
