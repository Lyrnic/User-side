package com.user.side.managers;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionsHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        FilesManager.logStatus(sStackTrace);

        System.exit(1);
    }
}
