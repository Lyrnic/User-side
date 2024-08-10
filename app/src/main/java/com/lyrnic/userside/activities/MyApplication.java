package com.lyrnic.userside.activities;

import android.app.Application;

import com.lyrnic.userside.managers.ExceptionsHandler;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionsHandler());
    }
}
