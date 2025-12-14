package com.example.mad_edumatch.firebaseModels;

public class LessonComment {
    private String commentId;
    private String lessonId;
    private String userId;
    private String userName;
    private String content;
    private long timestamp;

    // Attachment Fields
    private String attachmentUrl; // Stores Appwrite File ID
    private String attachmentName;

    public LessonComment() { }

    public LessonComment(String commentId, String lessonId, String userId, String userName, String content, long timestamp, String attachmentName, String attachmentUrl) {
        this.commentId = commentId;
        this.lessonId = lessonId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.timestamp = timestamp;
        this.attachmentName = attachmentName;
        this.attachmentUrl = attachmentUrl;
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getLessonId() { return lessonId; }
    public void setLessonId(String lessonId) { this.lessonId = lessonId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
}