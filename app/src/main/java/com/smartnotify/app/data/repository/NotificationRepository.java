package com.smartnotify.app.data.repository;

import android.content.Context;

import com.smartnotify.app.data.local.AppDatabase;
import com.smartnotify.app.data.local.dao.AppDao;
import com.smartnotify.app.data.local.entity.AppPriorityEntity;
import com.smartnotify.app.data.local.entity.NotificationEntity;
import com.smartnotify.app.data.prefs.UserPreferences;
import com.smartnotify.app.util.PriorityConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NotificationRepository {

    private final AppDao appDao;
    private final UserPreferences userPreferences;
    private final ExecutorService executorService;

    public NotificationRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        appDao = db.appDao();
        userPreferences = new UserPreferences(context);

        // Single thread executor banaya hai taaki DB operations background me smoothly hon
        executorService = Executors.newSingleThreadExecutor();
    }

    // ===================================
    // 1. PREFERENCES ACCESS
    // ===================================
    public UserPreferences getPreferences() {
        return userPreferences;
    }

    // ===================================
    // 2. APP PRIORITY LOGIC
    // ===================================

    // UI se jab drag & drop hoga, toh yeh method app ki priority save karega
    public void saveAppPriority(String packageName, int priority) {
        executorService.execute(() -> {
            appDao.insertAppPriority(new AppPriorityEntity(packageName, priority));
        });
    }

    // Service check karegi ki is app ki priority kya hai
    public int getAppPriority(String packageName) {
        Future<Integer> future = executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() {
                return appDao.getAppPriority(packageName);
            }
        });
        try {
            // Future.get() wait karega jab tak background thread se result nahi aa jata
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return PriorityConstants.UNASSIGNED; // Agar error aaya toh safe side 0 return karo
        }
    }

    // UI (Dialog) ko specific priority wali saari apps dena
    public List<String> getAppsByPriority(int priorityLevel) {
        Future<List<String>> future = executorService.submit(() -> appDao.getAppsByPriority(priorityLevel));
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    // ===================================
    // 3. BLOCKED NOTIFICATIONS LOGIC
    // ===================================

    // Jab notification intercept hoga, toh usko silently DB me save karna hai
    public void saveBlockedNotification(String packageName, String title, String text, long postTime, int priority) {
        executorService.execute(() -> {
            NotificationEntity entity = new NotificationEntity(packageName, title, text, postTime, priority);
            appDao.insertBlockedNotification(entity);
        });
    }

    // Scheduled time par DB se saare pending notifications uthana
    public List<NotificationEntity> getPendingNotifications(int priorityLevel) {
        Future<List<NotificationEntity>> future = executorService.submit(() -> appDao.getPendingNotifications(priorityLevel));
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Deliver hone ke baad list clear karna
    public void clearNotificationsByPriority(int priorityLevel) {
        executorService.execute(() -> appDao.clearNotificationsByPriority(priorityLevel));
    }

    // Jab Master Switch OFF ho, toh saara kachra clean karna
    public void clearAllNotifications() {
        executorService.execute(() -> appDao.clearAllNotifications());
    }
}