package com.demoody.findmydevice.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import androidx.core.app.ActivityCompat;
import java.util.UUID;

public class DeviceUtils {
    
    public static String getDeviceId(Context context) {
        // Use Android ID as primary identifier (more privacy-friendly than IMEI)
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        
        if (androidId != null && !androidId.equals("9774d56d682e549c")) {
            return androidId;
        }
        
        // Fallback to random UUID if Android ID is not available
        return UUID.randomUUID().toString();
    }
    
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }
    
    public static String getDeviceModel() {
        return Build.MODEL;
    }
    
    public static String getOsVersion() {
        return Build.VERSION.RELEASE;
    }
    
    public static int getSdkVersion() {
        return Build.VERSION.SDK_INT;
    }
    
    public static String getSimSerial(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // On Android 10+, getSimSerialNumber requires special permission
                    return telephonyManager.getSubscriberId();
                } else {
                    return telephonyManager.getSimSerialNumber();
                }
            }
        } catch (SecurityException e) {
            // Permission not granted or restricted
        }
        
        return null;
    }
    
    public static String getSimOperator(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                return telephonyManager.getSimOperatorName();
            }
        } catch (SecurityException e) {
            // Permission not granted
        }
        
        return null;
    }
    
    public static String getPhoneNumber(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                return telephonyManager.getLine1Number();
            }
        } catch (SecurityException e) {
            // Permission not granted
        }
        
        return null;
    }
    
    public static boolean hasSimCard(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                return telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
            }
        } catch (Exception e) {
            // Handle any exceptions
        }
        
        return false;
    }
    
    public static String generateRandomToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private static String capitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}