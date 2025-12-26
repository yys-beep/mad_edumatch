package com.example.mad_edumatch.helper;

import androidx.annotation.NonNull;
import com.example.mad_edumatch.firebaseModels.FreeLesson;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GamificationHelper {

    private static final String DB_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    public static void calculateScore(String tutorId) {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference();

        // 1. Fetch Lessons
        ref.child("free_lessons").orderByChild("tutorId").equalTo(tutorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int uploadCount = 0;
                int likeCount = 0;
                List<String> myLessonIds = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    FreeLesson lesson = ds.getValue(FreeLesson.class);

                    // =================================================================
                    // 🛡️ STRICT FILTER: FORCE CHECK TUTOR ID
                    // This ignores the 4 lessons belonging to other tutors
                    // =================================================================
                    if (lesson != null && lesson.getTutorId() != null && lesson.getTutorId().equals(tutorId)) {

                        uploadCount++; // NOW THIS WILL ONLY BE 3

                        if (lesson.getLikes() != null) {
                            likeCount += lesson.getLikes().size();
                        }
                        if (lesson.getLessonId() != null) {
                            myLessonIds.add(lesson.getLessonId());
                        }
                    }
                }

                // 2. Fetch Answers (Using corrected "forum_answers")
                fetchAnswersAndCompletions(tutorId, ref, uploadCount, likeCount, myLessonIds);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static void fetchAnswersAndCompletions(String tutorId, DatabaseReference ref, int uploadCount, int likeCount, List<String> lessonIds) {

        // Use "forum_answers" as verified
        ref.child("forum_answers").orderByChild("userId").equalTo(tutorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int answerCount = (int) snapshot.getChildrenCount();
                countCompletions(tutorId, ref, uploadCount, likeCount, answerCount, lessonIds);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static void countCompletions(String tutorId, DatabaseReference ref, int uploadCount, int likeCount, int answerCount, List<String> lessonIds) {

        // If strict filter removed all lessons, ensure we don't crash
        if (lessonIds.isEmpty()) {
            finalizeCalculation(tutorId, ref, uploadCount, likeCount, answerCount, 0);
            return;
        }

        final AtomicInteger completedLessonsProcessed = new AtomicInteger(0);
        final AtomicInteger totalCompletions = new AtomicInteger(0);

        for (String lessonId : lessonIds) {
            ref.child("lesson_participation").child(lessonId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot studentRecord : snapshot.getChildren()) {
                        Boolean isCompleted = studentRecord.child("isCompleted").getValue(Boolean.class);
                        if (Boolean.TRUE.equals(isCompleted)) {
                            totalCompletions.incrementAndGet();
                        }
                    }
                    if (completedLessonsProcessed.incrementAndGet() == lessonIds.size()) {
                        finalizeCalculation(tutorId, ref, uploadCount, likeCount, answerCount, totalCompletions.get());
                    }
                }
                public void onDataChange(@NonNull DatabaseError error) {
                    if (completedLessonsProcessed.incrementAndGet() == lessonIds.size()) {
                        finalizeCalculation(tutorId, ref, uploadCount, likeCount, answerCount, totalCompletions.get());
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }

    private static void finalizeCalculation(String tutorId, DatabaseReference ref, int uploads, int likes, int answers, int completions) {

        // --- SCORING FORMULA ---
        int scoreUpload = Math.min(20, uploads * 2);
        int scoreLikes = Math.min(20, likes * 1);
        int scoreAnswers = Math.min(30, answers * 2);
        int scoreCompletions = Math.min(30, completions * 3);

        int totalScore = scoreUpload + scoreLikes + scoreAnswers + scoreCompletions;
        if (totalScore > 100) totalScore = 100;

        Map<String, Boolean> badges = new HashMap<>();
        // Tiers
        if (totalScore >= 80) badges.put("tier_top", true);
        else if (totalScore >= 50) badges.put("tier_gold", true);
        else if (totalScore >= 20) badges.put("tier_silver", true);
        else badges.put("tier_bronze", true);

        // Achievements
        if (completions >= 10) badges.put("ach_impact", true);
        if (answers >= 10) badges.put("ach_helper", true);
        if (likes >= 50) badges.put("ach_loved", true);

        // Save
        Map<String, Object> updates = new HashMap<>();
        updates.put("contributionScore", totalScore);
        updates.put("badges", badges);
        updates.put("stat_completions", completions);
        updates.put("stat_answers", answers);

        ref.child("tutor_profiles").child(tutorId).updateChildren(updates);
        updateListingScore(tutorId, ref, totalScore);
    }

    private static void updateListingScore(String tutorId, DatabaseReference ref, int score) {
        ref.child("tutor_listings").orderByChild("tutorId").equalTo(tutorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ds.getRef().child("contributionScore").setValue(score);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}