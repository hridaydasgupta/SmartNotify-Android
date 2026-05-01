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

    public void saveAppPriority(String packageName, int priority) {
        executorService.execute(() -> {
            appDao.insertAppPriority(new AppPriorityEntity(packageName, priority));
        });
    }

    public int getAppPriority(String packageName) {
        Future<Integer> future = executorService.submit(() -> appDao.getAppPriority(packageName));
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return PriorityConstants.UNASSIGNED;
        }
    }

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

    public void saveBlockedNotification(String packageName, String title, String text, long postTime, int priority) {
        executorService.execute(() -> {
            NotificationEntity entity = new NotificationEntity(packageName, title, text, postTime, priority);
            appDao.insertBlockedNotification(entity);
        });
    }

    public List<NotificationEntity> getPendingNotifications(int priorityLevel) {
        Future<List<NotificationEntity>> future = executorService.submit(() -> appDao.getPendingNotifications(priorityLevel));
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void clearNotificationsByPriority(int priorityLevel) {
        executorService.execute(() -> appDao.clearNotificationsByPriority(priorityLevel));
    }

    public void clearAllNotifications() {
        executorService.execute(() -> appDao.clearAllNotifications());
    }

    // ===================================
    // 4. SYNC METHODS FOR CACHE & VIEWMODEL
    // ===================================

    // ViewModel ke liye: Unassigned apps filter karne ke kaam aayega
    public List<String> getAssignedPackageNamesSync() {
        Future<List<String>> future = executorService.submit(() -> appDao.getAllAssignedPackages());
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Service ke O(1) Cache ke liye: Ek baar me saari priority laane ke kaam aayega
    public List<AppPriorityEntity> getAllPrioritiesSync() {
        Future<List<AppPriorityEntity>> future = executorService.submit(() -> appDao.getAllPriorities());
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}