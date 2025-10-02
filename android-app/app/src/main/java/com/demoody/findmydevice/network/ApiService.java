package com.demoody.findmydevice.network;

import com.demoody.findmydevice.models.ApiResponse;
import com.demoody.findmydevice.models.DeviceRegistration;
import com.demoody.findmydevice.models.LocationData;
import com.demoody.findmydevice.models.RemoteCommand;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    
    @POST("api/register_device")
    Call<ApiResponse<Void>> registerDevice(@Body DeviceRegistration registration);
    
    @POST("api/devices/{device_id}/location")
    Call<ApiResponse<Void>> uploadLocation(
            @Path("device_id") String deviceId,
            @Header("Authorization") String token,
            @Body LocationData location
    );
    
    @GET("api/devices/{device_id}/commands/poll")
    Call<ApiResponse<List<RemoteCommand>>> pollCommands(
            @Path("device_id") String deviceId,
            @Header("Authorization") String token,
            @Query("since_id") int sinceId
    );
    
    @POST("api/devices/{device_id}/commands/{command_id}/ack")
    Call<ApiResponse<Void>> acknowledgeCommand(
            @Path("device_id") String deviceId,
            @Path("command_id") int commandId,
            @Header("Authorization") String token,
            @Body CommandAcknowledgment ack
    );
    
    @POST("api/devices/{device_id}/sim_change")
    Call<ApiResponse<Void>> reportSimChange(
            @Path("device_id") String deviceId,
            @Header("Authorization") String token,
            @Body SimChangeReport report
    );
    
    public static class CommandAcknowledgment {
        private String status;
        private String message;
        private long executed_at;
        
        public CommandAcknowledgment(String status, String message, long executedAt) {
            this.status = status;
            this.message = message;
            this.executed_at = executedAt;
        }
        
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public long getExecuted_at() { return executed_at; }
    }
    
    public static class SimChangeReport {
        private SimInfo old_sim;
        private SimInfo new_sim;
        private LocationData location;
        
        public SimChangeReport(SimInfo oldSim, SimInfo newSim, LocationData location) {
            this.old_sim = oldSim;
            this.new_sim = newSim;
            this.location = location;
        }
        
        public SimInfo getOld_sim() { return old_sim; }
        public SimInfo getNew_sim() { return new_sim; }
        public LocationData getLocation() { return location; }
    }
    
    public static class SimInfo {
        private String serial;
        private String operator;
        private String number;
        
        public SimInfo(String serial, String operator, String number) {
            this.serial = serial;
            this.operator = operator;
            this.number = number;
        }
        
        public String getSerial() { return serial; }
        public String getOperator() { return operator; }
        public String getNumber() { return number; }
    }
}