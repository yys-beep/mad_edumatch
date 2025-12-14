package com.example.mad_edumatch.firebaseModels;

public class Question {
    private String questionId;
    private String userId;
    private String userName;
    private String title;
    private String content;
    private long timestamp;
    private boolean solved;

    // NEW FIELDS for file upload
    private String fileUrl;
    private String fileName;

    // Empty constructor for Firebase
    public Question() { }

    public Question(String questionId, String userId, String userName, String title, String content, long timestamp, boolean solved, String fileUrl, String fileName) {
        this.questionId = questionId;
        this.userId = userId;
        this.userName = userName;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.solved = solved;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
    }

    public String getQuestionId() { return questionId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public boolean isSolved() { return solved; }

    // New Getters
    public String getFileUrl() { return fileUrl; }
    public String getFileName() { return fileName; }
}