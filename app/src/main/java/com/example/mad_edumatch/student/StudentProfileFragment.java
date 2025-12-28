package com.example.mad_edumatch.student;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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

import com.example.mad_edumatch.HomeActivity;
import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.FreeLesson;
import com.example.mad_edumatch.firebaseModels.StudentProfile;
import com.example.mad_edumatch.freeLesson.FreeLessonDetailFragment;
import com.example.mad_edumatch.helper.AvatarManager;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.recycleAdapters.AchievementAdapter;
import com.example.mad_edumatch.recycleAdapters.FreeLessonAdapter;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentProfileFragment extends Fragment {

    // Views
    private TextView tvStudentName, tvStudentAge, tvStudentAcademic, tvStudentDescription, tvStudentEmail, tvStudentContact;
    private TextView tvStudentAchievementsTitle, tvStudentFreeLessonsTitle;
    private RecyclerView rvAchievements, rvStudentFreeLessons;
    private Button btnEditProfile;
    private ImageView imgStudentProfilePic;
    private MaterialButton btnLanguage;

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
    private View rootView;

    // --- FIX: Store References to remove them later ---
    private DatabaseReference userRef;
    private ValueEventListener userListener;
    private DatabaseReference participationRef;
    private ValueEventListener participationListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (rootView == null) {
            rootView = inflater.inflate(R.layout.student_fragment_profile, container, false);

            bindViews(rootView);

            if (getArguments() != null && getArguments().getString("targetUserId") != null) {
                profileUserId = getArguments().getString("targetUserId");
            } else {
                profileUserId = CurrentUser.getInstance().getUid();
            }

            setupRecyclerViews();

            if (profileUserId != null && profileUserId.equals(CurrentUser.getInstance().getUid())) {
                btnEditProfile.setVisibility(View.VISIBLE);
                btnLanguage.setVisibility(View.VISIBLE);

                btnEditProfile.setOnClickListener(v -> {
                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new StudentEditProfileFragment())
                            .addToBackStack(null)
                            .commit();
                });

                btnLanguage.setOnClickListener(v -> showChangeLanguageDialog());

            } else {
                btnEditProfile.setVisibility(View.GONE);
                btnLanguage.setVisibility(View.GONE);
            }

            // Load data only ONCE when view is created
            loadStudentProfile();
        }

        return rootView;
    }

    // --- FIX: REMOVED onResume() method ---
    // Do NOT call loadStudentProfile() in onResume, or it will duplicate listeners.

    // --- FIX: Remove Listeners on Destroy ---
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
        if (participationRef != null && participationListener != null) {
            participationRef.removeEventListener(participationListener);
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
        btnLanguage = view.findViewById(R.id.btnChangeLanguageStudent);

        rvAchievements = view.findViewById(R.id.rvStudentAchievements);
        rvStudentFreeLessons = view.findViewById(R.id.rvStudentFreeLessons);

        tvAchievementCollapseToggle = view.findViewById(R.id.tvAchievementCollapseToggle);
        tvLessonsCollapseToggle = view.findViewById(R.id.tvLessonsCollapseToggle);

        tvNoAchievements = view.findViewById(R.id.tvNoAchievements);
        tvNoLessons = view.findViewById(R.id.tvNoLessons);

        tvStudentName.setText("");
        tvStudentAge.setText("");
        tvStudentAcademic.setText("");
        tvStudentDescription.setText("");
        tvStudentEmail.setText("");
        tvStudentContact.setText("");

        tvNoAchievements.setVisibility(View.GONE);
        tvNoLessons.setVisibility(View.GONE);
    }

    private void showChangeLanguageDialog() {
        final String[] listItems = {"English", "Bahasa Malaysia", "简体中文"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.language_option));
        builder.setSingleChoiceItems(listItems, -1, (dialog, i) -> {
            if (i == 0) {
                setLocale("en");
                dialog.dismiss();
                restartApp();
            } else if (i == 1) {
                setLocale("ms");
                dialog.dismiss();
                restartApp();
            }else if (i == 2) {
                setLocale("zh");
                dialog.dismiss();
                restartApp();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);

        if (getActivity() != null) {
            getActivity().getResources().updateConfiguration(config, getActivity().getResources().getDisplayMetrics());
            SharedPreferences.Editor editor = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE).edit();
            editor.putString("My_Lang", lang);
            editor.apply();
        }
    }

    private void restartApp() {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), HomeActivity.class);
        intent.putExtra("userRole", "Student");
        String currentName = tvStudentName.getText().toString();
        intent.putExtra("userName", currentName);
        intent.putExtra("TARGET_FRAGMENT", "PROFILE");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
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

    private void refreshLessonUI() {
        if (!isAdded()) return;
        int size = freeLessonList.size();
        tvStudentFreeLessonsTitle.setText(getString(R.string.participated_free_lessons) + " (" + size + ")");

        if (size == 0) {
            rvStudentFreeLessons.setVisibility(View.GONE);
            tvLessonsCollapseToggle.setVisibility(View.GONE);
            tvNoLessons.setVisibility(View.VISIBLE);
        } else {
            rvStudentFreeLessons.setVisibility(View.VISIBLE);
            tvNoLessons.setVisibility(View.GONE);

            if (size > INITIAL_ITEM_LIMIT) {
                tvLessonsCollapseToggle.setVisibility(View.VISIBLE);
                tvLessonsCollapseToggle.setText(isLessonsExpanded ? getString(R.string.collapse) : getString(R.string.view_all));
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
        tvAchievementCollapseToggle.setText(isAchievementExpanded ? getString(R.string.collapse) : getString(R.string.view_all));
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

        userRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("student_profiles")
                .child(profileUserId);

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                StudentProfile profile = snapshot.getValue(StudentProfile.class);
                if (profile == null) return;

                tvStudentName.setText(profile.getUsername());
                tvStudentAge.setText(getString(R.string.age) + ": " + profile.getAge());
                tvStudentAcademic.setText(profile.getAcademicLevel());
                tvStudentDescription.setText(profile.getDescription());
                tvStudentEmail.setText(getString(R.string.email) + ": " + profile.getEmail());
                tvStudentContact.setText(getString(R.string.profile_contact) + " " + profile.getContact());

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
                        tvAchievementCollapseToggle.setText(isAchievementExpanded ? getString(R.string.collapse) : getString(R.string.view_all));
                        achievementAdapter.setLimit(isAchievementExpanded ? achievementList.size() : INITIAL_ITEM_LIMIT);
                    } else {
                        tvAchievementCollapseToggle.setVisibility(View.GONE);
                        achievementAdapter.setLimit(INITIAL_ITEM_LIMIT);
                    }
                }
                achievementAdapter.notifyDataSetChanged();
                tvStudentAchievementsTitle.setText(getString(R.string.achievements) + " (" + achievementList.size() + ")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        userRef.addValueEventListener(userListener);
    }

    private void loadParticipatedLessons(String uidToLoad) {
        participationRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("lesson_participation");

        participationListener = new ValueEventListener() {
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
        };
        participationRef.addValueEventListener(participationListener);
    }

    private void fetchLessonDetails(List<String> ids) {
        // Clear list immediately to prevent stacking if this method is called multiple times quickly
        freeLessonList.clear();

        if (ids.isEmpty()) {
            finalizeLessonLoad();
            return;
        }

        final int[] count = {0};
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("free_lessons");

        for (String id : ids) {
            ref.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    FreeLesson lesson = snapshot.getValue(FreeLesson.class);
                    if (lesson != null) {
                        lesson.setLessonId(id);
                        // Prevent duplicates explicitly
                        boolean exists = false;
                        for(FreeLesson fl : freeLessonList) {
                            if(fl.getLessonId().equals(id)) {
                                exists = true;
                                break;
                            }
                        }
                        if(!exists) {
                            freeLessonList.add(lesson);
                        }
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