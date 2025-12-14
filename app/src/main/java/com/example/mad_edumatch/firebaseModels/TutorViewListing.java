package com.example.mad_edumatch.firebaseModels;

import android.text.TextUtils; // Added import for TextUtils
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class TutorViewListing {

    private String key;
    private String tutorId;
    private String name;
    private String subject;
    private double fee; // Stored as double
    private String area;
    private String contact;
    private String learningMode;
    private String deliveryMode;
    private String qualification;
    private String achievement;
    private long timestamp;

    // NEW: List of Academic Levels
    private List<String> academicLevels;

    public TutorViewListing() {
        // Default constructor required for calls to DataSnapshot.getValue(TutorViewListing.class)
    }

    // Updated Constructor
    public TutorViewListing(String tutorId, String name, String subject, double fee, String area,
                            String contact, String learningMode, String deliveryMode,
                            String qualification, String achievement, long timestamp,
                            List<String> academicLevels) {
        this.tutorId = tutorId;
        this.name = name;
        this.subject = subject;
        this.fee = fee;
        this.area = area;
        this.contact = contact;
        this.learningMode = learningMode;
        this.deliveryMode = deliveryMode;
        this.qualification = qualification;
        this.achievement = achievement;
        this.timestamp = timestamp;
        this.academicLevels = academicLevels;
    }

    // --- Getters ---

    public String getKey() { return key; }
    public String getTutorId() { return tutorId; }
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public double getFee() { return fee; } // Returns double
    public String getFeeString() { return String.format(Locale.getDefault(), "RM %.2f/hr", fee); } // Helper for display
    public String getArea() { return area; }
    public String getContact() { return contact; }
    public String getLearningMode() { return learningMode; }
    public String getDeliveryMode() { return deliveryMode; }
    public String getQualification() { return qualification; }
    public String getAchievement() { return achievement; }
    public long getTimestamp() { return timestamp; }

    public List<String> getAcademicLevels() {
        return academicLevels != null ? academicLevels : new ArrayList<>();
    }

    // Helper to display levels as a comma-separated string
    public String getLevelsAsString() {
        if (academicLevels == null || academicLevels.isEmpty()) return "N/A";
        return TextUtils.join(", ", academicLevels);
    }

    // --- Setters ---

    public void setKey(String key) { this.key = key; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }
    public void setName(String name) { this.name = name; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setFee(double fee) { this.fee = fee; } // Takes double
    public void setArea(String area) { this.area = area; }
    public void setContact(String contact) { this.contact = contact; }
    public void setLearningMode(String learningMode) { this.learningMode = learningMode; }
    public void setDeliveryMode(String deliveryMode) { this.deliveryMode = deliveryMode; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public void setAchievement(String achievement) { this.achievement = achievement; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public void setAcademicLevels(List<String> academicLevels) {
        this.academicLevels = academicLevels;
    }
}