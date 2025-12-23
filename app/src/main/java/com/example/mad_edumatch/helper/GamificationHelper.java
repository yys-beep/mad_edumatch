package com.example.mad_edumatch.helper;

import androidx.annotation.NonNull;
import com.example.mad_edumatch.firebaseModels.Answer;
import com.example.mad_edumatch.firebaseModels.FreeLesson;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class GamificationHelper {

    private static final String DB_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    // Call this whenever a Tutor does something (Uploads/Answers)
    public static void calculateScore(String tutorId) {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference();

        // 1. Fetch Lessons (For Uploads, Views, Likes)
        ref.child("free_lessons").orderByChild("tutorId").equalTo(tutorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int uploads = 0;
                int views = 0;
                int lessonLikes = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    FreeLesson l = ds.getValue(FreeLesson.class);
                    if (l != null) {
                        uploads++;
                        // views += l.getViewCount(); // Assuming viewCount exists in FreeLesson
                        if (l.getLikes() != null) lessonLikes += l.getLikes().size();
                    }
                }

                // 2. Fetch Answers (For Q&A Stats)
                fetchAnswers(tutorId, ref, uploads, views, lessonLikes);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static void fetchAnswers(String tutorId, DatabaseReference ref, int uploads, int views, int lessonLikes) {
        // NOTE: This assumes you have an index on 'userId' in 'answers'.
        // If not, for a small project, you can fetch all answers and filter manually.
        ref.child("answers").orderByChild("userId").equalTo(tutorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int answers = 0;
                int answerLikes = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Answer a = ds.getValue(Answer.class);
                    if (a != null) {
                        answers++;
                        if (a.getLikes() != null) answerLikes += a.getLikes().size();
                    }
                }

                performCalculation(tutorId, ref, uploads, views, lessonLikes, answers, answerLikes);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

// ... imports stay same

    private static void performCalculation(String tutorId, DatabaseReference ref, int uploads, int views, int lessonLikes, int answers, int answerLikes) {
        // 1. SCORE CALCULATION (Keep your weights)
        int scoreUpload = Math.min(5, uploads * 2);
        int scoreView = Math.min(15, (int)(views * 0.03));
        int scoreLessonLike = Math.min(35, (int)(lessonLikes * 0.7));
        int scoreQna = Math.min(30, (int)(answers * 1.5));
        int scoreQnaLike = Math.min(15, (int)(answerLikes * 1.5));

        int totalScore = scoreUpload + scoreView + scoreLessonLike + scoreQna + scoreQnaLike;
        if (totalScore > 100) totalScore = 100; // Hard Cap

        int studentsHelped = lessonLikes + answerLikes;

        // 2. BADGE LOGIC (Updated to your Rules)
        Map<String, Boolean> badges = new HashMap<>();

        // --- TIER BADGES (Based on Score) ---
        if (totalScore >= 81) {
            badges.put("tier_top", true);     // 🏆 Top Rated
        } else if (totalScore >= 51) {
            badges.put("tier_gold", true);    // 🥇 High Impact
        } else if (totalScore >= 21) {
            badges.put("tier_silver", true);  // 🥈 Active Contributor
        } else {
            badges.put("tier_bronze", true);  // 🥉 New Tutor (Default 0-20)
        }

        // --- ACHIEVEMENT BADGES (Based on Actions) ---

        // 📹 Lesson Starter (3 Uploads)
        if (uploads >= 3) badges.put("ach_starter", true);

        // ❤️ Crowd Favorite (50 Likes on lessons)
        if (lessonLikes >= 50) badges.put("ach_favorite", true);

        // 🚀 Viral Educator (500 Views)
        if (views >= 500) badges.put("ach_viral", true);

        // ✋ Helper Hand (3 Answers)
        if (answers >= 3) badges.put("ach_helper", true);

        // 🧠 Problem Solver (20 Answers)
        if (answers >= 20) badges.put("ach_solver", true);

        // ✨ Verified Expert (10 Likes on answers)
        if (answerLikes >= 10) badges.put("ach_expert", true);

        // --- SAVE TO FIREBASE ---
        Map<String, Object> updates = new HashMap<>();
        updates.put("contributionScore", totalScore);
        updates.put("totalViews", views);
        updates.put("studentsHelped", studentsHelped);
        updates.put("badges", badges);

        ref.child("tutor_profiles").child(tutorId).updateChildren(updates);

        // Update Listing for search ranking
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