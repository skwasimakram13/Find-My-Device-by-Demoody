package com.demoody.findmydevice.services;

import android.content.Intent;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.demoody.findmydevice.utils.PreferenceManager;
import java.util.Map;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    private static final String TAG = "FCMService";
    
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }
        
        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }
    
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        
        // Send token to server
        sendTokenToServer(token);
    }
    
    private void handleDataMessage(Map<String, String> data) {
        String commandType = data.get("command_type");
        String deviceId = data.get("device_id");
        
        // Verify this message is for our device
        PreferenceManager preferenceManager = new PreferenceManager(this);
        if (!preferenceManager.getDeviceId().equals(deviceId)) {
            Log.w(TAG, "Received command for different device ID");
            return;
        }
        
        if (commandType != null) {
            Intent serviceIntent = new Intent(this, CommandExecutorService.class);
            serviceIntent.putExtra("command_type", commandType);
            serviceIntent.putExtra("source", "FCM");
            
            // Add any additional data
            for (Map.Entry<String, String> entry : data.entrySet()) {
                serviceIntent.putExtra(entry.getKey(), entry.getValue());
            }
            
            startService(serviceIntent);
        }
    }
    
    private void sendTokenToServer(String token) {
        // TODO: Implement API call to update FCM token on server
        PreferenceManager preferenceManager = new PreferenceManager(this);
        // Store token locally for now
        // In a real implementation, you would send this to your server
        Log.d(TAG, "FCM token updated: " + token);
    }
}