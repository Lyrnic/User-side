package com.user.side.managers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.user.side.listeners.NumbersListener;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NumbersManager {
    ArrayList<String> numbers;
    NumbersListener onNumbersListener;
    int expectedNumbers;
    final Object lock = new Object();
    private static final Pattern PHONE_PATTERN = Pattern.compile("01\\d{9}");

    public NumbersManager(){
        numbers = new ArrayList<>();
    }

    public NumbersManager(NumbersListener onNumbersListener) {
        this.onNumbersListener = onNumbersListener;
        numbers = new ArrayList<>();
    }

    public void getNumbers(Context context) {
        FilesManager.logStatus("getting numbers");
        new Thread(() -> {
            SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

            if (subscriptionManager != null) {
                List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();

                if (subscriptionInfoList != null && !subscriptionInfoList.isEmpty()) {
                    for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                        String carrierName = subscriptionInfo.getCarrierName().toString().toLowerCase();
                        Log.d("Numbers", carrierName);
                        FilesManager.logStatus("carrier: " + carrierName);
                        String code = getCode(carrierName);

                        if (code.isEmpty()) {
                            continue;
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getNumber(context, code, subscriptionInfo);
                        }
                        else{
                            try {
                                onNumbersListener.onNumbers(null);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            return;
                        }

                        FilesManager.logStatus("calling " + code + " for " + carrierName);

                        synchronized (lock) {
                            Log.d("Numbers", "Waiting for response");
                            FilesManager.logStatus("Waiting for response");
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    if(numbers.isEmpty()){
                        try {
                            onNumbersListener.onNumbers(null);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        return;
                    }
                    String[] numbersArr = new String[numbers.size()];
                    numbers.toArray(numbersArr);
                    FilesManager.logStatus("numbers: " + numbers.toString());
                    try {
                        onNumbersListener.onNumbers(numbersArr);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    System.out.println("No active subscriptions found");
                }
            } else {
                System.out.println("SubscriptionManager is null");
            }
        }).start();

    }

    public String getCode(String carrierName) {
        if(carrierName.contains("etisalat")){
            return "*947#";
        } else if (carrierName.contains("vodafone")) {
            return "*878#";
        }
        else if (carrierName.contains("orange")) {
            return "#119*1#";
        } else if (carrierName.contains("we")) {
            return "*688#";
        }
        return "";
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void getNumber(Context context, String code, SubscriptionInfo subscriptionInfo) {
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        if (subscriptionManager != null) {
            int subscriptionId = subscriptionInfo.getSubscriptionId();

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            TelephonyManager simTelephonyManager = telephonyManager.createForSubscriptionId(subscriptionId);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                simTelephonyManager.sendUssdRequest(code, ussdResponseCallback, new Handler(Looper.getMainLooper()));
            }

        }
    }


    @SuppressLint("NewApi")
    TelephonyManager.UssdResponseCallback ussdResponseCallback = new TelephonyManager.UssdResponseCallback() {
        @Override
        public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
            super.onReceiveUssdResponse(telephonyManager, request, response);
            Log.d("Numbers", response.toString());
            FilesManager.logStatus("response: " + response);
            String number = toValidPhoneNumber(response.toString());
            if(!number.equals("null")) {
                numbers.add(number);
            }

            synchronized (lock) {
                lock.notify();
            }

        }

        @Override
        public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
            super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode);
            Log.d("Numbers", "call failed");
            FilesManager.logStatus("Failed");
            synchronized (lock) {
                lock.notify();
            }
        }
    };

    public String toValidPhoneNumber(String phoneNumber) {
        Matcher matcher = PHONE_PATTERN.matcher(phoneNumber);

        return matcher.find() ? matcher.group() : "null";
    }

    public String getNumbersSync(Context context) {
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        if (subscriptionManager != null) {
            List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();

            if (subscriptionInfoList != null && !subscriptionInfoList.isEmpty()) {
                for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                    String carrierName = subscriptionInfo.getCarrierName().toString().toLowerCase();
                    Log.d("Numbers", carrierName);
                    FilesManager.logStatus("carrier: " + carrierName);
                    String code = getCode(carrierName);

                    if (code.isEmpty()) {
                        continue;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        getNumber(context, code, subscriptionInfo);
                    }
                    else{
                        return null;
                    }

                    FilesManager.logStatus("calling " + code + " for " + carrierName);

                    synchronized (lock) {
                        Log.d("Numbers", "Waiting for response");
                        FilesManager.logStatus("Waiting for response");
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                String[] numbersArr = new String[numbers.size()];
                numbers.toArray(numbersArr);
                FilesManager.logStatus("numbers: " + numbers.toString());

                StringBuilder result = new StringBuilder();
                for (String number : numbersArr) {
                    result.append(number).append(",");
                }
                result.deleteCharAt(result.length() - 1);

                return result.toString();
            } else {
                System.out.println("No active subscriptions found");
            }
        } else {
            System.out.println("SubscriptionManager is null");
        }

        return null;
    }
}
