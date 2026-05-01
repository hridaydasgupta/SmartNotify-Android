package com.smartnotify.app.service.listener;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.smartnotify.app.data.repository.NotificationRepository;
import com.smartnotify.app.util.PriorityConstants;
import com.smartnotify.app.util.TimeUtils;

public class SmartNotificationListenerService extends NotificationListenerService {

    private NotificationRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        // Repository initialize kar rahe hain taaki DB aur Preferences use kar sakein
        repository = new NotificationRepository(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        String packageName = sbn.getPackageName();

        // 1. Khud ke app ke notifications ko block nahi karna hai (Infinite loop se bachne ke liye)
        if (packageName.equals(getPackageName())) return;

        // 2. Critical Notifications (Calls, Alarms, Music Player) ko hamesha aane dena hai
        if (isCritical(sbn)) return;

        // 3. MASTER SWITCH CHECK: Agar user ne Tile se app OFF kiya hai, toh sab pass hone do
        if (!repository.getPreferences().isMasterSwitchActive()) return;

        // 4. App ki Priority check karo DB se
        int priority = repository.getAppPriority(packageName);

        // Agar High Priority hai ya Unassigned hai, toh screen par aane do
        if (priority == PriorityConstants.HIGH || priority == PriorityConstants.UNASSIGNED) {
            return;
        }

        // Notification ka data extract karo (title aur message) save karne ke liye
        String title = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE, "New Notification");
        CharSequence textCharSeq = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
        String text = (textCharSeq != null) ? textCharSeq.toString() : "";
        long postTime = sbn.getPostTime();

        // 5. MEDIUM PRIORITY LOGIC (Focus Time Check)
        if (priority == PriorityConstants.MEDIUM) {
            String focusStart = repository.getPreferences().getFocusStartTime();
            String focusEnd = repository.getPreferences().getFocusEndTime();

            // Agar abhi Focus Time chal raha hai -> Block & Save
            if (TimeUtils.isTimeBetween(focusStart, focusEnd)) {
                cancelNotification(sbn.getKey()); // Notification screen se gayab
                repository.saveBlockedNotification(packageName, title, text, postTime, priority); // DB me save
                Log.d("SmartNotify", "Blocked Medium Priority (Focus Mode ON): " + packageName);
            }
        }

        // 6. LOW PRIORITY LOGIC (Window Check)
        else if (priority == PriorityConstants.LOW) {
            String lowStart = repository.getPreferences().getLowPriorityStartTime();
            String lowEnd = repository.getPreferences().getLowPriorityEndTime();

            // Agar abhi Low Priority Window NAHI chal rahi hai -> Block & Save
            if (!TimeUtils.isTimeBetween(lowStart, lowEnd)) {
                cancelNotification(sbn.getKey()); // Notification screen se gayab
                repository.saveBlockedNotification(packageName, title, text, postTime, priority); // DB me save
                Log.d("SmartNotify", "Blocked Low Priority (Outside Window): " + packageName);
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Jab user khud swipe karke notification hatata hai
        Log.d("SmartNotify", "Notification removed by user: " + sbn.getPackageName());
    }

    /**
     * Check karta hai ki notification foreground service, phone call, ya alarm ka toh nahi hai.
     * Isey hum block nahi karenge.
     */
    private boolean isCritical(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        boolean isForegroundService = (notification.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0;
        boolean isOngoing = (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        return isForegroundService || isOngoing;
    }
}