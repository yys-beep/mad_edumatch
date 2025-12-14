package com.example.mad_edumatch.helper;

import com.example.mad_edumatch.R;

public class AvatarManager {

    public static final int[] AVATAR_DRAWABLES = new int[]{
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6,
            R.drawable.avatar_7,
            R.drawable.avatar_8,
            R.drawable.avatar_9,
            R.drawable.avatar_10
    };

    public static int getAvatarResourceId(String avatarName) {
        // FAIL-SAFE: If null or empty, return avatar_1 immediately
        if (avatarName == null || avatarName.isEmpty()) {
            return R.drawable.avatar_1;
        }

        try {
            // Expected format: "avatar_1", "avatar_5", etc.
            String[] parts = avatarName.split("_");
            if (parts.length > 1) {
                int index = Integer.parseInt(parts[1]) - 1; // Convert to 0-based index
                if (index >= 0 && index < AVATAR_DRAWABLES.length) {
                    return AVATAR_DRAWABLES[index];
                }
            }
        } catch (Exception ignored) {
            // If parsing fails (e.g. filename is wrong), ignore and return default
        }

        // Final fallback
        return R.drawable.avatar_1;
    }

    public static String getAvatarName(int index) {
        if (index >= 0 && index < AVATAR_DRAWABLES.length) {
            return "avatar_" + (index + 1);
        }
        return "avatar_1";
    }
}