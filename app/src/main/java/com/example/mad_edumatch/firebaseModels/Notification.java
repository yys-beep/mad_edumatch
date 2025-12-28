package com.example.mad_edumatch.firebaseModels;

import com.google.firebase.database.PropertyName;

import java.io.Serializable;

public class Notification implements Serializable {
    private String id;           // The Firebase push key
    private String title;        // "New Kudos!" or "New Solution!"
    private String message;      // The display text
    private String action_type;  // "OPEN_LESSON" or "OPEN_QUESTION"
    private String sourceId;     // The Lesson ID or Question ID
    private String senderId;
    private long timestamp;      // For sorting by newest first
    private boolean isRead;      // For the unread badge

    public Notification() {
        // Required empty constructor for Firebase
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAction_type() { return action_type; }
    public void setAction_type(String action_type) { this.action_type = action_type; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @PropertyName("isRead")  // Forces Firebase to read "isRead" key
    public boolean isRead() { return isRead; }

    @PropertyName("isRead")  // Forces Firebase to write "isRead" key
    public void setRead(boolean read) { isRead = read; }
}