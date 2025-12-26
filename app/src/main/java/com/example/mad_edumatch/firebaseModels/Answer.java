package com.example.mad_edumatch.firebaseModels;

import java.util.HashMap;
import java.util.Map;
public class Answer {
    private String answerId;
    private String questionId;
    private String userId;
    private String userName; // Changed from 'username' to match your DB
    private String content;
    private String solutionLink; // Added this field
    private long timestamp;
    private String attachmentUrl;
    private String attachmentName;
    private Map<String, Boolean> likes = new HashMap<>();

    public Answer() { } // Empty constructor for Firebase

    public Map<String, Boolean> getLikes() { return likes; }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }

    public Answer(String answerId, String questionId, String userId, String userName, String content, String solutionLink, long timestamp, String attachmentUrl, String attachmentName) {
        this.answerId = answerId;
        this.questionId = questionId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.solutionLink = solutionLink; // Initialize
        this.timestamp = timestamp;
        this.attachmentUrl = attachmentUrl;
        this.attachmentName = attachmentName;
    }

    public String getAnswerId() { return answerId; }
    public String getQuestionId() { return questionId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; } // Getter matches field
    public String getContent() { return content; }
    public String getSolutionLink() { return solutionLink; } // New Getter
    public long getTimestamp() { return timestamp; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public String getAttachmentName() { return attachmentName; }
}