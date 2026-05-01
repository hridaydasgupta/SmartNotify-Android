package com.smartnotify.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_priorities")
public class AppPriorityEntity {

    @PrimaryKey
    @NonNull
    public String packageName;

    public int priority;

    public AppPriorityEntity(@NonNull String packageName, int priority) {
        this.packageName = packageName;
        this.priority = priority;
    }
}