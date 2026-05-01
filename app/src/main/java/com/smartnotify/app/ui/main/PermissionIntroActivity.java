package com.smartnotify.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.smartnotify.app.R;
import com.smartnotify.app.ui.setup.PrioritySetupActivity;
import com.smartnotify.app.util.NotificationPermissionUtil;

public class PermissionIntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_intro);

        AppCompatButton btnGrant = findViewById(R.id.btnContinue);

        // Button click hone par user ko direct Android ki "Notification Access" setting me bhej do
        btnGrant.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        });
    }

    // Jab user Settings se wapas app me aayega, tab check karo permission mili ya nahi
    @Override
    protected void onResume() {
        super.onResume();
        if (NotificationPermissionUtil.hasAccess(this)) {
            // User ne ALLOW kar diya hai! Ab Dashboard pe bhejo
            startActivity(new Intent(this, PrioritySetupActivity.class));
            finish();
        }
        // Agar allow nahi kiya, toh isi screen par raho taaki wo firse koshish kar sake
    }
}