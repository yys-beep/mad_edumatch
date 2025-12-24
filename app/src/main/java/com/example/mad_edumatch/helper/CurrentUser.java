package com.example.mad_edumatch.helper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CurrentUser {
    private static CurrentUser instance;
    // Removed the "private FirebaseUser firebaseUser;" field
    private String uid;
    private String name;
    private CurrentUser() { }

    public static CurrentUser getInstance() {
        if (instance == null) {
            instance = new CurrentUser();
        }
        return instance;
    }

    // Always fetch the latest status directly from Firebase
    public String getUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public boolean isLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    public void refresh() {
        // No longer needed if you use the direct fetch method above,
        // but kept to avoid breaking existing code.
    }
}