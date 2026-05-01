package com.smartnotify.app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_notifications")
public class NotificationEntity {

    @PrimaryKey(autoGenerate = true)
    public int id; // Unique ID har notification ke liye

    public String packageName;
    public String title;
    public String text;
    public long postTime; // Kab aaya tha
    public int priority;  // Is it Medium or Low priority?

    public NotificationEntity(String packageName, String title, String text, long postTime, int priority) {
        this.packageName = packageName;
        this.title = title;
        this.text = text;
        this.postTime = postTime;
        this.priority = priority;
    }
}