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
        // Check karo ki intent sach me Phone Restart ka hi hai na
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {

            Log.d("SmartNotify_Boot", "Phone restarted! Alarms ko wapas zinda kar rahe hain...");

            // 🔥 FIX: Ab hum wahi UserPreferences use kar rahe hain jo tumne Setup Activity me banaya tha
            UserPreferences prefs = new UserPreferences(context);

            // Database se Focus End Time aur Low Priority Start Time nikal lo
            String focusEndTime = prefs.getFocusEndTime();
            String lowWindowTime = prefs.getLowPriorityStartTime();

            // WorkScheduler ko wapas start kar do!
            WorkScheduler.scheduleMediumPriorityRelease(context, focusEndTime);
            WorkScheduler.scheduleLowPriorityRelease(context, lowWindowTime);

            Log.d("SmartNotify_Boot", "All background schedules restored successfully! Focus Ends: " + focusEndTime);
        }
    }
}