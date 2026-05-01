package com.smartnotify.app.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class UserPreferences {

    private static final String PREF_NAME = "smart_notify_secure_prefs";
    private SharedPreferences prefs;

    // Keys for saving data
    private static final String KEY_MASTER_SWITCH = "master_switch_active";
    private static final String KEY_FOCUS_START = "focus_start_time";
    private static final String KEY_FOCUS_END = "focus_end_time";
    private static final String KEY_LOW_PRIORITY_START = "low_priority_start";
    private static final String KEY_LOW_PRIORITY_END = "low_priority_end";

    public UserPreferences(Context context) {
        try {
            // Secure Encryption Key generate karna
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Encrypted Preferences create karna
            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            e.printStackTrace();
            // Agar kisi old phone me encryption fail ho jaye, toh normal safe preferences fallback
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    // =========================
    // MASTER SWITCH (ON / OFF)
    // =========================
    public boolean isMasterSwitchActive() {
        return prefs.getBoolean(KEY_MASTER_SWITCH, true);
    }

    public void setMasterSwitch(boolean isActive) {
        prefs.edit().putBoolean(KEY_MASTER_SWITCH, isActive).apply();
    }

    // =========================
    // FOCUS TIME (For Medium Priority)
    // =========================
    // Default: 09:00 to 13:00 (1 PM)
    public String getFocusStartTime() { return prefs.getString(KEY_FOCUS_START, "09:00"); }
    public void setFocusStartTime(String time) { prefs.edit().putString(KEY_FOCUS_START, time).apply(); }

    public String getFocusEndTime() { return prefs.getString(KEY_FOCUS_END, "13:00"); }
    public void setFocusEndTime(String time) { prefs.edit().putString(KEY_FOCUS_END, time).apply(); }

    // =========================
    // LOW PRIORITY WINDOW
    // =========================
    // Default: 20:00 (8 PM) to 22:00 (10 PM)
    public String getLowPriorityStartTime() { return prefs.getString(KEY_LOW_PRIORITY_START, "20:00"); }
    public void setLowPriorityStartTime(String time) { prefs.edit().putString(KEY_LOW_PRIORITY_START, time).apply(); }

    public String getLowPriorityEndTime() { return prefs.getString(KEY_LOW_PRIORITY_END, "22:00"); }
    public void setLowPriorityEndTime(String time) { prefs.edit().putString(KEY_LOW_PRIORITY_END, time).apply(); }
}