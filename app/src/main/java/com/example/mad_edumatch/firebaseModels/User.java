package com.example.mad_edumatch.firebaseModels;

public class User {
    private String uid;
    private String name;
    private String email;
    private String role; // "Student" or "Tutor"
    private long registerTime; // Stored as timestamp
    private String profileImageUrl; // Now added to the constructor/primary data

    public User() {} // Required for Firebase

    public User(String uid, String name, String email, String role, long registerTime, String profileImageUrl) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.registerTime = registerTime;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public long getRegisterTime() { return registerTime; }
    public String getProfileImageUrl() { return profileImageUrl; }

    // Setters (Useful for updates)
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}