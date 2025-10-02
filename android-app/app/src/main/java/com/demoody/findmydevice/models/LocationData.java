package com.demoody.findmydevice.models;

public class LocationData {
    private double lat;
    private double lng;
    private double accuracy;
    private long timestamp;
    private String provider;
    
    public LocationData() {}
    
    public LocationData(double lat, double lng, double accuracy, long timestamp, String provider) {
        this.lat = lat;
        this.lng = lng;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
        this.provider = provider;
    }
    
    public double getLat() {
        return lat;
    }
    
    public void setLat(double lat) {
        this.lat = lat;
    }
    
    public double getLng() {
        return lng;
    }
    
    public void setLng(double lng) {
        this.lng = lng;
    }
    
    public double getAccuracy() {
        return accuracy;
    }
    
    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
}