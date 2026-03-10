package com.samsung.android.security.v8.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.samsung.android.security.v8.services.CoreService;

/**
 * BootReceiver - مستقبل تشغيل الجهاز
 * يبدأ الخدمة تلقائياً عند تشغيل الجهاز
 */
public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            
            Log.d(TAG, "Device booted, starting service");
            
            // بدء الخدمة الأساسية
            Intent serviceIntent = new Intent(context, CoreService.class);
            context.startForegroundService(serviceIntent);
        }
    }
}
