package com.example.mad_edumatch.helper;

import android.text.format.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class TimeHelper {
    public static String getMalaysiaTime(long timestamp) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
        cal.setTimeInMillis(timestamp);
        return DateFormat.format("dd MMM yyyy, hh:mm aa", cal).toString();
    }
}