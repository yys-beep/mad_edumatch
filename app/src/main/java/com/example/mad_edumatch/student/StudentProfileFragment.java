package com.example.mad_edumatch.student;

import android.os.Bundle;
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
import com.example.mad_edumatch.firebaseModels.StudentProfile;
import com.example.mad_edumatch.freeLesson.FreeLessonDetailFragment;
import com.example.mad_edumatch.helper.AvatarManager;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.recycleAdapters.AchievementAdapter;
import com.example.mad_edumatch.recycleAdapters.FreeLessonAdapter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StudentProfileFragment extends Fragment {

    // Views
    private TextView tvStudentName, tvStudentAge, tvStudentAcademic, tvStudentDescription, tvStudentEmail, tvStudentContact;
    private TextView tvStudentAchievementsTitle, tvStudentFreeLessonsTitle;
    private RecyclerView rvAchievements, rvStudentFreeLessons;
    private Button btnEditProfile;
    private ImageView imgStudentProfilePic;

    // Toggles and Empty Messages
    private TextView tvAchievementCollapseToggle;
    private TextView tvLessonsCollapseToggle;
    private TextView tvNoAchievements;
    private TextView tvNoLessons;

    // Adapters
    private AchievementAdapter achievementAdapter;
    private FreeLessonAdapter freeLessonAdapter;

    // Data Lists
    private ArrayList<String> achievementList = new ArrayList<>();
    private ArrayList<FreeLesson> freeLessonList = new ArrayList<>();

    // State
    private boolean isAchievementExpanded = false;
    private boolean isLessonsExpanded = false;

    private static final int INITIAL_ITEM_LIMIT = 3;
    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    private String profileUserId;
    private View rootView; // Cache the view

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // 1. View Caching: If view exists, reuse it to prevent flicker
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.student_fragment_profile, container, false);

            bindViews(rootView);

            // Determine User ID
            if (getArguments() != null && getArguments().getString("targetUserId") != null) {
                profileUserId = getArguments().getString("targetUserId");
                btnEditProfile.setVisibility(View.GONE);
            } else {
                profileUserId = CurrentUser.getInstance().getUid();
                btnEditProfile.setVisibility(View.VISIBLE);
            }

            setupRecyclerViews();

            btnEditProfile.setOnClickListener(v -> {
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new StudentEditProfileFragment())
                        .addToBackStack(null)
                        .commit();
            });

            loadStudentProfile();
        }

        return rootView;
    }

    // 2. Refresh data silently when returning (keeps view intact)
    @Override
    public void onResume() {
        super.onResume();
        if (rootView != null) {
            loadStudentProfile();
        }
    }

    private void bindViews(View view) {
        imgStudentProfilePic = view.findViewById(R.id.imgStudentProfilePic);
        tvStudentName = view.findViewById(R.id.tvStudentName);
        tvStudentAge = view.findViewById(R.id.tvStudentAge);
        tvStudentAcademic = view.findViewById(R.id.tvStudentAcademic);
        tvStudentDescription = view.findViewById(R.id.tvStudentDescription);
        tvStudentEmail = view.findViewById(R.id.tvStudentEmail);
        tvStudentContact = view.findViewById(R.id.tvStudentContact);
        tvStudentAchievementsTitle = view.findViewById(R.id.tvStudentAchievementsTitle);
        tvStudentFreeLessonsTitle = view.findViewById(R.id.tvStudentFreeLessonsTitle);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        rvAchievements = view.findViewById(R.id.rvStudentAchievements);
        rvStudentFreeLessons = view.findViewById(R.id.rvStudentFreeLessons);

        tvAchievementCollapseToggle = view.findViewById(R.id.tvAchievementCollapseToggle);
        tvLessonsCollapseToggle = view.findViewById(R.id.tvLessonsCollapseToggle);

        tvNoAchievements = view.findViewById(R.id.tvNoAchievements);
        tvNoLessons = view.findViewById(R.id.tvNoLessons);

        // --- FIX 3: Clear XML Defaults Immediately ---
        // This prevents "Name", "Age", "Description" from flashing on screen
        tvStudentName.setText("");
        tvStudentAge.setText("");
        tvStudentAcademic.setText("");
        tvStudentDescription.setText("");
        tvStudentEmail.setText("");
        tvStudentContact.setText("");

        // Hide "No Data" messages initially so they don't flash
        tvNoAchievements.setVisibility(View.GONE);
        tvNoLessons.setVisibility(View.GONE);
    }

    private void setupRecyclerViews() {
        rvAchievements.setLayoutManager(new LinearLayoutManager(getContext()));
        achievementAdapter = new AchievementAdapter(achievementList);
        achievementAdapter.setLimit(INITIAL_ITEM_LIMIT);
        rvAchievements.setAdapter(achievementAdapter);

        rvStudentFreeLessons.setLayoutManager(new LinearLayoutManager(getContext()));
        freeLessonAdapter = new FreeLessonAdapter(freeLessonList, this::openLessonDetail);
        freeLessonAdapter.setLimit(INITIAL_ITEM_LIMIT);
        rvStudentFreeLessons.setAdapter(freeLessonAdapter);
        rvStudentFreeLessons.setNestedScrollingEnabled(false);

        tvAchievementCollapseToggle.setOnClickListener(v -> toggleAchievementView());
        tvLessonsCollapseToggle.setOnClickListener(v -> toggleLessonsView());
    }

    // Helper to refresh Lesson UI (Matches Tutor Profile Logic)
    private void refreshLessonUI() {
        if (!isAdded()) return;
        int size = freeLessonList.size();
        tvStudentFreeLessonsTitle.setText("Participated Free Lessons (" + size + ")");

        if (size == 0) {
            rvStudentFreeLessons.setVisibility(View.GONE);
            tvLessonsCollapseToggle.setVisibility(View.GONE);
            tvNoLessons.setVisibility(View.VISIBLE);
        } else {
            rvStudentFreeLessons.setVisibility(View.VISIBLE);
            tvNoLessons.setVisibility(View.GONE);

            if (size > INITIAL_ITEM_LIMIT) {
                tvLessonsCollapseToggle.setVisibility(View.VISIBLE);
                tvLessonsCollapseToggle.setText(isLessonsExpanded ? "Collapse" : "View All");
                freeLessonAdapter.setLimit(isLessonsExpanded ? 0 : INITIAL_ITEM_LIMIT);
            } else {
                tvLessonsCollapseToggle.setVisibility(View.GONE);
                freeLessonAdapter.setLimit(0);
            }
            freeLessonAdapter.notifyDataSetChanged();
        }
    }

    private void toggleLessonsView() {
        isLessonsExpanded = !isLessonsExpanded;
        refreshLessonUI();
    }

    private void toggleAchievementView() {
        isAchievementExpanded = !isAchievementExpanded;
        int limit = isAchievementExpanded ? achievementList.size() : INITIAL_ITEM_LIMIT;
        achievementAdapter.setLimit(limit);
        achievementAdapter.notifyDataSetChanged();
        tvAchievementCollapseToggle.setText(isAchievementExpanded ? "Collapse" : "View All");
    }

    private void openLessonDetail(FreeLesson lesson) {
        FreeLessonDetailFragment fragment = new FreeLessonDetailFragment();
        Bundle args = new Bundle();
        args.putString("lessonId", lesson.getLessonId());
        args.putString("tutorName", lesson.getTutorName());
        args.putString("tutorId", lesson.getTutorId());
        args.putString("title", lesson.getTitle());
        args.putString("desc", lesson.getDescription());
        args.putString("videoUrl", lesson.getVideoLink());
        args.putString("matUrl", lesson.getMaterialUrl());
        args.putString("matName", lesson.getMaterialName());
        fragment.setArguments(args);
        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
    }

    // ====================================================================
    // DATA LOADING
    // ====================================================================

    private void loadStudentProfile() {
        if (profileUserId == null) return;

        loadParticipatedLessons(profileUserId);

        DatabaseReference userRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("student_profiles")
                .child(profileUserId);

        // FIX 4: Use addValueEventListener (Realtime).
        // This is often faster than .get() because it uses the persistent cache.
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                StudentProfile profile = snapshot.getValue(StudentProfile.class);
                if (profile == null) return;

                tvStudentName.setText(profile.getUsername());
                tvStudentAge.setText("Age: " + profile.getAge());
                tvStudentAcademic.setText(profile.getAcademicLevel());
                tvStudentDescription.setText(profile.getDescription());
                tvStudentEmail.setText("Email: " + profile.getEmail());
                tvStudentContact.setText("Contact: " + profile.getContact());

                int resId = AvatarManager.getAvatarResourceId(profile.getProfileImageUrl());
                if (resId != 0) imgStudentProfilePic.setImageResource(resId);

                achievementList.clear();
                if (profile.getAchievements() != null) {
                    achievementList.addAll(profile.getAchievements());
                }

                if (achievementList.isEmpty()) {
                    rvAchievements.setVisibility(View.GONE);
                    tvNoAchievements.setVisibility(View.VISIBLE);
                    tvAchievementCollapseToggle.setVisibility(View.GONE);
                } else {
                    rvAchievements.setVisibility(View.VISIBLE);
                    tvNoAchievements.setVisibility(View.GONE);

                    if (achievementList.size() > INITIAL_ITEM_LIMIT) {
                        tvAchievementCollapseToggle.setVisibility(View.VISIBLE);
                        tvAchievementCollapseToggle.setText(isAchievementExpanded ? "Collapse" : "View All");
                        achievementAdapter.setLimit(isAchievementExpanded ? achievementList.size() : INITIAL_ITEM_LIMIT);
                    } else {
                        tvAchievementCollapseToggle.setVisibility(View.GONE);
                        achievementAdapter.setLimit(INITIAL_ITEM_LIMIT);
                    }
                }
                achievementAdapter.notifyDataSetChanged();
                tvStudentAchievementsTitle.setText("Achievements (" + achievementList.size() + ")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadParticipatedLessons(String uidToLoad) {
        // Use addValueEventListener here too for consistency and speed
        FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("lesson_participation")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        List<String> ids = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            if (ds.hasChild(uidToLoad) &&
                                    Boolean.TRUE.equals(ds.child(uidToLoad).child("isCompleted").getValue(Boolean.class))) {
                                ids.add(ds.getKey());
                            }
                        }
                        fetchLessonDetails(ids);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        finalizeLessonLoad();
                    }
                });
    }

    private void fetchLessonDetails(List<String> ids) {
        if (ids.isEmpty()) {
            finalizeLessonLoad();
            return;
        }

        freeLessonList.clear();
        final int[] count = {0};
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("free_lessons");

        for (String id : ids) {
            ref.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    FreeLesson lesson = snapshot.getValue(FreeLesson.class);
                    if (lesson != null) {
                        lesson.setLessonId(id);
                        freeLessonList.add(lesson);
                    }
                    count[0]++;
                    if (count[0] == ids.size()) finalizeLessonLoad();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    count[0]++;
                    if (count[0] == ids.size()) finalizeLessonLoad();
                }
            });
        }
    }

    private void finalizeLessonLoad() {
        if (!isAdded()) return;
        freeLessonList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        refreshLessonUI();
    }
}