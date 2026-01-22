package com.example.mad_edumatch.helper;

import android.content.Context;
import com.example.mad_edumatch.R;
import java.util.ArrayList;
import java.util.List;

public class ListingDataHelper {

    // --- KEYS (What gets saved to Database) ---
    public static final String KEY_ONE_TO_ONE = "ONE_TO_ONE";
    public static final String KEY_GROUP = "GROUP";
    public static final String KEY_PHYSICAL = "PHYSICAL";
    public static final String KEY_ONLINE = "ONLINE";

    // Order matches your XML ChipGroup order: Primary, Lower Sec, SPM, STPM, IGCSE, Uni
    public static final String[] LEVEL_KEYS = {"PRIMARY", "LOWER_SEC", "SPM", "STPM", "IGCSE", "UNI"};

    // --- CONVERTERS (Key -> Display Text) ---

    public static String getLearningModeDisplayName(Context context, String key) {
        if (key == null) return "N/A";
        switch (key) {
            case KEY_ONE_TO_ONE: return context.getString(R.string.val_one_to_one);
            case KEY_GROUP: return context.getString(R.string.val_group);
            default: return key; // Fallback for old data (e.g., "Satu-ke-satu")
        }
    }

    public static String getDeliveryModeDisplayName(Context context, String key) {
        if (key == null) return "N/A";
        switch (key) {
            case KEY_PHYSICAL: return context.getString(R.string.val_physical);
            case KEY_ONLINE: return context.getString(R.string.val_online);
            default: return key;
        }
    }

    public static String getLevelDisplayName(Context context, String key) {
        if (key == null) return "";
        switch (key) {
            case "PRIMARY": return context.getString(R.string.primary);
            case "LOWER_SEC": return context.getString(R.string.lower_secondary_f1_f3);
            case "SPM": return context.getString(R.string.spm);
            case "STPM": return context.getString(R.string.stpm_a_level);
            case "IGCSE": return context.getString(R.string.igcse);
            case "UNI": return context.getString(R.string.university);
            default: return key;
        }
    }

    // Helper for Lists (e.g. converting a list of keys to a string like "SPM, IGCSE")
    public static String getLevelsAsString(Context context, List<String> keys) {
        if (keys == null || keys.isEmpty()) return "N/A";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            sb.append(getLevelDisplayName(context, keys.get(i)));
            if (i < keys.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }
}