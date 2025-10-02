package com.demoody.findmydevice.models;

public class DeviceRegistration {
    private String device_id;
    private String device_name;
    private String auth_token;
    private String model;
    private String os_version;
    private String fcm_token;
    
    public DeviceRegistration() {}
    
    public DeviceRegistration(String deviceId, String deviceName, String authToken, 
                            String model, String osVersion, String fcmToken) {
        this.device_id = deviceId;
        this.device_name = deviceName;
        this.auth_token = authToken;
        this.model = model;
        this.os_version = osVersion;
        this.fcm_token = fcmToken;
    }
    
    public String getDevice_id() {
        return device_id;
    }
    
    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }
    
    public String getDevice_name() {
        return device_name;
    }
    
    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }
    
    public String getAuth_token() {
        return auth_token;
    }
    
    public void setAuth_token(String auth_token) {
        this.auth_token = auth_token;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getOs_version() {
        return os_version;
    }
    
    public void setOs_version(String os_version) {
        this.os_version = os_version;
    }
    
    public String getFcm_token() {
        return fcm_token;
    }
    
    public void setFcm_token(String fcm_token) {
        this.fcm_token = fcm_token;
    }
}