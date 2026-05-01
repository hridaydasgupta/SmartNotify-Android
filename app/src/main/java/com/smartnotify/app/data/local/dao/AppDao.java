package com.smartnotify.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.smartnotify.app.data.local.entity.AppPriorityEntity;
import com.smartnotify.app.data.local.entity.NotificationEntity;

import java.util.List;

@Dao
public interface AppDao {

    // ===================================
    // APP PRIORITY QUERIES
    // ===================================

    // App priority save ya update karne ke liye
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAppPriority(AppPriorityEntity entity);

    // Kisi specific app ki priority nikalne ke liye (0 aayega agar unassigned hai)
    @Query("SELECT priority FROM app_priorities WHERE packageName = :pkgName LIMIT 1")
    int getAppPriority(String pkgName);

    // 👇 NAYA CODE YAHAN ADD KIYA HAI 👇
    // Kisi specific priority wali saari apps ke package names nikalne ke liye
    @Query("SELECT packageName FROM app_priorities WHERE priority = :priorityLevel")
    List<String> getAppsByPriority(int priorityLevel);
    // 👆 NAYA CODE YAHAN TAK HAI 👆

    // ===================================
    // BLOCKED NOTIFICATIONS QUERIES
    // ===================================

    // Notification chup-chaap save karne ke liye
    @Insert
    void insertBlockedNotification(NotificationEntity notification);

    // Saare pending notifications nikalne ke liye (e.g., jab Focus Time khatam ho)
    @Query("SELECT * FROM pending_notifications WHERE priority = :priorityLevel ORDER BY postTime ASC")
    List<NotificationEntity> getPendingNotifications(int priorityLevel);

    // Notification deliver hone ke baad usko DB se delete karne ke liye
    @Query("DELETE FROM pending_notifications WHERE priority = :priorityLevel")
    void clearNotificationsByPriority(int priorityLevel);

    // Master Switch OFF hone par saare notifications clear karne ke liye
    @Query("DELETE FROM pending_notifications")
    void clearAllNotifications();
}