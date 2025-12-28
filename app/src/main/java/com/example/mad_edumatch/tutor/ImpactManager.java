package com.example.mad_edumatch.tutor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.FreeLesson;
import com.example.mad_edumatch.firebaseModels.TutorProfile;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImpactManager {

    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    public static void bindImpact(View dashboardView, String userId, Fragment fragment) {
        if (dashboardView == null || userId == null || !fragment.isAdded()) return;

        TextView tvHosted = dashboardView.findViewById(R.id.tvDashHosted);
        TextView tvHelped = dashboardView.findViewById(R.id.tvDashHelped);
        TextView tvLikes = dashboardView.findViewById(R.id.tvDashLikes);
        TextView tvCompletions = dashboardView.findViewById(R.id.tvDashCompletions); // NEW

        TextView tvScore = dashboardView.findViewById(R.id.tvDashScore);
        ProgressBar pbScore = dashboardView.findViewById(R.id.pbContribution);
        LinearLayout badgeContainer = dashboardView.findViewById(R.id.layoutBadgeContainer);

        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("tutor_profiles").child(userId);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!fragment.isAdded()) return;

                TutorProfile profile = snapshot.getValue(TutorProfile.class);
                if (profile != null) {
                    tvScore.setText(profile.getContributionScore() + "/100");
                    pbScore.setProgress(profile.getContributionScore());

                    renderBadges(badgeContainer, profile.getBadges(), fragment.getContext());

                    // Pass the new TextView to calculation
                    calculateRealStats(userId, tvHosted, tvHelped, tvLikes, tvCompletions);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static void calculateRealStats(String tutorId, TextView tvHosted, TextView tvHelped, TextView tvLikes, TextView tvCompletions) {

        // --- 1. HOSTED LESSONS, LIKES, AND GATHER IDs FOR COMPLETION CHECK ---
        DatabaseReference lessonsRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("free_lessons");

        lessonsRef.orderByChild("tutorId").equalTo(tutorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int hostedCount = 0;
                int totalLikes = 0;
                List<String> myLessonIds = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    FreeLesson lesson = ds.getValue(FreeLesson.class);

                    // STRICT FILTER: Only count if ID matches (Ignore ghosts)
                    if (lesson != null && lesson.getTutorId() != null && lesson.getTutorId().equals(tutorId)) {
                        hostedCount++;
                        if (lesson.getLikes() != null) totalLikes += lesson.getLikes().size();
                        if (lesson.getLessonId() != null) myLessonIds.add(lesson.getLessonId());
                    }
                }

                tvHosted.setText("Hosted Lessons: " + hostedCount);
                tvLikes.setText("Total Likes Received: " + totalLikes);

                // NOW FETCH COMPLETIONS FOR THESE SPECIFIC LESSONS
                countCompletions(myLessonIds, tvCompletions);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // --- 2. QUESTIONS ANSWERED ---
        DatabaseReference answersRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("forum_answers");
        answersRef.orderByChild("userId").equalTo(tutorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long answerCount = snapshot.getChildrenCount();
                tvHelped.setText("Questions Answered: " + answerCount);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static void countCompletions(List<String> lessonIds, TextView tvCompletions) {
        if (lessonIds.isEmpty()) {
            tvCompletions.setText("Students Completed: 0");
            return;
        }

        DatabaseReference partRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("lesson_participation");
        final int[] totalCompleted = {0};
        final int[] processedCount = {0};

        for (String lessonId : lessonIds) {
            partRef.child(lessonId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot studentRecord : snapshot.getChildren()) {
                        Boolean isCompleted = studentRecord.child("isCompleted").getValue(Boolean.class);
                        if (Boolean.TRUE.equals(isCompleted)) {
                            totalCompleted[0]++;
                        }
                    }
                    processedCount[0]++;
                    if (processedCount[0] == lessonIds.size()) {
                        tvCompletions.setText("Students Completed: " + totalCompleted[0]);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {
                    processedCount[0]++; // Prevent hanging
                }
            });
        }
    }

    // --- BADGES RENDERING ---
    private static void renderBadges(LinearLayout container, Map<String, Boolean> badges, Context context) {
        container.removeAllViews();
        if (badges == null) return;

        for (Map.Entry<String, Boolean> entry : badges.entrySet()) {
            if (entry.getValue()) {
                String key = entry.getKey();
                ImageView badgeIcon = new ImageView(context);
                int drawableId = getBadgeDrawable(key);
                if (drawableId == 0) continue;

                badgeIcon.setImageResource(drawableId);
                badgeIcon.setImageTintList(ColorStateList.valueOf(getBadgeColor(key, context)));

                int size = (int) (42 * context.getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(0, 0, 24, 0);
                badgeIcon.setLayoutParams(params);
                badgeIcon.setOnClickListener(v -> showBadgeDialog(key, context));
                container.addView(badgeIcon);
            }
        }
    }

    private static int getBadgeDrawable(String key) {
        switch (key) {
            case "tier_top": case "tier_gold": case "tier_silver": case "tier_bronze": return R.drawable.outline_workspace_premium_24;
            case "ach_impact": return R.drawable.outline_auto_stories_24;
            case "ach_helper": return R.drawable.outline_volunteer_activism_24;
            case "ach_loved": return R.drawable.baseline_favorite_border_24;
            default: return R.drawable.outline_award_star_24;
        }
    }

    private static int getBadgeColor(String key, Context context) {
        switch (key) {
            case "tier_top": return Color.parseColor("#E91E63");
            case "tier_gold": return Color.parseColor("#FF8F00");
            case "tier_silver": return Color.parseColor("#616161");
            case "tier_bronze": return Color.parseColor("#795548");
            case "ach_loved": return Color.parseColor("#D32F2F");
            default: return Color.parseColor("#1565C0");
        }
    }

    private static void showBadgeDialog(String key, Context context) {
        String title, desc;
        switch (key) {
            case "tier_top": title = "Top Rated Tutor"; desc = "Elite educator status!"; break;
            case "tier_gold": title = "Gold Tier"; desc = "High impact contributor."; break;
            case "tier_silver": title = "Silver Tier"; desc = "Recognized contributor."; break;
            case "tier_bronze": title = "Bronze Tier"; desc = "Welcome to the community."; break;
            case "ach_impact": title = "Impact Creator"; desc = "10+ Students completed your lessons."; break;
            case "ach_helper": title = "Community Helper"; desc = "10+ Questions answered."; break;
            case "ach_loved": title = "Crowd Favorite"; desc = "50+ Likes received."; break;
            default: title = "Achievement"; desc = "Badge unlocked!"; break;
        }
        new AlertDialog.Builder(context).setTitle(title).setMessage(desc).setPositiveButton("OK", null).show();
    }
}