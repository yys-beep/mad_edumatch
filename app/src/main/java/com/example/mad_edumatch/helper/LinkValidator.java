package com.example.mad_edumatch.helper;

import android.text.TextUtils;
import android.util.Patterns;

public class LinkValidator {

    /**
     * Checks if a string is a valid URL.
     * Rules:
     * 1. Not null or empty.
     * 2. Starts with http:// or https://
     * 3. Matches Android's standard WEB_URL pattern.
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // 1. Basic Protocol Check
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        // 2. Strict Pattern Check (Handles weird chars, spaces, etc.)
        return Patterns.WEB_URL.matcher(url).matches();
    }
}