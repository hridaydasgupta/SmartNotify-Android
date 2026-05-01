package com.smartnotify.app.util;

import android.content.Context;
import androidx.core.app.NotificationManagerCompat;

public class NotificationPermissionUtil {

    // Check karta hai ki humari app ko notification read karne ka access hai ya nahi
    public static boolean hasAccess(Context context) {
        // Yeh official AndroidX method hai! Koi string matching ka dhoka nahi.
        // Yeh exact enabled apps ki list nikalta hai aur check karta hai.
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.getPackageName());
    }
}