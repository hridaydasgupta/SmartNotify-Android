package com.smartnotify.app.service.listener;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.smartnotify.app.data.repository.NotificationRepository;
import com.smartnotify.app.util.PriorityConstants;
import com.smartnotify.app.util.TimeUtils;

import java.util.concurrent.ConcurrentHashMap;

public class SmartNotificationListenerService extends NotificationListenerService {

    private NotificationRepository repository;

    // 🚀 O(1) Speed ke liye Thread-Safe Cache
    private final ConcurrentHashMap<String, Integer> priorityCache = new ConcurrentHashMap<>();

    // Jab UI se user priority change karega, toh ye Receiver is cache ko clear kar dega
    private final BroadcastReceiver cacheInvalidationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.smartnotify.CACHE_UPDATE".equals(intent.getAction())) {
                priorityCache.clear();
                Log.d("SmartNotify", "Priority Cache Invalidated (Refreshed) by UI");
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new NotificationRepository(this);

        // Broadcast Receiver register kar rahe hain
        IntentFilter filter = new IntentFilter("com.smartnotify.CACHE_UPDATE");
        // Android 13+ safety flag
        registerReceiver(cacheInvalidationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(cacheInvalidationReceiver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        String packageName = sbn.getPackageName();

        // 1. Khud ke app ke notifications ko block nahi karna hai
        if (packageName.equals(getPackageName())) return;

        // 2. Critical Notifications (Calls, Alarms) ko hamesha aane dena hai
        if (isCritical(sbn)) return;

        // 3. MASTER SWITCH CHECK
        if (!repository.getPreferences().isMasterSwitchActive()) return;

        // 4. App ki Priority check karo (Fast O(1) Cache se)
        int priority = getAppPriorityFast(packageName);

        // Agar High Priority hai ya Unassigned hai, toh screen par aane do
        if (priority == PriorityConstants.HIGH || priority == PriorityConstants.UNASSIGNED) {
            return;
        }

        // Notification ka data extract karo
        String title = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE, "New Notification");
        CharSequence textCharSeq = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
        String text = (textCharSeq != null) ? textCharSeq.toString() : "";
        long postTime = sbn.getPostTime();

        // 5. MEDIUM PRIORITY LOGIC (Focus Time Check)
        if (priority == PriorityConstants.MEDIUM) {
            String focusStart = repository.getPreferences().getFocusStartTime();
            String focusEnd = repository.getPreferences().getFocusEndTime();

            if (TimeUtils.isTimeBetween(focusStart, focusEnd)) {
                cancelNotification(sbn.getKey());
                repository.saveBlockedNotification(packageName, title, text, postTime, priority);
                Log.d("SmartNotify", "Blocked Medium Priority (Focus Mode ON): " + packageName);
            }
        }

        // 6. LOW PRIORITY LOGIC (Window Check)
        else if (priority == PriorityConstants.LOW) {
            String lowStart = repository.getPreferences().getLowPriorityStartTime();
            String lowEnd = repository.getPreferences().getLowPriorityEndTime();

            if (!TimeUtils.isTimeBetween(lowStart, lowEnd)) {
                cancelNotification(sbn.getKey());
                repository.saveBlockedNotification(packageName, title, text, postTime, priority);
                Log.d("SmartNotify", "Blocked Low Priority (Outside Window): " + packageName);
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d("SmartNotify", "Notification removed by user: " + sbn.getPackageName());
    }

    private boolean isCritical(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        boolean isForegroundService = (notification.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0;
        boolean isOngoing = (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        return isForegroundService || isOngoing;
    }

    // ==========================================
    // 🧠 SMART CACHING ENGINE (AOA Optimization)
    // ==========================================
    private int getAppPriorityFast(String packageName) {
        // Agar memory mein already hai, toh bina DB hit kiye 0ms mein return karo
        if (priorityCache.containsKey(packageName)) {
            return priorityCache.get(packageName);
        }

        // Agar nahi hai, toh ek baar DB se padho aur aage ke liye cache mein daal lo
        int priority = repository.getAppPriority(packageName);
        priorityCache.put(packageName, priority);

        return priority;
    }
}