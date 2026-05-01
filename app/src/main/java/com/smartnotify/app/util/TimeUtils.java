package com.smartnotify.app.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    /**
     * Yeh function check karega ki current time, user ke start aur end time ke beech me hai ya nahi.
     * @param startTime String format me "HH:mm" (e.g., "09:00")
     * @param endTime String format me "HH:mm" (e.g., "13:00")
     * @return true agar current time range ke andar hai, warna false
     */
    public static boolean isTimeBetween(String startTime, String endTime) {
        try {
            // Current time ko minutes me convert karo (e.g., 2:30 PM = 14*60 + 30 = 870 mins)
            Calendar calendar = Calendar.getInstance();
            int currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

            // Start time ko parse karo
            String[] startParts = startTime.split(":");
            int startMinutes = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);

            // End time ko parse karo
            String[] endParts = endTime.split(":");
            int endMinutes = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);

            // Logic: Agar range same day ki hai (e.g., 09:00 to 17:00)
            if (startMinutes <= endMinutes) {
                return currentMinutes >= startMinutes && currentMinutes <= endMinutes;
            }
            // Logic: Agar range overnight hai (e.g., 22:00 to 06:00 next day)
            else {
                return currentMinutes >= startMinutes || currentMinutes <= endMinutes;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false; // Agar koi error aaye toh false return kardo (safe side)
        }
    }

    /**
     * UI me dikhane ke liye 24-hour time ko 12-hour AM/PM me convert karega
     * e.g., "13:00" -> "01:00 PM"
     */
    public static String formatTo12Hour(String time24) {
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf24.parse(time24);
            if (date != null) {
                return sdf12.format(date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time24; // Agar parse na ho paye toh waisa hi return kardo
    }
}