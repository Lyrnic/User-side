package com.user.side.activities;

import android.app.Application;

import com.user.side.managers.ExceptionsHandler;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionsHandler());
    }
}
