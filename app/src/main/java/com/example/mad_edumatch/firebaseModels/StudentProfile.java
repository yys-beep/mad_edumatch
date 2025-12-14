package com.example.mad_edumatch.firebaseModels;

import java.util.ArrayList;

public class StudentProfile {
    private String userId;
    private String username;
    private String email;
    private String contact;
    private int age;
    private String academicLevel;
    private String description;
    private long registerTime;
    private String profileImageUrl; // Stores the avatar string (e.g., "avatar_1")
    private ArrayList<String> achievements;
    private ArrayList<String> participatedFreeLessons;

    // Default constructor required for Firebase
    public StudentProfile() {}

    public StudentProfile(String userId, String username, String email, String contact,
                          int age, String academicLevel, String description,
                          long registerTime, String profileImageUrl, // Added here
                          ArrayList<String> achievements, ArrayList<String> participatedFreeLessons) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.contact = contact;
        this.age = age;
        this.academicLevel = academicLevel;
        this.description = description;
        this.registerTime = registerTime;
        this.profileImageUrl = profileImageUrl; // Set correctly from argument
        this.achievements = achievements;
        this.participatedFreeLessons = participatedFreeLessons;
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

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getAcademicLevel() { return academicLevel; }
    public void setAcademicLevel(String academicLevel) { this.academicLevel = academicLevel; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getRegisterTime() { return registerTime; }
    public void setRegisterTime(long registerTime) { this.registerTime = registerTime; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public ArrayList<String> getAchievements() { return achievements; }
    public void setAchievements(ArrayList<String> achievements) { this.achievements = achievements; }

    public ArrayList<String> getParticipatedFreeLessons() { return participatedFreeLessons; }
    public void setParticipatedFreeLessons(ArrayList<String> participatedFreeLessons) { this.participatedFreeLessons = participatedFreeLessons; }
}