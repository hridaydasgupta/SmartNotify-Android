package com.smartnotify.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.smartnotify.app.data.prefs.UserPreferences;
import com.smartnotify.app.worker.WorkScheduler;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Check karo ki intent sach me Phone Restart ka hi hai na
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) { // HTC/Samsung fast boot support

            Log.d("SmartNotify_Boot", "Phone restarted! Waking up the Silent Guardian...");

            UserPreferences prefs = new UserPreferences(context);

            // 🚀 Safety Check: Agar Master Switch ON hai tabhi alarms lagao
            if (prefs.isMasterSwitchActive()) {

                // Database se Focus End Time aur Low Priority Start Time nikal lo
                String focusEndTime = prefs.getFocusEndTime();
                String lowWindowTime = prefs.getLowPriorityStartTime();

                // 🔥 Null Check: Agar time properly set hai tabhi schedule karo
                if (focusEndTime != null && !focusEndTime.isEmpty()) {
                    WorkScheduler.scheduleMediumPriorityRelease(context, focusEndTime);
                    Log.d("SmartNotify_Boot", "Medium Priority schedule restored for: " + focusEndTime);
                }

                if (lowWindowTime != null && !lowWindowTime.isEmpty()) {
                    WorkScheduler.scheduleLowPriorityRelease(context, lowWindowTime);
                    Log.d("SmartNotify_Boot", "Low Priority schedule restored for: " + lowWindowTime);
                }

                Log.d("SmartNotify_Boot", "All background schedules restored successfully!");

            } else {
                Log.d("SmartNotify_Boot", "Master Switch is OFF. Alarms not restored.");
            }
        }
    }
}