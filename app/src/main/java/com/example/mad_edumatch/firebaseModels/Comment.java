package com.example.mad_edumatch.firebaseModels;

public class Comment {
    private String commentId;
    private String answerId;
    private String userId;
    private String userName;
    private String content;
    private long timestamp;

    public Comment() {}

    public Comment(String commentId, String answerId, String userId, String userName, String content, long timestamp) {
        this.commentId = commentId;
        this.answerId = answerId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getAnswerId() { return answerId; }
    public void setAnswerId(String answerId) { this.answerId = answerId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}