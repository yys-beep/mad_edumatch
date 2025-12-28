package com.example.mad_edumatch.helper;

import com.example.mad_edumatch.R;

public class LocalizationHelper {

    // 1. Map for Academic Levels (Primary, SPM, etc.)
    public static int getLevelStringId(String dbValue) {
        if (dbValue == null) return R.string.not_available; // Fallback

        switch (dbValue.trim()) {
            case "Primary":
                return R.string.primary;
            case "Lower Secondary (F1-F3)":
                return R.string.lower_secondary_f1_f3;
            case "SPM":
                return R.string.spm;
            case "STPM / A-Level":
                return R.string.stpm_a_level;
            case "IGCSE":
                return R.string.igcse;
            case "University":
                return R.string.university;
            default:
                return 0; // Return 0 if custom value
        }
    }

    // 2. Map for Delivery Mode (Online / Physical)
    public static int getDeliveryModeStringId(String dbValue) {
        if (dbValue == null) return R.string.not_available;

        switch (dbValue.trim()) {
            case "Online":
                return R.string.online;
            case "Physical":
                return R.string.physical;
            default:
                return 0;
        }
    }

    // 3. Map for Learning Mode (One-to-One / Group)
    public static int getLearningModeStringId(String dbValue) {
        if (dbValue == null) return R.string.not_available;

        switch (dbValue.trim()) {
            case "One-to-One":
                return R.string.one_to_one;
            case "Group":
                return R.string.group;
            default:
                return 0;
        }
    }
}