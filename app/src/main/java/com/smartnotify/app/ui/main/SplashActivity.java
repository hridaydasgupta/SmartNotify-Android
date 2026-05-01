package com.smartnotify.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.smartnotify.app.R;
import com.smartnotify.app.ui.setup.PrioritySetupActivity; // Yeh hum next banayenge
import com.smartnotify.app.util.NotificationPermissionUtil;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // XML hum baad me daalenge

        // 1.2 Seconds ka delay taaki user logo dekh sake
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Agar permission pehle se hai, toh direct Dashboard (Setup) pe jao
            if (NotificationPermissionUtil.hasAccess(this)) {
                startActivity(new Intent(this, PrioritySetupActivity.class));
            } else {
                // Agar nahi hai, toh Permission mangne wali screen par jao
                startActivity(new Intent(this, PermissionIntroActivity.class));
            }

            finish(); // Splash screen ko backstack se hata do

        }, 1200);
    }
}