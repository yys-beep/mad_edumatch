package com.example.mad_edumatch.firebaseModels;

public class StudentRequest {
    private String requestId;
    private String studentId;
    private String subject;
    private String level;
    private String area;
    private String learningMode;  // "One-to-One" or "One-to-Many"
    private String deliveryMode;  // "Physical" or "Online"
    private double budget;
    private String description;

    // --- NEW FIELD FOR SORTING ---
    private long timestamp;

    // Empty constructor required for Firebase
    public StudentRequest() {
    }

    // Updated Constructor to include 'timestamp'
    public StudentRequest(String requestId, String studentId, String subject, String level, String area, String learningMode, String deliveryMode, double budget, String description, long timestamp) {
        this.requestId = requestId;
        this.studentId = studentId;
        this.subject = subject;
        this.level = level;
        this.area = area;
        this.learningMode = learningMode;
        this.deliveryMode = deliveryMode;
        this.budget = budget;
        this.description = description;
        this.timestamp = timestamp; // Initialize here
    }

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getLearningMode() { return learningMode; }
    public void setLearningMode(String learningMode) { this.learningMode = learningMode; }

    public String getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(String deliveryMode) { this.deliveryMode = deliveryMode; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // --- NEW GETTER & SETTER ---
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}