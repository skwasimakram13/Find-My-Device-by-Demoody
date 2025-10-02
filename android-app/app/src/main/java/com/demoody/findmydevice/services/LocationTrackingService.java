package com.demoody.findmydevice.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.demoody.findmydevice.MainActivity;
import com.demoody.findmydevice.R;
import com.demoody.findmydevice.models.ApiResponse;
import com.demoody.findmydevice.models.LocationData;
import com.demoody.findmydevice.network.ApiClient;
import com.demoody.findmydevice.utils.LocationUtils;
import com.demoody.findmydevice.utils.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocationTrackingService extends Service {
    private static final String TAG = "LocationTrackingService";
    private static final String CHANNEL_ID = "location_tracking_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long LOCATION_UPDATE_INTERVAL = 5 * 60 * 1000; // 5 minutes
    
    private PreferenceManager preferenceManager;
    private ApiClient apiClient;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        preferenceManager = new PreferenceManager(this);
        apiClient = ApiClient.getInstance(this);
        
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        
        if (!isRunning) {
            startForeground(NOTIFICATION_ID, createNotification());
            startLocationTracking();
            isRunning = true;
        }
        
        return START_STICKY; // Restart if killed
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        
        stopLocationTracking();
        isRunning = false;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_description));
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.location_tracking_notification_title))
                .setContentText(getString(R.string.location_tracking_notification_text))
                .setSmallIcon(R.drawable.ic_power_off) // You might want a different icon
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    
    private void startLocationTracking() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Initial location update
        updateLocation();
        
        // Schedule periodic updates
        scheduler.scheduleAtFixedRate(
                this::updateLocation,
                LOCATION_UPDATE_INTERVAL,
                LOCATION_UPDATE_INTERVAL,
                TimeUnit.MILLISECONDS
        );
        
        Log.d(TAG, "Location tracking started");
    }
    
    private void stopLocationTracking() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        Log.d(TAG, "Location tracking stopped");
    }
    
    private void updateLocation() {
        if (!preferenceManager.isDeviceRegistered()) {
            Log.w(TAG, "Device not registered, skipping location update");
            return;
        }
        
        LocationUtils.getCurrentLocation(this, new LocationUtils.LocationCallback() {
            @Override
            public void onLocationReceived(Location location) {
                uploadLocationToServer(location);
            }
            
            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "Location error: " + error);
            }
        });
    }
    
    private void uploadLocationToServer(Location location) {
        String deviceId = preferenceManager.getDeviceId();
        String authHeader = apiClient.getAuthHeader();
        
        if (deviceId == null || authHeader == null) {
            Log.w(TAG, "Missing device ID or auth token");
            return;
        }
        
        LocationData locationData = new LocationData(
                location.getLatitude(),
                location.getLongitude(),
                location.hasAccuracy() ? location.getAccuracy() : -1,
                System.currentTimeMillis(),
                location.getProvider()
        );
        
        Call<ApiResponse<Void>> call = apiClient.getApiService()
                .uploadLocation(deviceId, authHeader, locationData);
        
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "Location uploaded successfully");
                } else {
                    Log.w(TAG, "Failed to upload location: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Network error uploading location", t);
            }
        });
    }
}