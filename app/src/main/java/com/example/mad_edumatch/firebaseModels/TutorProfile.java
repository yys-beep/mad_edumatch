package com.example.mad_edumatch.firebaseModels;

import java.util.ArrayList;

public class TutorProfile {
    private String userId;
    private String username;
    private String email;
    private String contact;
    private ArrayList<String> subjects;
    private String fee;
    private String area;
    private String qualification;
    private ArrayList<String> achievement;
    private ArrayList<String> experience;
    private String description;
    private long registerTime;
    private String profileImageUrl; // Still stored here
    private boolean isVerified;

    // Default constructor required for Firebase
    public TutorProfile() {}

    public TutorProfile(String userId, String username, String email, String contact,
                        ArrayList<String> subjects, String fee, String area, String qualification,
                        ArrayList<String> achievement, ArrayList<String> experience, String description,
                        long registerTime, String profileImageUrl) { // Added profileImageUrl
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.contact = contact;
        this.subjects = subjects;
        this.fee = fee;
        this.area = area;
        this.qualification = qualification;
        this.achievement = achievement;
        this.experience = experience;
        this.description = description;
        this.registerTime = registerTime;
        this.profileImageUrl = profileImageUrl; // Initialized
        this.isVerified = false;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public ArrayList<String> getSubjects() { return subjects; }
    public void setSubjects(ArrayList<String> subjects) { this.subjects = subjects; }

    public String getFee() { return fee; }
    public void setFee(String fee) { this.fee = fee; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public ArrayList<String> getAchievement() { return achievement; }
    public void setAchievement(ArrayList<String> achievement) { this.achievement = achievement; }

    public ArrayList<String> getExperience() { return experience; }
    public void setExperience(ArrayList<String> experience) { this.experience = experience; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; } // Setter needed for updates

    public boolean isVerified() { return isVerified; }
    public long getRegisterTime() { return registerTime; }
}
