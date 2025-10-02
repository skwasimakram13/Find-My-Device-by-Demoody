package com.demoody.findmydevice.models;

import com.google.gson.JsonObject;

public class RemoteCommand {
    public enum CommandType {
        LOCK, ALARM, GET_LOCATION, SHOW_MESSAGE, WIPE
    }
    
    private int id;
    private String device_id;
    private String type;
    private JsonObject payload;
    private String status;
    private long created_at;
    private long executed_at;
    
    public RemoteCommand() {}
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getDevice_id() {
        return device_id;
    }
    
    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public CommandType getCommandType() {
        try {
            return CommandType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public JsonObject getPayload() {
        return payload;
    }
    
    public void setPayload(JsonObject payload) {
        this.payload = payload;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getCreated_at() {
        return created_at;
    }
    
    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }
    
    public long getExecuted_at() {
        return executed_at;
    }
    
    public void setExecuted_at(long executed_at) {
        this.executed_at = executed_at;
    }
}