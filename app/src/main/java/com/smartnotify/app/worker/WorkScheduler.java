package com.smartnotify.app.worker;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.smartnotify.app.util.PriorityConstants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WorkScheduler {

    private static final String WORK_NAME_MEDIUM = "release_medium_priority";
    private static final String WORK_NAME_LOW = "release_low_priority";

    /**
     * Medium Priority (Focus Time) ke khatam hone par notifications release karne ka schedule lagayega
     */
    public static void scheduleMediumPriorityRelease(Context context, String focusEndTime) {
        long delayMillis = calculateDelay(focusEndTime);

        Log.d("SmartNotify", "Medium Priority release scheduled in: " + (delayMillis / 1000 / 60) + " minutes.");

        Data inputData = new Data.Builder()
                .putInt(NotificationReleaseWorker.KEY_PRIORITY_LEVEL, PriorityConstants.MEDIUM)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationReleaseWorker.class)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

        // REPLACE policy use kar rahe hain taaki agar user time change kare, toh purana alarm hat jaye
        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_MEDIUM,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }

    /**
     * Low Priority Window ke start hone par notifications release karne ka schedule lagayega
     */
    public static void scheduleLowPriorityRelease(Context context, String lowPriorityTime) {
        long delayMillis = calculateDelay(lowPriorityTime);

        Log.d("SmartNotify", "Low Priority release scheduled in: " + (delayMillis / 1000 / 60) + " minutes.");

        Data inputData = new Data.Builder()
                .putInt(NotificationReleaseWorker.KEY_PRIORITY_LEVEL, PriorityConstants.LOW)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationReleaseWorker.class)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_LOW,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }

    /**
     * Helper Method: Current time se lekar target time tak ka difference (milliseconds me) calculate karega
     */
    private static long calculateDelay(String targetTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

            // Current Time nikalo
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);

            // Target Time parse karo
            Date date = sdf.parse(targetTime);
            Calendar target = Calendar.getInstance();
            if (date != null) {
                target.setTime(date);
            }
            int targetHour = target.get(Calendar.HOUR_OF_DAY);
            int targetMinute = target.get(Calendar.MINUTE);

            // Dono ko minutes me convert karo
            int currentTotalMinutes = (currentHour * 60) + currentMinute;
            int targetTotalMinutes = (targetHour * 60) + targetMinute;

            int diffMinutes;

            // Logic: Agar target time aaj nikal chuka hai, toh matlab user kal ke liye alarm laga raha hai
            if (targetTotalMinutes <= currentTotalMinutes) {
                // Kal ka time (24 ghante = 1440 minutes add karo)
                diffMinutes = (targetTotalMinutes + 1440) - currentTotalMinutes;
            } else {
                // Aaj ka time
                diffMinutes = targetTotalMinutes - currentTotalMinutes;
            }

            // Minutes ko milliseconds me convert karke return karo
            return (long) diffMinutes * 60 * 1000;

        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Agar error aaye toh safe side 0 return karo
        }
    }
}