package com.example.mad_edumatch.tutor;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.FreeLesson;
import com.example.mad_edumatch.firebaseModels.TutorProfile;
import com.example.mad_edumatch.freeLesson.FreeLessonDetailFragment;
import com.example.mad_edumatch.helper.AvatarManager;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.helper.GamificationHelper;
import com.example.mad_edumatch.recycleAdapters.AchievementAdapter;
import com.example.mad_edumatch.recycleAdapters.FreeLessonAdapter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class TutorProfileFragment extends Fragment {

    private ImageView imgProfile;
    private TextView tvName, tvQual, tvDesc, tvSubjects, tvFee, tvArea, tvContact, tvEmail;
    private Button btnEdit;

    private TextView tvHeaderLessons, tvHeaderExperience, tvHeaderAchievements;
    private TextView tvLessonCollapseToggle, tvExperienceCollapseToggle, tvAchievementsCollapseToggle;
    private TextView tvNoExperience, tvNoAchievements;

    private RecyclerView rvLessons, rvExperience, rvAchievements;
    private FreeLessonAdapter freeLessonAdapter;
    private AchievementAdapter experienceAdapter, achievementAdapter;

    private ArrayList<FreeLesson> hostedLessonList = new ArrayList<>();
    private ArrayList<String> experienceList = new ArrayList<>();
    private ArrayList<String> achievementList = new ArrayList<>();

    // State
    // CHANGE 1: Start collapsed (false) so it shows only 3 items initially
    private boolean isLessonsExpanded = false;
    private boolean isExperienceExpanded = false;
    private boolean isAchievementsExpanded = false;
    private static final int INITIAL_ITEM_LIMIT = 3;
    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    private String profileUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tutor_fragment_profile, container, false);

        if (getArguments() != null && getArguments().getString("targetUserId") != null) {
            profileUserId = getArguments().getString("targetUserId");
        } else {
            profileUserId = CurrentUser.getInstance().getUid();
        }

        bindViews(view);
        setupRecyclerViews();

        if (profileUserId != null && profileUserId.equals(CurrentUser.getInstance().getUid())) {
            btnEdit.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(v -> getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TutorEditProfileFragment())
                    .addToBackStack(null).commit());
        } else {
            btnEdit.setVisibility(View.GONE);
        }

        GamificationHelper.calculateScore(profileUserId);
        loadHostedLessons();
        loadProfile();
        loadDashboardData(view);

        return view;
    }

    private void bindViews(View view) {
        imgProfile = view.findViewById(R.id.imgTotorProfilePic);
        tvName = view.findViewById(R.id.tvTutorName);
        tvQual = view.findViewById(R.id.tvTutorQualification);
        tvDesc = view.findViewById(R.id.tvTutorDescription);
        tvSubjects = view.findViewById(R.id.tvTutorSubjects);
        tvFee = view.findViewById(R.id.tvTutorFee);
        tvArea = view.findViewById(R.id.tvTutorArea);
        tvContact = view.findViewById(R.id.tvTutorContact);
        tvEmail = view.findViewById(R.id.tvTutorEmail);
        btnEdit = view.findViewById(R.id.btnEditProfile);

        tvHeaderLessons = view.findViewById(R.id.tvHeaderLessons);
        tvHeaderExperience = view.findViewById(R.id.tvHeaderExperience);
        tvHeaderAchievements = view.findViewById(R.id.tvHeaderAchievements);

        rvLessons = view.findViewById(R.id.rvTutorHostedLessons);
        tvLessonCollapseToggle = view.findViewById(R.id.tvLessonCollapseToggle);

        rvExperience = view.findViewById(R.id.rvTutorExperience);
        tvExperienceCollapseToggle = view.findViewById(R.id.tvExperienceCollapseToggle);
        tvNoExperience = view.findViewById(R.id.tvNoExperience);

        rvAchievements = view.findViewById(R.id.rvTutorAchievements);
        tvAchievementsCollapseToggle = view.findViewById(R.id.tvAchievementsCollapseToggle);
        tvNoAchievements = view.findViewById(R.id.tvNoAchievements);
    }

    private void setupRecyclerViews() {
        rvLessons.setLayoutManager(new LinearLayoutManager(getContext()));
        freeLessonAdapter = new FreeLessonAdapter(hostedLessonList, this::openLessonDetail);
        // CHANGE 2: Set initial limit
        freeLessonAdapter.setLimit(INITIAL_ITEM_LIMIT);
        rvLessons.setAdapter(freeLessonAdapter);
        // Important: Prevent nested scrolling issues if inside ScrollView
        rvLessons.setNestedScrollingEnabled(false);

        rvExperience.setLayoutManager(new LinearLayoutManager(getContext()));
        experienceAdapter = new AchievementAdapter(experienceList);
        experienceAdapter.setLimit(INITIAL_ITEM_LIMIT);
        rvExperience.setAdapter(experienceAdapter);
        rvExperience.setNestedScrollingEnabled(false);

        rvAchievements.setLayoutManager(new LinearLayoutManager(getContext()));
        achievementAdapter = new AchievementAdapter(achievementList);
        achievementAdapter.setLimit(INITIAL_ITEM_LIMIT);
        rvAchievements.setAdapter(achievementAdapter);
        rvAchievements.setNestedScrollingEnabled(false);

        tvLessonCollapseToggle.setOnClickListener(v -> toggleLessonView());
        tvExperienceCollapseToggle.setOnClickListener(v -> toggleExperienceView());
        tvAchievementsCollapseToggle.setOnClickListener(v -> toggleAchievementView());
    }

    private void loadDashboardData(View root) {
        if (profileUserId == null || root == null) return;
        View impactView = root.findViewById(R.id.layoutDashboard);
        if (impactView != null) {
            ImpactManager.bindImpact(impactView, profileUserId, this);
        }
    }

    private void loadProfile() {
        if (profileUserId == null) return;
        DatabaseReference userRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("tutor_profiles").child(profileUserId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                TutorProfile profile = snapshot.getValue(TutorProfile.class);
                if (profile != null) {
                    tvName.setText(getSafeDisplayString(profile.getUsername()));
                    tvQual.setText(getSafeDisplayString(profile.getQualification()));
                    tvDesc.setText(getSafeDisplayString(profile.getDescription()));
                    tvContact.setText("Contact: " + getSafeDisplayString(profile.getContact()));
                    tvArea.setText("Area: " + getSafeDisplayString(profile.getArea()));
                    tvEmail.setText("Email: " + getSafeDisplayString(profile.getEmail()));

                    String fee = getSafeDisplayString(profile.getFee());
                    tvFee.setText(fee.equals("N/A") ? "Hourly Fee: N/A" : "Hourly Fee: RM " + fee + "/hr");

                    String subjectsText = (profile.getSubjects() != null && !profile.getSubjects().isEmpty())
                            ? TextUtils.join(", ", profile.getSubjects()) : "N/A";
                    tvSubjects.setText("Subjects: " + subjectsText);

                    // Sync Experience
                    experienceList.clear();
                    if (profile.getExperience() != null) experienceList.addAll(profile.getExperience());
                    tvHeaderExperience.setText("Experience (" + experienceList.size() + ")");
                    refreshSectionUI(experienceList.size(), rvExperience, tvNoExperience, tvExperienceCollapseToggle, experienceAdapter, isExperienceExpanded);

                    // Sync Achievements
                    achievementList.clear();
                    if (profile.getAchievement() != null) achievementList.addAll(profile.getAchievement());
                    tvHeaderAchievements.setText("Achievements (" + achievementList.size() + ")");
                    refreshSectionUI(achievementList.size(), rvAchievements, tvNoAchievements, tvAchievementsCollapseToggle, achievementAdapter, isAchievementsExpanded);

                    int resId = AvatarManager.getAvatarResourceId(profile.getProfileImageUrl());
                    imgProfile.setImageResource(resId != 0 ? resId : R.drawable.avatar_1);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Helper for Experience/Achievement
    private void refreshSectionUI(int size, RecyclerView rv, TextView emptyTv, TextView toggle, AchievementAdapter adapter, boolean isExpanded) {
        if (size == 0) {
            rv.setVisibility(View.GONE);
            emptyTv.setVisibility(View.VISIBLE);
            toggle.setVisibility(View.GONE);
        } else {
            rv.setVisibility(View.VISIBLE);
            emptyTv.setVisibility(View.GONE);
            if (size > INITIAL_ITEM_LIMIT) {
                toggle.setVisibility(View.VISIBLE);
                toggle.setText(isExpanded ? "Collapse" : "View All");
                adapter.setLimit(isExpanded ? size : INITIAL_ITEM_LIMIT);
            } else {
                toggle.setVisibility(View.GONE);
                adapter.setLimit(size);
            }
            adapter.notifyDataSetChanged();
        }
    }

    // CHANGE 3: New Helper logic specifically for Lessons
    private void refreshLessonUI() {
        int size = hostedLessonList.size();
        tvHeaderLessons.setText("Hosted Free Lessons (" + size + ")");

        if (size == 0) {
            rvLessons.setVisibility(View.GONE);
            tvLessonCollapseToggle.setVisibility(View.GONE);
            // Optional: You could show a "No lessons" textview here if you have one
        } else {
            rvLessons.setVisibility(View.VISIBLE);
            if (size > INITIAL_ITEM_LIMIT) {
                tvLessonCollapseToggle.setVisibility(View.VISIBLE);
                tvLessonCollapseToggle.setText(isLessonsExpanded ? "Collapse" : "View All");

                // 0 means "Show All" in FreeLessonAdapter, INITIAL_ITEM_LIMIT means "Show 3"
                freeLessonAdapter.setLimit(isLessonsExpanded ? 0 : INITIAL_ITEM_LIMIT);
            } else {
                tvLessonCollapseToggle.setVisibility(View.GONE);
                freeLessonAdapter.setLimit(0); // Show everything since it fits
            }
            freeLessonAdapter.notifyDataSetChanged();
        }
    }

    private void toggleExperienceView() {
        isExperienceExpanded = !isExperienceExpanded;
        refreshSectionUI(experienceList.size(), rvExperience, tvNoExperience, tvExperienceCollapseToggle, experienceAdapter, isExperienceExpanded);
    }

    private void toggleAchievementView() {
        isAchievementsExpanded = !isAchievementsExpanded;
        refreshSectionUI(achievementList.size(), rvAchievements, tvNoAchievements, tvAchievementsCollapseToggle, achievementAdapter, isAchievementsExpanded);
    }

    // CHANGE 4: Updated toggle logic to use refreshLessonUI
    private void toggleLessonView() {
        isLessonsExpanded = !isLessonsExpanded;
        refreshLessonUI();
    }

    private void loadHostedLessons() {
        if (profileUserId == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("free_lessons");
        ref.orderByChild("tutorId").equalTo(profileUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!isAdded()) return;
                hostedLessonList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FreeLesson lesson = ds.getValue(FreeLesson.class);
                    if (lesson != null) {
                        lesson.setLessonId(ds.getKey());
                        hostedLessonList.add(lesson);
                    }
                }
                hostedLessonList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                // CHANGE 5: Call the new helper instead of manually setting visibility
                refreshLessonUI();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getSafeDisplayString(String value) {
        return (value == null || value.trim().isEmpty() || value.equals("-") || value.equals("N/A")) ? "N/A" : value.trim();
    }

    private void openLessonDetail(FreeLesson lesson) {
        FreeLessonDetailFragment fragment = new FreeLessonDetailFragment();
        Bundle args = new Bundle();
        args.putString("lessonId", lesson.getLessonId());
        args.putString("title", lesson.getTitle());
        args.putString("videoUrl", lesson.getVideoLink());
        args.putString("materialUrl", lesson.getMaterialUrl());
        args.putString("materialName", lesson.getMaterialName());
        fragment.setArguments(args);
        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
    }
}