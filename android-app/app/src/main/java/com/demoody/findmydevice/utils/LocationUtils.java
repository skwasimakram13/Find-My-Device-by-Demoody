package com.demoody.findmydevice.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationUtils {
    
    public interface LocationCallback {
        void onLocationReceived(Location location);
        void onLocationError(String error);
    }
    
    public static void getCurrentLocation(Context context, LocationCallback callback) {
        if (!hasLocationPermission(context)) {
            callback.onLocationError("Location permission not granted");
            return;
        }
        
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            callback.onLocationReceived(location);
                        } else {
                            // Request fresh location
                            requestFreshLocation(context, callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        callback.onLocationError("Failed to get location: " + e.getMessage());
                    });
        } catch (SecurityException e) {
            callback.onLocationError("Security exception: " + e.getMessage());
        }
    }
    
    private static void requestFreshLocation(Context context, LocationCallback callback) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build();
        
        com.google.android.gms.location.LocationCallback locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLastLocation();
                    callback.onLocationReceived(location);
                    fusedLocationClient.removeLocationUpdates(this);
                }
            }
        };
        
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            
            // Remove updates after 30 seconds to prevent battery drain
            android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                callback.onLocationError("Location request timeout");
            }, 30000);
            
        } catch (SecurityException e) {
            callback.onLocationError("Security exception: " + e.getMessage());
        }
    }
    
    public static boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    public static boolean hasBackgroundLocationPermission(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not required on older versions
    }
    
    public static boolean isLocationEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                   locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        return false;
    }
    
    public static String getLocationProvider(Location location) {
        if (location != null) {
            return location.getProvider();
        }
        return "unknown";
    }
    
    public static double getLocationAccuracy(Location location) {
        if (location != null && location.hasAccuracy()) {
            return location.getAccuracy();
        }
        return -1;
    }
}