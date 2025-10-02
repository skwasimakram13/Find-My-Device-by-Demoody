package com.demoody.findmydevice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import com.demoody.findmydevice.services.CommandExecutorService;
import com.demoody.findmydevice.utils.PreferenceManager;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }
        
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }
        
        try {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null) {
                return;
            }
            
            for (Object pdu : pdus) {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                String messageBody = smsMessage.getMessageBody();
                String sender = smsMessage.getOriginatingAddress();
                
                Log.d(TAG, "Received SMS from: " + sender + ", Message: " + messageBody);
                
                if (isValidCommand(context, messageBody)) {
                    processCommand(context, messageBody, sender);
                    // Abort broadcast to prevent other apps from processing this SMS
                    abortBroadcast();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS", e);
        }
    }
    
    private boolean isValidCommand(Context context, String messageBody) {
        if (messageBody == null || messageBody.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = messageBody.trim().split("\\s+");
        if (parts.length < 3) {
            return false;
        }
        
        // Format: FINDMY <device-id> <secret-token> [command]
        if (!"FINDMY".equalsIgnoreCase(parts[0])) {
            return false;
        }
        
        PreferenceManager preferenceManager = new PreferenceManager(context);
        String deviceId = preferenceManager.getDeviceId();
        String smsSecret = preferenceManager.getSmsSecret();
        
        if (deviceId == null || smsSecret == null) {
            return false;
        }
        
        return deviceId.equals(parts[1]) && smsSecret.equals(parts[2]);
    }
    
    private void processCommand(Context context, String messageBody, String sender) {
        String[] parts = messageBody.trim().split("\\s+");
        
        String command = "LOCATE"; // Default command
        if (parts.length > 3) {
            command = parts[3].toUpperCase();
        }
        
        Log.d(TAG, "Processing SMS command: " + command + " from " + sender);
        
        Intent serviceIntent = new Intent(context, CommandExecutorService.class);
        serviceIntent.putExtra("command_type", command);
        serviceIntent.putExtra("sender", sender);
        serviceIntent.putExtra("source", "SMS");
        
        context.startService(serviceIntent);
    }
}