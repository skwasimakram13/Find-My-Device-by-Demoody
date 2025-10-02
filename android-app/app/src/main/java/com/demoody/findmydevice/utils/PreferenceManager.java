package com.demoody.findmydevice.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class PreferenceManager {
    private static final String PREF_NAME = "device_prefs";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_DEVICE_TOKEN = "device_token";
    private static final String KEY_IS_REGISTERED = "is_registered";
    private static final String KEY_TRACKING_ENABLED = "tracking_enabled";
    private static final String KEY_LAST_SIM_SERIAL = "last_sim_serial";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_FAKE_SHUTDOWN_PASSWORD = "fake_shutdown_password";
    private static final String KEY_SMS_SECRET = "sms_secret";
    
    private SharedPreferences sharedPreferences;
    
    public PreferenceManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            
            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback to regular SharedPreferences if encryption fails
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }
    
    public void setDeviceId(String deviceId) {
        sharedPreferences.edit().putString(KEY_DEVICE_ID, deviceId).apply();
    }
    
    public String getDeviceId() {
        return sharedPreferences.getString(KEY_DEVICE_ID, null);
    }
    
    public void setDeviceName(String deviceName) {
        sharedPreferences.edit().putString(KEY_DEVICE_NAME, deviceName).apply();
    }
    
    public String getDeviceName() {
        return sharedPreferences.getString(KEY_DEVICE_NAME, "Unknown Device");
    }
    
    public void setDeviceToken(String token) {
        sharedPreferences.edit().putString(KEY_DEVICE_TOKEN, token).apply();
    }
    
    public String getDeviceToken() {
        return sharedPreferences.getString(KEY_DEVICE_TOKEN, null);
    }
    
    public void setDeviceRegistered(boolean registered) {
        sharedPreferences.edit().putBoolean(KEY_IS_REGISTERED, registered).apply();
    }
    
    public boolean isDeviceRegistered() {
        return sharedPreferences.getBoolean(KEY_IS_REGISTERED, false);
    }
    
    public void setTrackingEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_TRACKING_ENABLED, enabled).apply();
    }
    
    public boolean isTrackingEnabled() {
        return sharedPreferences.getBoolean(KEY_TRACKING_ENABLED, false);
    }
    
    public void setLastSimSerial(String simSerial) {
        sharedPreferences.edit().putString(KEY_LAST_SIM_SERIAL, simSerial).apply();
    }
    
    public String getLastSimSerial() {
        return sharedPreferences.getString(KEY_LAST_SIM_SERIAL, null);
    }
    
    public void setServerUrl(String url) {
        sharedPreferences.edit().putString(KEY_SERVER_URL, url).apply();
    }
    
    public String getServerUrl() {
        return sharedPreferences.getString(KEY_SERVER_URL, "https://api.findmydevice.demoody.com");
    }
    
    public void setFakeShutdownPassword(String password) {
        sharedPreferences.edit().putString(KEY_FAKE_SHUTDOWN_PASSWORD, password).apply();
    }
    
    public String getFakeShutdownPassword() {
        return sharedPreferences.getString(KEY_FAKE_SHUTDOWN_PASSWORD, "1234");
    }
    
    public void setSmsSecret(String secret) {
        sharedPreferences.edit().putString(KEY_SMS_SECRET, secret).apply();
    }
    
    public String getSmsSecret() {
        return sharedPreferences.getString(KEY_SMS_SECRET, null);
    }
    
    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }
}