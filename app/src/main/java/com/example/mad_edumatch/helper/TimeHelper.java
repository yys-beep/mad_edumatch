package com.example.mad_edumatch.helper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeHelper {

    // Helper for Chat Detail (e.g., "15 Dec 2025, 02:30 PM")
    public static String getMalaysiaTime(long timestamp) {
        try {
            // 1. Create a Date object
            Date date = new Date(timestamp);

            // 2. Define the format
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm aa", Locale.getDefault());

            // 3. FORCE TimeZone to Malaysia
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));

            return sdf.format(date);
        } catch (Exception e) {
            return "";
        }
    }
}