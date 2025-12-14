package com.example.mad_edumatch.firebaseModels;

import java.util.HashMap;
import java.util.Map;

public class FreeLesson {
    private String lessonId;
    private String tutorId;
    private String tutorName;
    private String title;
    private String description;

    // CHANGED: Must match "videoLink" in Firebase
    private String videoLink;

    private String materialUrl;
    private String materialName;
    private long timestamp;
    private int likeCount;

    // NEW: Handle the "likes" map from your JSON
    private Map<String, Boolean> likes = new HashMap<>();

    // NEW: Handle missing duration gracefully
    private long durationMinutes = 0;

    public FreeLesson() { }

    public FreeLesson(String lessonId, String tutorId, String tutorName, String title, String description, String videoLink, String materialUrl, String materialName, long timestamp, int likeCount, long durationMinutes) {
        this.lessonId = lessonId;
        this.tutorId = tutorId;
        this.tutorName = tutorName;
        this.title = title;
        this.description = description;
        this.videoLink = videoLink;
        this.materialUrl = materialUrl;
        this.materialName = materialName;
        this.timestamp = timestamp;
        this.likeCount = likeCount;
        this.durationMinutes = durationMinutes;
    }

    public String getLessonId() { return lessonId; }
    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }
    public String getTutorId() { return tutorId; }
    public String getTutorName() { return tutorName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }

    // Getter matching the new field name
    public String getVideoLink() { return videoLink; }

    public String getMaterialUrl() { return materialUrl; }
    public String getMaterialName() { return materialName; }
    public long getTimestamp() { return timestamp; }
    public int getLikeCount() { return likeCount; }
    public Map<String, Boolean> getLikes() { return likes; }
    public long getDurationMinutes() { return durationMinutes; }

    public void setTutorId(String tutorId) { this.tutorId = tutorId; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setVideoLink(String videoLink) { this.videoLink = videoLink; }
    public void setMaterialUrl(String materialUrl) { this.materialUrl = materialUrl; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }
    public void setDurationMinutes(long durationMinutes) { this.durationMinutes = durationMinutes; }
}