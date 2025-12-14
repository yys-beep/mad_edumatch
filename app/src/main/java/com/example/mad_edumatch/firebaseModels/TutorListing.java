package com.example.mad_edumatch.firebaseModels;

import android.text.TextUtils;

import java.util.List;
import java.util.ArrayList;

public class TutorListing {

    public String tutorId;
    public String name;
    public String subject;
    public String area;
    public String contact;
    public double fee;
    public String learningMode;   // One-to-One or One-to-Many
    public String deliveryMode;   // Physical or Online
    public String qualification;
    public String achievement;
    public long timestamp;        // For sorting by newest

    // REQUIRED for the Filter logic (Standardized name to match TutorViewListing)
    public List<String> academicLevels;

    public TutorListing() {
        // Default constructor required for Firebase
    }

    public TutorListing(String tutorId, String name, String subject, String area, String contact,
                        double fee, String learningMode, String deliveryMode,
                        String qualification, String achievement, long timestamp, List<String> academicLevels) {
        this.tutorId = tutorId;
        this.name = name;
        this.subject = subject;
        this.area = area;
        this.contact = contact;
        this.fee = fee;
        this.learningMode = learningMode;
        this.deliveryMode = deliveryMode;
        this.qualification = qualification;
        this.achievement = achievement;
        this.timestamp = timestamp;
        this.academicLevels = academicLevels;
    }

    // --- Getters ---

    public String getTutorId() { return tutorId; }
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public String getArea() { return area; }
    public String getContact() { return contact; }
    public double getFee() { return fee; }
    public String getLearningMode() { return learningMode; }
    public String getDeliveryMode() { return deliveryMode; }
    public String getQualification() { return qualification; }
    public String getAchievement() { return achievement; }
    public long getTimestamp() { return timestamp; }
    public List<String> getAcademicLevels() { return academicLevels != null ? academicLevels : new ArrayList<>(); }

    // --- Setters (Essential for updates) ---

    public void setTutorId(String tutorId) { this.tutorId = tutorId; }
    public void setName(String name) { this.name = name; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setArea(String area) { this.area = area; }
    public void setContact(String contact) { this.contact = contact; }
    public void setFee(double fee) { this.fee = fee; }
    public void setLearningMode(String learningMode) { this.learningMode = learningMode; }
    public void setDeliveryMode(String deliveryMode) { this.deliveryMode = deliveryMode; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public void setAchievement(String achievement) { this.achievement = achievement; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setAcademicLevels(List<String> academicLevels) { this.academicLevels = academicLevels; }

    // Helper to display levels as a string (e.g., "Primary, SPM") - For convenience
    public String getLevelsAsString() {
        if (academicLevels == null || academicLevels.isEmpty()) return "All Levels";
        return TextUtils.join(", ", academicLevels); // Assuming you use TextUtils.join in Android
    }
}