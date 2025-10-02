package com.demoody.findmydevice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.demoody.findmydevice.FakeShutdownActivity;
import com.demoody.findmydevice.utils.PreferenceManager;

public class ScreenOffReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenOffReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);
        
        PreferenceManager preferenceManager = new PreferenceManager(context);
        
        // Only show fake shutdown if tracking is enabled
        if (!preferenceManager.isTrackingEnabled()) {
            return;
        }
        
        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            // Show fake shutdown overlay when screen turns off
            // This is a deterrent for casual thieves
            showFakeShutdown(context);
        } else if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
            // This broadcast is sent when shutdown is initiated
            // We can't prevent it, but we can show our overlay
            showFakeShutdown(context);
        }
    }
    
    private void showFakeShutdown(Context context) {
        try {
            Intent fakeShutdownIntent = new Intent(context, FakeShutdownActivity.class);
            fakeShutdownIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                      Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                      Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(fakeShutdownIntent);
            Log.d(TAG, "Started fake shutdown activity");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start fake shutdown activity", e);
        }
    }
}