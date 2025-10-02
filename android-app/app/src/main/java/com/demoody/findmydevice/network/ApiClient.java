package com.demoody.findmydevice.network;

import android.content.Context;
import com.demoody.findmydevice.utils.PreferenceManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static ApiClient instance;
    private ApiService apiService;
    private PreferenceManager preferenceManager;
    
    private ApiClient(Context context) {
        preferenceManager = new PreferenceManager(context);
        
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        String baseUrl = preferenceManager.getServerUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        apiService = retrofit.create(ApiService.class);
    }
    
    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }
    
    public ApiService getApiService() {
        return apiService;
    }
    
    public String getAuthHeader() {
        String token = preferenceManager.getDeviceToken();
        if (token != null) {
            return "Bearer " + token;
        }
        return null;
    }
    
    public void updateBaseUrl(String newUrl) {
        preferenceManager.setServerUrl(newUrl);
        // Recreate the instance with new URL
        instance = new ApiClient(null);
    }
}