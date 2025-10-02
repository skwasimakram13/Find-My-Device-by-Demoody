package com.demoody.findmydevice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.demoody.findmydevice.services.LocationTrackingService;
import com.demoody.findmydevice.utils.DeviceUtils;
import com.demoody.findmydevice.utils.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            
            PreferenceManager preferenceManager = new PreferenceManager(context);
            
            // Check for SIM change on boot
            checkSimChange(context, preferenceManager);
            
            // Restart location tracking if it was enabled
            if (preferenceManager.isTrackingEnabled()) {
                Intent serviceIntent = new Intent(context, LocationTrackingService.class);
                context.startForegroundService(serviceIntent);
                Log.d(TAG, "Restarted location tracking service");
            }
        }
    }
    
    private void checkSimChange(Context context, PreferenceManager preferenceManager) {
        String currentSimSerial = DeviceUtils.getSimSerial(context);
        String lastSimSerial = preferenceManager.getLastSimSerial();
        
        if (currentSimSerial != null && lastSimSerial != null && 
            !currentSimSerial.equals(lastSimSerial)) {
            
            Log.w(TAG, "SIM change detected on boot");
            
            // TODO: Report SIM change to server
            // This would typically trigger an immediate location report
            // and notification to the user's account
            
            preferenceManager.setLastSimSerial(currentSimSerial);
        } else if (currentSimSerial != null && lastSimSerial == null) {
            // First time setup
            preferenceManager.setLastSimSerial(currentSimSerial);
        }
    }
}