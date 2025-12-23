package com.example.mad_edumatch.student;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.chat.ChatDetailFragment;
import com.example.mad_edumatch.firebaseModels.FreeLesson;
import com.example.mad_edumatch.firebaseModels.TutorProfile;
import com.example.mad_edumatch.freeLesson.FreeLessonDetailFragment;
import com.example.mad_edumatch.helper.AvatarManager;
import com.example.mad_edumatch.helper.GamificationHelper; // Make sure this exists
import com.example.mad_edumatch.recycleAdapters.FreeLessonAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

public class StudentViewTutorProfileFragment extends Fragment {

    private String tutorId;

    // UI Fields
    private ImageView ivAvatar;
    private TextView tvName, tvSubHeader, tvDesc, tvSubject, tvFee, tvContact, tvExperience, tvAchievements;
    private Button btnContact;

    // Dashboard Views
    private TextView tvDashViews, tvDashHelped, tvDashScore;
    private ProgressBar pbContribution;
    private LinearLayout layoutBadgeContainer;

    // Lesson List
    private TextView tvFreeLessonsTitle, tvNoLessons;
    private RecyclerView rvLessons;
    private FreeLessonAdapter lessonAdapter;
    private ArrayList<FreeLesson> lessonList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_view_tutor, container, false);

        if (getArguments() != null) {
            tutorId = getArguments().getString("tutorId");
        }

        // Trigger Score Calculation when viewing profile
        if (tutorId != null) {
            GamificationHelper.calculateScore(tutorId);
        }

        bindViews(view);

        rvLessons.setLayoutManager(new LinearLayoutManager(getContext()));
        lessonAdapter = new FreeLessonAdapter(lessonList, this::openLessonDetail);
        rvLessons.setAdapter(lessonAdapter);

        loadTutorData();
        loadTutorLessons();

        return view;
    }

    private void bindViews(View view) {
        ivAvatar = view.findViewById(R.id.ivTutorAvatar);
        tvName = view.findViewById(R.id.tvTutorName);
        tvSubHeader = view.findViewById(R.id.tvTutorSubHeader);
        tvDesc = view.findViewById(R.id.tvTutorDesc);
        tvSubject = view.findViewById(R.id.tvTutorSubject);
        tvFee = view.findViewById(R.id.tvTutorFee);
        tvContact = view.findViewById(R.id.tvTutorContact);
        tvExperience = view.findViewById(R.id.tvTutorExperience);
        tvAchievements = view.findViewById(R.id.tvTutorAchievements);
        btnContact = view.findViewById(R.id.btnContactTutor);

        // Dashboard
        tvDashViews = view.findViewById(R.id.tvDashViews);
        tvDashHelped = view.findViewById(R.id.tvDashHelped);
        tvDashScore = view.findViewById(R.id.tvDashScore);
        pbContribution = view.findViewById(R.id.pbContribution);
        layoutBadgeContainer = view.findViewById(R.id.layoutBadgeContainer);

        // Lessons
        tvFreeLessonsTitle = view.findViewById(R.id.tvFreeLessonsTitle);
        tvNoLessons = view.findViewById(R.id.tvNoLessons);
        rvLessons = view.findViewById(R.id.rvTutorFreeLessons);

        btnContact.setOnClickListener(v -> openChat());
    }

    private void loadTutorData() {
        if (tutorId == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_profiles").child(tutorId);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                TutorProfile p = snapshot.getValue(TutorProfile.class);
                if (p != null) {
                    tvName.setText(p.getUsername() != null ? p.getUsername() : "Tutor");
                    tvDesc.setText(p.getDescription() != null ? p.getDescription() : "N/A");
                    tvFee.setText(p.getFee() != null ? "RM " + p.getFee() + "/hr" : "N/A");
                    tvContact.setText(p.getContact() != null ? p.getContact() : "N/A");
                    tvSubHeader.setText(p.getQualification() != null ? p.getQualification() : "N/A");

                    if (p.getSubjects() != null) {
                        StringBuilder sb = new StringBuilder();
                        for (String s : p.getSubjects()) sb.append(s).append(", ");
                        String sText = sb.toString();
                        if (sText.length() > 2) sText = sText.substring(0, sText.length() - 2);
                        tvSubject.setText(sText);
                    } else {
                        tvSubject.setText("N/A");
                    }

                    if (p.getProfileImageUrl() != null) {
                        int resId = AvatarManager.getAvatarResourceId(p.getProfileImageUrl());
                        if (resId != 0) ivAvatar.setImageResource(resId);
                    }

                    // Populate Dashboard
                    tvDashViews.setText("👀 Views: " + p.getTotalViews());
                    tvDashHelped.setText("🎓 Helped: " + p.getStudentsHelped());
                    tvDashScore.setText(p.getContributionScore() + "/100");
                    pbContribution.setProgress(p.getContributionScore());

                    renderBadges(p.getBadges());

                    // Populate Experience List
                    if (p.getExperience() != null && !p.getExperience().isEmpty()) {
                        // Simple formatted string for list
                        StringBuilder sb = new StringBuilder();
                        for(String e : p.getExperience()) sb.append("• ").append(e).append("\n");
                        tvExperience.setText(sb.toString().trim());
                    } else {
                        tvExperience.setText("No experience recorded.");
                    }

                    // Populate Achievements List
                    if (p.getAchievement() != null && !p.getAchievement().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for(String a : p.getAchievement()) sb.append("• ").append(a).append("\n");
                        tvAchievements.setText(sb.toString().trim());
                    } else {
                        tvAchievements.setText("No achievements recorded.");
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void renderBadges(Map<String, Boolean> badges) {
        if (layoutBadgeContainer == null) return;
        layoutBadgeContainer.removeAllViews();
        if (badges == null) return;

        // Render specific badges
        if (badges.containsKey("tier_top")) addBadgeIcon("tier_top", "🏆");
        else if (badges.containsKey("tier_gold")) addBadgeIcon("tier_gold", "🥇");
        else if (badges.containsKey("tier_silver")) addBadgeIcon("tier_silver", "🥈");
        else addBadgeIcon("tier_bronze", "🥉");

        if (badges.containsKey("ach_starter")) addBadgeIcon("ach_starter", "📹");
        if (badges.containsKey("ach_favorite")) addBadgeIcon("ach_favorite", "❤️");
        if (badges.containsKey("ach_viral")) addBadgeIcon("ach_viral", "🚀");
        if (badges.containsKey("ach_helper")) addBadgeIcon("ach_helper", "✋");
        if (badges.containsKey("ach_solver")) addBadgeIcon("ach_solver", "🧠");
        if (badges.containsKey("ach_expert")) addBadgeIcon("ach_expert", "✨");
    }

    private void addBadgeIcon(String badgeKey, String iconEmoji) {
        TextView tv = new TextView(getContext());
        tv.setText(iconEmoji);
        tv.setTextSize(24);
        tv.setPadding(20, 10, 20, 10);
        tv.setBackgroundResource(R.drawable.circle_bg_light);
        tv.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 16, 0);
        tv.setLayoutParams(params);

        tv.setOnClickListener(v -> showBadgeInfo(badgeKey));
        layoutBadgeContainer.addView(tv);
    }

    private void showBadgeInfo(String key) {
        String title = "Badge", desc = "Description";
        // Simple logic for demo, you can expand this switch case
        switch(key) {
            case "tier_bronze": title = "New Tutor"; desc = "Just started."; break;
            case "tier_silver": title = "Active Contributor"; desc = "Regular participation."; break;
            case "tier_gold": title = "High Impact"; desc = "Very helpful community member."; break;
            case "tier_top": title = "Top Rated"; desc = "Elite top 5% of tutors."; break;
            case "ach_starter": title = "Lesson Starter"; desc = "3 lessons uploaded."; break;
        }
        new AlertDialog.Builder(getContext()).setTitle(title).setMessage(desc).setPositiveButton("OK", null).show();
    }

    private void loadTutorLessons() {
        if (tutorId == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("free_lessons");

        ref.orderByChild("tutorId").equalTo(tutorId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                lessonList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FreeLesson fl = ds.getValue(FreeLesson.class);
                    if (fl != null) lessonList.add(fl);
                }
                tvFreeLessonsTitle.setText("Hosted Free Lessons (" + lessonList.size() + ")");
                if (lessonList.isEmpty()) {
                    tvNoLessons.setVisibility(View.VISIBLE);
                    rvLessons.setVisibility(View.GONE);
                } else {
                    tvNoLessons.setVisibility(View.GONE);
                    rvLessons.setVisibility(View.VISIBLE);
                }
                lessonAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void openLessonDetail(FreeLesson lesson) {
        FreeLessonDetailFragment fragment = new FreeLessonDetailFragment();
        Bundle args = new Bundle();
        args.putString("lessonId", lesson.getLessonId());
        fragment.setArguments(args);
        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
    }

    private void openChat() {
        ChatDetailFragment chatFragment = new ChatDetailFragment();
        Bundle args = new Bundle();
        args.putString("targetUserId", tutorId);
        chatFragment.setArguments(args);
        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, chatFragment).addToBackStack(null).commit();
    }
}