package com.smartnotify.app.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.smartnotify.app.R;
import com.smartnotify.app.data.local.entity.NotificationEntity;
import com.smartnotify.app.data.repository.NotificationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationHelper {

    private static final String CHANNEL_ID = "smart_notify_release_channel";
    private static final String CHANNEL_NAME = "Scheduled Notifications";

    public static void deliverNotificationsZeroLag(Context context, List<NotificationEntity> pendingNotifications, NotificationRepository repository, int priorityLevel) {
        if (pendingNotifications == null || pendingNotifications.isEmpty()) return;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Notification Channel banayein (Oreo+ ke liye zaroori hai)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        PackageManager pm = context.getPackageManager();
        Handler handler = new Handler(Looper.getMainLooper());

        // 🚀 STEP 1: APP-WISE GROUPING LOGIC (Bundling) 🚀
        Map<String, List<NotificationEntity>> groupedNotifications = new HashMap<>();

        for (NotificationEntity entity : pendingNotifications) {
            if (!groupedNotifications.containsKey(entity.packageName)) {
                groupedNotifications.put(entity.packageName, new ArrayList<>());
            }
            groupedNotifications.get(entity.packageName).add(entity);
        }

        // 🚀 STEP 2: STAGGERED RELEASE LOGIC (Zero Lag) 🚀
        int delayMillis = 0;
        int delayIncrement = 300; // Har App ke notification ke beech 300ms ka gap

        for (Map.Entry<String, List<NotificationEntity>> entry : groupedNotifications.entrySet()) {
            String packageName = entry.getKey();
            List<NotificationEntity> appNotifs = entry.getValue();

            handler.postDelayed(() -> {

                String appName = "Unknown App";
                try {
                    appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                // 🔥 CLICK KARNE PAR ORIGINAL APP KHOLNE KA LOGIC 🔥
                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                PendingIntent pendingIntent = null;
                if (launchIntent != null) {
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    // Android 12+ ke liye FLAG_IMMUTABLE zaroori hai
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        flags |= PendingIntent.FLAG_IMMUTABLE;
                    }
                    pendingIntent = PendingIntent.getActivity(context, packageName.hashCode(), launchIntent, flags);
                }

                // Notification Builder setup
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_smart) // 🔥 Apna official icon laga diya 🔥
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

                // Agar Launch Intent mil gaya (jaise WhatsApp), toh set kar do
                if (pendingIntent != null) {
                    builder.setContentIntent(pendingIntent);
                }

                // Agar app ka sirf 1 notification hai
                if (appNotifs.size() == 1) {
                    NotificationEntity singleNotif = appNotifs.get(0);
                    builder.setContentTitle(appName + ": " + singleNotif.title)
                            .setContentText(singleNotif.text);
                }
                // Agar ek hi app ke multiple notifications hain
                else {
                    NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                    inboxStyle.setBigContentTitle(appNotifs.size() + " new notifications from " + appName);

                    int linesToShow = Math.min(appNotifs.size(), 5);
                    for (int i = 0; i < linesToShow; i++) {
                        inboxStyle.addLine(appNotifs.get(i).title + " - " + appNotifs.get(i).text);
                    }

                    if (appNotifs.size() > 5) {
                        inboxStyle.setSummaryText("+" + (appNotifs.size() - 5) + " more messages");
                    }

                    builder.setContentTitle(appName)
                            .setContentText(appNotifs.size() + " new notifications")
                            .setStyle(inboxStyle);
                }

                // Notification push karna
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        notificationManager.notify(packageName.hashCode(), builder.build());
                    }
                } else {
                    notificationManager.notify(packageName.hashCode(), builder.build());
                }

            }, delayMillis);

            delayMillis += delayIncrement;
        }

        // Jab loops schedule ho jayein, DB se saara kachra saaf kar do
        repository.clearNotificationsByPriority(priorityLevel);
    }
}