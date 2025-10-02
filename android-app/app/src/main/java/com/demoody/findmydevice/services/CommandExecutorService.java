package com.demoody.findmydevice.services;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.demoody.findmydevice.R;
import com.demoody.findmydevice.models.RemoteCommand;
import com.demoody.findmydevice.receivers.DeviceAdminReceiver;
import com.demoody.findmydevice.utils.LocationUtils;
import com.demoody.findmydevice.utils.PreferenceManager;

public class CommandExecutorService extends Service {
    private static final String TAG = "CommandExecutorService";
    
    private PreferenceManager preferenceManager;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminComponent;
    private MediaPlayer alarmPlayer;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        preferenceManager = new PreferenceManager(this);
        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        deviceAdminComponent = new ComponentName(this, DeviceAdminReceiver.class);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String commandType = intent.getStringExtra("command_type");
            String source = intent.getStringExtra("source");
            
            Log.d(TAG, "Executing command: " + commandType + " from " + source);
            
            executeCommand(commandType, intent);
        }
        
        stopSelf(startId);
        return START_NOT_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void executeCommand(String commandType, Intent intent) {
        try {
            switch (commandType.toUpperCase()) {
                case "LOCK":
                    executeLockCommand();
                    break;
                case "ALARM":
                case "RING":
                    executeAlarmCommand();
                    break;
                case "LOCATE":
                case "GET_LOCATION":
                    executeLocationCommand();
                    break;
                case "SHOW_MESSAGE":
                    String message = intent.getStringExtra("message");
                    executeShowMessageCommand(message);
                    break;
                case "WIPE":
                    executeWipeCommand();
                    break;
                default:
                    Log.w(TAG, "Unknown command: " + commandType);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing command: " + commandType, e);
        }
    }
    
    private void executeLockCommand() {
        if (devicePolicyManager.isAdminActive(deviceAdminComponent)) {
            devicePolicyManager.lockNow();
            Log.d(TAG, "Device locked");
            Toast.makeText(this, "Device locked remotely", Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "Device admin not active, cannot lock device");
        }
    }
    
    private void executeAlarmCommand() {
        try {
            // Stop any existing alarm
            stopAlarm();
            
            // Create and start alarm sound
            alarmPlayer = MediaPlayer.create(this, R.raw.alarm_sound);
            if (alarmPlayer == null) {
                // Fallback to system alarm sound
                alarmPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI);
            }
            
            if (alarmPlayer != null) {
                alarmPlayer.setLooping(true);
                
                // Set volume to maximum
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
                
                alarmPlayer.start();
                Log.d(TAG, "Alarm started");
                
                // Stop alarm after 2 minutes
                android.os.Handler handler = new android.os.Handler();
                handler.postDelayed(this::stopAlarm, 120000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting alarm", e);
        }
    }
    
    private void stopAlarm() {
        if (alarmPlayer != null) {
            try {
                if (alarmPlayer.isPlaying()) {
                    alarmPlayer.stop();
                }
                alarmPlayer.release();
                alarmPlayer = null;
                Log.d(TAG, "Alarm stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping alarm", e);
            }
        }
    }
    
    private void executeLocationCommand() {
        LocationUtils.getCurrentLocation(this, new LocationUtils.LocationCallback() {
            @Override
            public void onLocationReceived(Location location) {
                Log.d(TAG, "Current location: " + location.getLatitude() + ", " + location.getLongitude());
                // TODO: Send location to server or SMS sender
                Toast.makeText(CommandExecutorService.this, 
                    "Location: " + location.getLatitude() + ", " + location.getLongitude(), 
                    Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "Location error: " + error);
            }
        });
    }
    
    private void executeShowMessageCommand(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = "This device is being tracked for security purposes.";
        }
        
        // Show message as toast (in a real implementation, you might want a full-screen dialog)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.d(TAG, "Showed message: " + message);
    }
    
    private void executeWipeCommand() {
        if (devicePolicyManager.isAdminActive(deviceAdminComponent)) {
            // This is a destructive operation - be very careful
            Log.w(TAG, "WIPE command received - this will erase all data!");
            
            // In a production app, you might want additional confirmation
            // devicePolicyManager.wipeData(0);
            
            // For safety, just show a warning for now
            Toast.makeText(this, "WIPE command received but not executed (safety)", Toast.LENGTH_LONG).show();
        } else {
            Log.w(TAG, "Device admin not active, cannot wipe device");
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAlarm();
    }
}