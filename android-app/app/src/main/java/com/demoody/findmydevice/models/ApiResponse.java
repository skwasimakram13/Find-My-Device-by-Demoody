package com.demoody.findmydevice.models;

public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    private String device_token;
    private int command_id;
    
    public ApiResponse() {}
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public String getDevice_token() {
        return device_token;
    }
    
    public void setDevice_token(String device_token) {
        this.device_token = device_token;
    }
    
    public int getCommand_id() {
        return command_id;
    }
    
    public void setCommand_id(int command_id) {
        this.command_id = command_id;
    }
    
    public boolean isSuccess() {
        return "ok".equals(status) || "success".equals(status);
    }
}