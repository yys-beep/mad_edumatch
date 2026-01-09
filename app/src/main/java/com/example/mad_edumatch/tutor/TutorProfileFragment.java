package com.example.mad_edumatch.tutor;

import android.app.AlertDialog; 
import android.content.Context; 
import android.content.Intent;  
import android.content.SharedPreferences; 
import android.content.res.Configuration; 
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

import com.example.mad_edumatch.HomeActivity; // Import HomeActivity for restart
import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.FreeLesson;
import com.example.mad_edumatch.firebaseModels.TutorProfile;
import com.example.mad_edumatch.freeLesson.FreeLessonDetailFragment;
import com.example.mad_edumatch.helper.AvatarManager;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.helper.GamificationHelper;
import com.example.mad_edumatch.recycleAdapters.AchievementAdapter;
import com.example.mad_edumatch.recycleAdapters.FreeLessonAdapter;

import com.google.android.material.button.MaterialButton; // Import MaterialButton
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

public class TutorProfileFragment extends Fragment {

    private ImageView imgProfile;
    private TextView tvName, tvQual, tvDesc, tvSubjects, tvFee, tvArea, tvContact, tvEmail;
    private Button btnEdit;

    // 1. ADD: New Button Variable for Language
    private MaterialButton btnLanguage;

    private TextView tvHeaderLessons, tvHeaderExperience, tvHeaderAchievements;
    private TextView tvLessonCollapseToggle, tvExperienceCollapseToggle, tvAchievementsCollapseToggle;
    private TextView tvNoExperience, tvNoAchievements;

    private RecyclerView rvLessons, rvExperience, rvAchievements;
    private FreeLessonAdapter freeLessonAdapter;
    private AchievementAdapter experienceAdapter, achievementAdapter;

    private ArrayList<FreeLesson> hostedLessonList = new ArrayList<>();
    private ArrayList<String> experienceList = new ArrayList<>();
    private ArrayList<String> achievementList = new ArrayList<>();

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

        // 2. MODIFIED: Control visibility of Edit AND Language buttons
        // Only show these buttons if the user is viewing their OWN profile
        if (profileUserId != null && profileUserId.equals(CurrentUser.getInstance().getUid())) {
            btnEdit.setVisibility(View.VISIBLE);
            btnLanguage.setVisibility(View.VISIBLE);

            btnEdit.setOnClickListener(v -> getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TutorEditProfileFragment())
                    .addToBackStack(null).commit());

            // Set Language Click Listener
            btnLanguage.setOnClickListener(v -> showChangeLanguageDialog());
        } else {
            btnEdit.setVisibility(View.GONE);
            btnLanguage.setVisibility(View.GONE); // Hide for visitors
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

        // 3. BIND: Find the new button ID from XML
        btnLanguage = view.findViewById(R.id.btnChangeLanguageTutor);

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

    // 4. ADD: Method to show Language Selection Dialog
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

    // 5. ADD: Method to save preference and update config
    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);

        if (getActivity() != null) {
            getActivity().getResources().updateConfiguration(config, getActivity().getResources().getDisplayMetrics());

            // Save to Shared Preferences "Settings" -> "My_Lang"
            SharedPreferences.Editor editor = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE).edit();
            editor.putString("My_Lang", lang);
            editor.apply();
        }
    }

    // 6. ADD: Method to Restart Activity and return to Profile
    private void restartApp() {
        if (getActivity() == null) return;

        Intent intent = new Intent(getActivity(), HomeActivity.class);

        // A. Pass User Data back to HomeActivity so it doesn't show default info
        // Since we are in TutorProfileFragment, the role is definitely "Tutor"
        intent.putExtra("userRole", "Tutor");

        // Get the name from the current TextView
        String currentName = tvName.getText().toString();
        intent.putExtra("userName", currentName);

        // B. Tell HomeActivity to open this Profile page directly
        intent.putExtra("TARGET_FRAGMENT", "PROFILE");

        // C. Clear the stack so the user cannot "back" into the old language
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        getActivity().finish();
    }

    // ... (The rest of your existing methods remain unchanged) ...

    private void setupRecyclerViews() {
        rvLessons.setLayoutManager(new LinearLayoutManager(getContext()));
        freeLessonAdapter = new FreeLessonAdapter(hostedLessonList, this::openLessonDetail);
        freeLessonAdapter.setLimit(INITIAL_ITEM_LIMIT);
        rvLessons.setAdapter(freeLessonAdapter);
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
        ImpactManager.bindImpact(impactView, profileUserId, this);
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

                    // Using String Formatting for Labels
                    tvContact.setText(getString(R.string.contact_label, getSafeDisplayString(profile.getContact())));
                    tvArea.setText(getString(R.string.area_label, getSafeDisplayString(profile.getArea())));
                    tvEmail.setText(getString(R.string.email_label, getSafeDisplayString(profile.getEmail())));

                    String fee = getSafeDisplayString(profile.getFee());
                    if (fee.equals("N/A")) {
                        tvFee.setText(getString(R.string.fee_na));
                    } else {
                        tvFee.setText(getString(R.string.fee_format, fee));
                    }

                    String subjectsText = (profile.getSubjects() != null && !profile.getSubjects().isEmpty())
                            ? TextUtils.join(", ", profile.getSubjects()) : "N/A";
                    tvSubjects.setText(getString(R.string.subjects_label, subjectsText));

                    // Experience Section
                    experienceList.clear();
                    if (profile.getExperience() != null) experienceList.addAll(profile.getExperience());

                    // Format: "Experience (3)" or "Pengalaman (3)"
                    tvHeaderExperience.setText(getString(R.string.experience_header, experienceList.size()));

                    refreshSectionUI(experienceList.size(), rvExperience, tvNoExperience, tvExperienceCollapseToggle, experienceAdapter, isExperienceExpanded);

                    // Achievements Section
                    achievementList.clear();
                    if (profile.getAchievement() != null) achievementList.addAll(profile.getAchievement());

                    // Format: "Achievements (5)" or "Pencapaian (5)"
                    tvHeaderAchievements.setText(getString(R.string.achievements_header, achievementList.size()));

                    refreshSectionUI(achievementList.size(), rvAchievements, tvNoAchievements, tvAchievementsCollapseToggle, achievementAdapter, isAchievementsExpanded);

                    int resId = AvatarManager.getAvatarResourceId(profile.getProfileImageUrl());
                    imgProfile.setImageResource(resId != 0 ? resId : R.drawable.avatar_1);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void refreshSectionUI(int size, RecyclerView rv, TextView emptyTv, TextView toggle, AchievementAdapter adapter, boolean isExpanded) {
        if (size == 0) {
            rv.setVisibility(View.GONE);
            emptyTv.setVisibility(View.VISIBLE);
            toggle.setVisibility(View.GONE);
        } else {
            emptyTv.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE); // Always visible if we have data

            if (size > INITIAL_ITEM_LIMIT) {
                toggle.setVisibility(View.VISIBLE);

                if (isExpanded) {
                    // EXPANDED: Show All Items
                    toggle.setText(getString(R.string.collapse));
                    adapter.setLimit(size);
                } else {
                    // COLLAPSED: Show only 3 Items (This keeps the list "up")
                    toggle.setText(getString(R.string.view_all));
                    adapter.setLimit(INITIAL_ITEM_LIMIT);
                }
            } else {
                // If 3 or fewer items, show all and hide the button
                toggle.setVisibility(View.GONE);
                adapter.setLimit(size);
            }
            adapter.notifyDataSetChanged();
        }
    }

    // Helper specifically for Lessons
    private void refreshLessonUI() {
        int size = hostedLessonList.size();
        tvHeaderLessons.setText(getString(R.string.hosted_lessons_header, size));

        if (size == 0) {
            rvLessons.setVisibility(View.GONE);
            tvLessonCollapseToggle.setVisibility(View.GONE);
        } else {
            rvLessons.setVisibility(View.VISIBLE); // Always visible if data exists

            if (size > INITIAL_ITEM_LIMIT) {
                tvLessonCollapseToggle.setVisibility(View.VISIBLE);

                if (isLessonsExpanded) {
                    // EXPANDED: Show All
                    tvLessonCollapseToggle.setText(getString(R.string.collapse));
                    freeLessonAdapter.setLimit(0); // 0 means "Show All" in your adapter logic
                } else {
                    // COLLAPSED: Show only 3
                    tvLessonCollapseToggle.setText(getString(R.string.view_all));
                    freeLessonAdapter.setLimit(INITIAL_ITEM_LIMIT);
                }
            } else {
                tvLessonCollapseToggle.setVisibility(View.GONE);
                freeLessonAdapter.setLimit(0);
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
