package com.demoody.findmydevice;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.demoody.findmydevice.models.ApiResponse;
import com.demoody.findmydevice.models.DeviceRegistration;
import com.demoody.findmydevice.network.ApiClient;
import com.demoody.findmydevice.receivers.DeviceAdminReceiver;
import com.demoody.findmydevice.services.LocationTrackingService;
import com.demoody.findmydevice.utils.DeviceUtils;
import com.demoody.findmydevice.utils.LocationUtils;
import com.demoody.findmydevice.utils.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final int REQUEST_BACKGROUND_LOCATION_PERMISSION = 1002;
    private static final int REQUEST_DEVICE_ADMIN = 1003;
    
    private TextView statusText;
    private TextView deviceInfoText;
    private Button enableTrackingBtn;
    private Button enableDeviceAdminBtn;
    private Button testLocationBtn;
    private Button settingsBtn;
    private PreferenceManager preferenceManager;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminComponent;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        initializeComponents();
        updateUI();
    }
    
    private void initializeViews() {
        statusText = findViewById(R.id.statusText);
        deviceInfoText = findViewById(R.id.deviceInfoText);
        enableTrackingBtn = findViewById(R.id.enableTrackingBtn);
        enableDeviceAdminBtn = findViewById(R.id.enableDeviceAdminBtn);
        testLocationBtn = findViewById(R.id.testLocationBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        
        enableTrackingBtn.setOnClickListener(v -> handleTrackingToggle());
        enableDeviceAdminBtn.setOnClickListener(v -> enableDeviceAdmin());
        testLocationBtn.setOnClickListener(v -> testLocation());
        settingsBtn.setOnClickListener(v -> openSettings());
    }
    
    private void initializeComponents() {
        preferenceManager = new PreferenceManager(this);
        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        deviceAdminComponent = new ComponentName(this, DeviceAdminReceiver.class);
        apiClient = ApiClient.getInstance(this);
        
        // Register device if not already registered
        if (!preferenceManager.isDeviceRegistered()) {
            registerDevice();
        }
        
        updateDeviceInfo();
    }
    
    private void handleTrackingToggle() {
        if (!hasLocationPermissions()) {
            requestLocationPermissions();
            return;
        }
        
        if (preferenceManager.isTrackingEnabled()) {
            stopLocationTracking();
        } else {
            startLocationTracking();
        }
    }
    
    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_LOCATION_PERMISSION);
    }
    
    private void requestBackgroundLocationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_BACKGROUND_LOCATION_PERMISSION);
        }
    }
    
    private void startLocationTracking() {
        if (!hasLocationPermissions()) {
            Toast.makeText(this, "Location permissions required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check for background location permission on Android 10+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                requestBackgroundLocationPermission();
                return;
            }
        }
        
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        startForegroundService(serviceIntent);
        
        preferenceManager.setTrackingEnabled(true);
        updateUI();
        Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show();
    }
    
    private void stopLocationTracking() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);
        
        preferenceManager.setTrackingEnabled(false);
        updateUI();
        Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show();
    }
    
    private void enableDeviceAdmin() {
        if (!devicePolicyManager.isAdminActive(deviceAdminComponent)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Enable device administrator to allow remote lock and wipe features");
            startActivityForResult(intent, REQUEST_DEVICE_ADMIN);
        } else {
            Toast.makeText(this, "Device admin already enabled", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void registerDevice() {
        String deviceId = DeviceUtils.getDeviceId(this);
        String deviceName = DeviceUtils.getDeviceName();
        String authToken = DeviceUtils.generateRandomToken();
        
        DeviceRegistration registration = new DeviceRegistration(
                deviceId,
                deviceName,
                authToken,
                DeviceUtils.getDeviceModel(),
                DeviceUtils.getOsVersion(),
                null // FCM token will be set later
        );
        
        Call<ApiResponse<Void>> call = apiClient.getApiService().registerDevice(registration);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    preferenceManager.setDeviceId(deviceId);
                    preferenceManager.setDeviceName(deviceName);
                    preferenceManager.setDeviceToken(response.body().getDevice_token());
                    preferenceManager.setDeviceRegistered(true);
                    
                    // Generate SMS secret
                    preferenceManager.setSmsSecret(DeviceUtils.generateRandomToken().substring(0, 8));
                    
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Device registered successfully", Toast.LENGTH_SHORT).show();
                        updateUI();
                    });
                } else {
                    // Fallback to local registration
                    registerDeviceLocally(deviceId, deviceName, authToken);
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                // Fallback to local registration
                registerDeviceLocally(deviceId, deviceName, authToken);
            }
        });
    }
    
    private void registerDeviceLocally(String deviceId, String deviceName, String authToken) {
        preferenceManager.setDeviceId(deviceId);
        preferenceManager.setDeviceName(deviceName);
        preferenceManager.setDeviceToken(authToken);
        preferenceManager.setDeviceRegistered(true);
        preferenceManager.setSmsSecret(DeviceUtils.generateRandomToken().substring(0, 8));
        
        runOnUiThread(() -> {
            Toast.makeText(this, "Device registered locally (server unavailable)", Toast.LENGTH_SHORT).show();
            updateUI();
        });
    }
    
    private void updateUI() {
        StringBuilder status = new StringBuilder();
        
        // Device registration status
        if (preferenceManager.isDeviceRegistered()) {
            status.append("✓ Device registered\n");
            status.append("Device ID: ").append(preferenceManager.getDeviceId()).append("\n\n");
        } else {
            status.append("✗ Device not registered\n\n");
        }
        
        // Location tracking status
        if (preferenceManager.isTrackingEnabled()) {
            status.append("✓ Location tracking active\n");
            enableTrackingBtn.setText("Stop Tracking");
        } else {
            status.append("✗ Location tracking inactive\n");
            enableTrackingBtn.setText("Start Tracking");
        }
        
        // Device admin status
        if (devicePolicyManager.isAdminActive(deviceAdminComponent)) {
            status.append("✓ Device admin enabled\n");
            enableDeviceAdminBtn.setText("Device Admin Enabled");
            enableDeviceAdminBtn.setEnabled(false);
        } else {
            status.append("✗ Device admin disabled\n");
            enableDeviceAdminBtn.setText("Enable Device Admin");
            enableDeviceAdminBtn.setEnabled(true);
        }
        
        statusText.setText(status.toString());
    }
    
    private void updateDeviceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Device ID: ").append(preferenceManager.getDeviceId()).append("\n");
        info.append("Model: ").append(DeviceUtils.getDeviceModel()).append("\n");
        info.append("OS Version: ").append(DeviceUtils.getOsVersion()).append("\n");
        info.append("SDK: ").append(DeviceUtils.getSdkVersion()).append("\n");
        
        String simOperator = DeviceUtils.getSimOperator(this);
        if (simOperator != null) {
            info.append("SIM Operator: ").append(simOperator).append("\n");
        }
        
        info.append("Location Enabled: ").append(LocationUtils.isLocationEnabled(this) ? "Yes" : "No").append("\n");
        info.append("SMS Secret: ").append(preferenceManager.getSmsSecret()).append("\n");
        
        deviceInfoText.setText(info.toString());
    }
    
    private void testLocation() {
        if (!hasLocationPermissions()) {
            requestLocationPermissions();
            return;
        }
        
        Toast.makeText(this, "Getting location...", Toast.LENGTH_SHORT).show();
        
        LocationUtils.getCurrentLocation(this, new LocationUtils.LocationCallback() {
            @Override
            public void onLocationReceived(Location location) {
                runOnUiThread(() -> {
                    String locationInfo = String.format("Location: %.6f, %.6f\nAccuracy: %.1fm\nProvider: %s",
                            location.getLatitude(),
                            location.getLongitude(),
                            location.hasAccuracy() ? location.getAccuracy() : 0,
                            location.getProvider());
                    Toast.makeText(MainActivity.this, locationInfo, Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onLocationError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Location error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void openSettings() {
        // TODO: Implement settings activity
        Toast.makeText(this, "Settings not implemented yet", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationTracking();
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case REQUEST_BACKGROUND_LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationTracking();
                } else {
                    Toast.makeText(this, "Background location permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        
        updateUI();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_DEVICE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Device admin enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Device admin not enabled", Toast.LENGTH_SHORT).show();
            }
            updateUI();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
}