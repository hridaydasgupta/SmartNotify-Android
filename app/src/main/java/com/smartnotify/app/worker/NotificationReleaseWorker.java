package com.smartnotify.app.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.smartnotify.app.data.local.entity.NotificationEntity;
import com.smartnotify.app.data.repository.NotificationRepository;
import com.smartnotify.app.util.NotificationHelper;

import java.util.List;

public class NotificationReleaseWorker extends Worker {

    public static final String KEY_PRIORITY_LEVEL = "priority_level";

    public NotificationReleaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Pata karo ki kya release karna hai: Medium (Focus Time over) ya Low (Window start)?
        int priorityLevel = getInputData().getInt(KEY_PRIORITY_LEVEL, -1);
        if (priorityLevel == -1) {
            return Result.failure();
        }

        Log.d("SmartNotify", "Worker Jag Gaya! Releasing notifications for priority: " + priorityLevel);

        try {
            NotificationRepository repository = new NotificationRepository(getApplicationContext());

            // 1. Database se chhupaye hue ruki hui notifications uthao
            List<NotificationEntity> pendingNotifications = repository.getPendingNotifications(priorityLevel);

            // 2. Agar list khali nahi hai, toh Zero-Lag Delivery Engine start karo
            if (pendingNotifications != null && !pendingNotifications.isEmpty()) {
                NotificationHelper.deliverNotificationsZeroLag(getApplicationContext(), pendingNotifications, repository, priorityLevel);
            }

            return Result.success();

        } catch (Exception e) {
            Log.e("SmartNotify", "Error in NotificationReleaseWorker", e);
            return Result.retry(); // Agar kuch fat gaya, toh OS isko thodi der baad wapas try karega
        }
    }
}