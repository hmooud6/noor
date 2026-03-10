package com.samsung.android.security.v8.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * PreferenceManager - إدارة التفضيلات المشفرة
 */
public class PreferenceManager {
    
    private static final String PREFS_NAME = "device_config";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_ENCRYPTION_KEY = "enc_key";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_LAST_UPDATE = "last_update";
    
    /**
     * الحصول على SharedPreferences مشفرة
     */
    private static SharedPreferences getEncryptedPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
            
            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // إذا فشل التشفير، استخدم SharedPreferences عادية
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }
    
    /**
     * التحقق إذا كانت المرة الأولى
     */
    public static boolean isFirstLaunch(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }
    
    /**
     * تعيين أن المرة الأولى انتهت
     */
    public static void setFirstLaunchCompleted(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }
    
    /**
     * حفظ مفتاح التشفير
     */
    public static void saveEncryptionKey(Context context, String key) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        prefs.edit().putString(KEY_ENCRYPTION_KEY, key).apply();
    }
    
    /**
     * الحصول على مفتاح التشفير
     */
    public static String getEncryptionKey(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        return prefs.getString(KEY_ENCRYPTION_KEY, null);
    }
    
    /**
     * حفظ معرف الجهاز
     */
    public static void saveDeviceId(Context context, String deviceId) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
    }
    
    /**
     * الحصول على معرف الجهاز
     */
    public static String getDeviceId(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        return prefs.getString(KEY_DEVICE_ID, null);
    }
    
    /**
     * حفظ وقت آخر تحديث
     */
    public static void saveLastUpdate(Context context, long timestamp) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        prefs.edit().putLong(KEY_LAST_UPDATE, timestamp).apply();
    }
    
    /**
     * الحصول على وقت آخر تحديث
     */
    public static long getLastUpdate(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        return prefs.getLong(KEY_LAST_UPDATE, 0);
    }
    
    /**
     * حفظ قيمة String
     */
    public static void saveString(Context context, String key, String value) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        prefs.edit().putString(key, value).apply();
    }
    
    /**
     * الحصول على قيمة String
     */
    public static String getString(Context context, String key, String defaultValue) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        return prefs.getString(key, defaultValue);
    }
    
    /**
     * حفظ قيمة Boolean
     */
    public static void saveBoolean(Context context, String key, boolean value) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        prefs.edit().putBoolean(key, value).apply();
    }
    
    /**
     * الحصول على قيمة Boolean
     */
    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        return prefs.getBoolean(key, defaultValue);
    }
    
    /**
     * مسح جميع البيانات
     */
    public static void clearAll(Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        prefs.edit().clear().apply();
    }
}
