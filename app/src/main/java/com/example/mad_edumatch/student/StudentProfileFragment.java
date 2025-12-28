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
    private Button btnEditProfile, btnChangeLanguage, btnBackToHome;
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

    // State & Constants
    private boolean isAchievementExpanded = false; // Starts Collapsed (Limit 3)
    private boolean isLessonsExpanded = true;      // Starts Expanded (Visible)

    private static final int INITIAL_ITEM_LIMIT = 3;
    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    // Variable to determine WHICH user we are viewing
    private String profileUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.student_fragment_profile, container, false);

        bindViews(view);

        // --- 1. DETERMINE USER ID ---
        if (getArguments() != null && getArguments().getString("targetUserId") != null) {
            // Case A: Viewing someone else (passed from Chat)
            profileUserId = getArguments().getString("targetUserId");

            // HIDE EDIT BUTTON because it's not our profile
            btnEditProfile.setVisibility(View.GONE);
            btnChangeLanguage.setVisibility(View.GONE);
            btnBackToHome.setVisibility(View.VISIBLE);

        } else {
            // Case B: Viewing my own profile (Default)
            profileUserId = CurrentUser.getInstance().getUid();

            // SHOW EDIT BUTTON
            btnEditProfile.setVisibility(View.VISIBLE);
            btnChangeLanguage.setVisibility(View.VISIBLE);
            btnBackToHome.setVisibility(View.VISIBLE);
        }

        setupRecyclerViews();

        btnEditProfile.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StudentEditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });
        btnChangeLanguage.setOnClickListener(v -> showLanguageDialog());

        btnBackToHome.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
        loadStudentProfile();

        return view;
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
        btnChangeLanguage = view.findViewById(R.id.btnChangeLanguageProfile);
        btnBackToHome = view.findViewById(R.id.btnBackToHome);
        rvAchievements = view.findViewById(R.id.rvStudentAchievements);
        rvStudentFreeLessons = view.findViewById(R.id.rvStudentFreeLessons);

        tvAchievementCollapseToggle = view.findViewById(R.id.tvAchievementCollapseToggle);
        tvLessonsCollapseToggle = view.findViewById(R.id.tvLessonsCollapseToggle);

        tvNoAchievements = view.findViewById(R.id.tvNoAchievements);
        tvNoLessons = view.findViewById(R.id.tvNoLessons);
    }

    private void setupRecyclerViews() {
        // 1. Achievements (Uses Limit Logic)
        rvAchievements.setLayoutManager(new LinearLayoutManager(getContext()));
        achievementAdapter = new AchievementAdapter(achievementList);
        achievementAdapter.setLimit(INITIAL_ITEM_LIMIT);
        rvAchievements.setAdapter(achievementAdapter);

        // 2. Lessons (Uses Show/Hide Logic)
        rvStudentFreeLessons.setLayoutManager(new LinearLayoutManager(getContext()));
        freeLessonAdapter = new FreeLessonAdapter(freeLessonList, this::openLessonDetail);
        rvStudentFreeLessons.setAdapter(freeLessonAdapter);

        // Click Listeners
        tvAchievementCollapseToggle.setOnClickListener(v -> toggleAchievementView());
        tvLessonsCollapseToggle.setOnClickListener(v -> toggleLessonsView());
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

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void toggleLessonsView() {
        isLessonsExpanded = !isLessonsExpanded;

        if (isLessonsExpanded) {
            rvStudentFreeLessons.setVisibility(View.VISIBLE);
            tvLessonsCollapseToggle.setText("Collapse");
        } else {
            rvStudentFreeLessons.setVisibility(View.GONE);
            tvLessonsCollapseToggle.setText("View All");
        }
    }

    private void toggleAchievementView() {
        isAchievementExpanded = !isAchievementExpanded;

        int limit = isAchievementExpanded ? achievementList.size() : INITIAL_ITEM_LIMIT;
        achievementAdapter.setLimit(limit);
        achievementAdapter.notifyDataSetChanged();

        tvAchievementCollapseToggle.setText(isAchievementExpanded ? "Collapse" : "View All");
    }

    // ====================================================================
    // DATA LOADING
    // ====================================================================

    private void loadStudentProfile() {
        // --- CRITICAL FIX: USE profileUserId INSTEAD OF CurrentUser ---
        if (profileUserId == null) return;

        loadParticipatedLessons(profileUserId);

        FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("student_profiles")
                .child(profileUserId) // Use the variable!
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (!task.isSuccessful() || task.getResult() == null) return;

                    StudentProfile profile = task.getResult().getValue(StudentProfile.class);
                    if (profile == null) return;

                    tvStudentName.setText(profile.getUsername());
                    tvStudentAge.setText("Age: " + profile.getAge());
                    tvStudentAcademic.setText(profile.getAcademicLevel());
                    tvStudentDescription.setText(profile.getDescription());
                    tvStudentEmail.setText("Email: " + profile.getEmail());
                    tvStudentContact.setText("Contact: " + profile.getContact());

                    int resId = AvatarManager.getAvatarResourceId(profile.getProfileImageUrl());
                    if (resId != 0) imgStudentProfilePic.setImageResource(resId);

                    // --- ACHIEVEMENTS ---
                    achievementList.clear();
                    if (profile.getAchievements() != null) {
                        achievementList.addAll(profile.getAchievements());
                    }

                    isAchievementExpanded = false;
                    achievementAdapter.setLimit(INITIAL_ITEM_LIMIT);
                    achievementAdapter.notifyDataSetChanged();

                    // VISIBILITY CHECK
                    if (achievementList.isEmpty()) {
                        rvAchievements.setVisibility(View.GONE);
                        tvNoAchievements.setVisibility(View.VISIBLE);
                        tvAchievementCollapseToggle.setVisibility(View.GONE);
                    } else {
                        rvAchievements.setVisibility(View.VISIBLE);
                        tvNoAchievements.setVisibility(View.GONE);

                        if (achievementList.size() > INITIAL_ITEM_LIMIT) {
                            tvAchievementCollapseToggle.setVisibility(View.VISIBLE);
                            tvAchievementCollapseToggle.setText("View All");
                        } else {
                            tvAchievementCollapseToggle.setVisibility(View.GONE);
                        }
                    }

                    // --- SHOW COUNT ---
                    tvStudentAchievementsTitle.setText("Achievements (" + achievementList.size() + ")");
                });
    }

    private void loadParticipatedLessons(String uidToLoad) {
        FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("lesson_participation")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        List<String> ids = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            // Check if the specific user (uidToLoad) has participated and completed
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
        freeLessonAdapter.notifyDataSetChanged();

        // --- SHOW COUNT ---
        tvStudentFreeLessonsTitle.setText("Participated Free Lessons (" + freeLessonList.size() + ")");

        if (freeLessonList.isEmpty()) {
            rvStudentFreeLessons.setVisibility(View.GONE);
            tvLessonsCollapseToggle.setVisibility(View.GONE);
            tvNoLessons.setVisibility(View.VISIBLE);
        } else {
            tvNoLessons.setVisibility(View.GONE);
            rvStudentFreeLessons.setVisibility(View.VISIBLE);
            tvLessonsCollapseToggle.setVisibility(View.VISIBLE);
            tvLessonsCollapseToggle.setText("Collapse");
            isLessonsExpanded = true;
        }
    }
    private void showLanguageDialog() {
        String[] languages = {"English", "Bahasa Melayu"};

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.language_option))
                .setSingleChoiceItems(languages, -1, (dialog, which) -> {
                    if (which == 0) {
                        setLocale("en");
                    } else if (which == 1) {
                        setLocale("ms");
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void setLocale(String langCode) {
        java.util.Locale locale = new java.util.Locale(langCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);

        requireActivity().getResources().updateConfiguration(config,
                requireActivity().getResources().getDisplayMetrics());

        // Save language choice in SharedPreferences
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE);
        prefs.edit().putString("My_Lang", langCode).apply();

        // RESTART HomeActivity to apply changes everywhere
        android.content.Intent intent = requireActivity().getIntent();
        requireActivity().finish();
        startActivity(intent);
    }
}