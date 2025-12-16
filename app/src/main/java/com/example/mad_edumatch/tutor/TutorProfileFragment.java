package com.example.mad_edumatch.tutor;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.example.mad_edumatch.recycleAdapters.AchievementAdapter;
import com.example.mad_edumatch.recycleAdapters.FreeLessonAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class TutorProfileFragment extends Fragment {

    // Views
    private ImageView imgProfile;
    private TextView tvName, tvQual, tvDesc, tvSubjects, tvFee, tvArea, tvContact, tvEmail;
    private Button btnEdit;

    // Headers (Use these to show counts!)
    private TextView tvHeaderLessons, tvHeaderExperience, tvHeaderAchievements;

    // Toggle Buttons
    private TextView tvLessonCollapseToggle, tvExperienceCollapseToggle, tvAchievementsCollapseToggle;

    // Empty Messages
    private TextView tvNoExperience, tvNoAchievements;
    private View lessonHeaderContainer; // To hide lesson header if empty

    // RecyclerViews
    private RecyclerView rvLessons, rvExperience, rvAchievements;

    // Adapters
    private FreeLessonAdapter freeLessonAdapter;
    private AchievementAdapter experienceAdapter;
    private AchievementAdapter achievementAdapter;

    // Data
    private ArrayList<FreeLesson> hostedLessonList = new ArrayList<>();
    private ArrayList<String> experienceList = new ArrayList<>();
    private ArrayList<String> achievementList = new ArrayList<>();

    // State
    private boolean isLessonsExpanded = true;
    private boolean isExperienceExpanded = false;
    private boolean isAchievementsExpanded = false;
    private static final int INITIAL_ITEM_LIMIT = 3;
    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    // ID Variable
    private String profileUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tutor_fragment_profile, container, false);

        bindViews(view);
        setupRecyclerViews();

        // --- DETERMINE USER ID ---
        if (getArguments() != null && getArguments().getString("targetUserId") != null) {
            // Viewing someone else
            profileUserId = getArguments().getString("targetUserId");
            btnEdit.setVisibility(View.GONE); // Hide Edit button
        } else {
            // Viewing myself
            profileUserId = CurrentUser.getInstance().getUid();
            btnEdit.setVisibility(View.VISIBLE); // Show Edit button
        }

        btnEdit.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TutorEditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        loadHostedLessons();
        loadProfile();

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

        // Bind Headers for Counts
        tvHeaderLessons = view.findViewById(R.id.tvHeaderLessons);
        tvHeaderExperience = view.findViewById(R.id.tvHeaderExperience);
        tvHeaderAchievements = view.findViewById(R.id.tvHeaderAchievements);

        // Lists & Toggles
        rvLessons = view.findViewById(R.id.rvTutorHostedLessons);
        tvLessonCollapseToggle = view.findViewById(R.id.tvLessonCollapseToggle);
        if (tvLessonCollapseToggle.getParent() instanceof View) {
            lessonHeaderContainer = (View) tvLessonCollapseToggle.getParent();
        }

        rvExperience = view.findViewById(R.id.rvTutorExperience);
        tvExperienceCollapseToggle = view.findViewById(R.id.tvExperienceCollapseToggle);
        tvNoExperience = view.findViewById(R.id.tvNoExperience);

        rvAchievements = view.findViewById(R.id.rvTutorAchievements);
        tvAchievementsCollapseToggle = view.findViewById(R.id.tvAchievementsCollapseToggle);
        tvNoAchievements = view.findViewById(R.id.tvNoAchievements);
    }

    private void setupRecyclerViews() {
        // Lessons
        rvLessons.setLayoutManager(new LinearLayoutManager(getContext()));
        freeLessonAdapter = new FreeLessonAdapter(hostedLessonList, this::openLessonDetail);
        rvLessons.setAdapter(freeLessonAdapter);

        // Experience
        rvExperience.setLayoutManager(new LinearLayoutManager(getContext()));
        experienceAdapter = new AchievementAdapter(experienceList);
        experienceAdapter.setLimit(INITIAL_ITEM_LIMIT);
        rvExperience.setAdapter(experienceAdapter);

        // Achievements
        rvAchievements.setLayoutManager(new LinearLayoutManager(getContext()));
        achievementAdapter = new AchievementAdapter(achievementList);
        achievementAdapter.setLimit(INITIAL_ITEM_LIMIT);
        rvAchievements.setAdapter(achievementAdapter);

        tvLessonCollapseToggle.setOnClickListener(v -> toggleLessonView());
        tvExperienceCollapseToggle.setOnClickListener(v -> toggleExperienceView());
        tvAchievementsCollapseToggle.setOnClickListener(v -> toggleAchievementView());
    }

    private void toggleLessonView() {
        isLessonsExpanded = !isLessonsExpanded;
        if (isLessonsExpanded) {
            rvLessons.setVisibility(View.VISIBLE);
            tvLessonCollapseToggle.setText("Collapse");
        } else {
            rvLessons.setVisibility(View.GONE);
            tvLessonCollapseToggle.setText("View All");
        }
    }

    private void toggleExperienceView() {
        isExperienceExpanded = !isExperienceExpanded;
        int limit = isExperienceExpanded ? experienceList.size() : INITIAL_ITEM_LIMIT;
        experienceAdapter.setLimit(limit);
        experienceAdapter.notifyDataSetChanged();
        tvExperienceCollapseToggle.setText(isExperienceExpanded ? "Collapse" : "View All");
    }

    private void toggleAchievementView() {
        isAchievementsExpanded = !isAchievementsExpanded;
        int limit = isAchievementsExpanded ? achievementList.size() : INITIAL_ITEM_LIMIT;
        achievementAdapter.setLimit(limit);
        achievementAdapter.notifyDataSetChanged();
        tvAchievementsCollapseToggle.setText(isAchievementsExpanded ? "Collapse" : "View All");
    }

    private void loadHostedLessons() {
        if (profileUserId == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("free_lessons");

        // Query by the determined profileUserId
        ref.orderByChild("tutorId").equalTo(profileUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!isAdded()) return;

                hostedLessonList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        FreeLesson lesson = ds.getValue(FreeLesson.class);
                        if (lesson != null) {
                            lesson.setLessonId(ds.getKey());
                            hostedLessonList.add(lesson);
                        }
                    } catch (Exception e) {}
                }
                freeLessonAdapter.notifyDataSetChanged();

                // --- SHOW COUNT ---
                tvHeaderLessons.setText("Hosted Free Lessons (" + hostedLessonList.size() + ")");

                if (hostedLessonList.isEmpty()) {
                    if (lessonHeaderContainer != null) lessonHeaderContainer.setVisibility(View.GONE);
                    rvLessons.setVisibility(View.GONE);
                } else {
                    if (lessonHeaderContainer != null) lessonHeaderContainer.setVisibility(View.VISIBLE);
                    rvLessons.setVisibility(View.VISIBLE);
                    tvLessonCollapseToggle.setText("Collapse");
                    isLessonsExpanded = true;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadProfile() {
        if (profileUserId == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("tutor_profiles")
                .child(profileUserId); // Use the variable

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

                    String subjectsText = "N/A";
                    if (profile.getSubjects() != null && !profile.getSubjects().isEmpty()) {
                        subjectsText = TextUtils.join(", ", profile.getSubjects());
                    }
                    tvSubjects.setText("Subjects: " + subjectsText);

                    // Experience
                    experienceList.clear();
                    if (profile.getExperience() != null) experienceList.addAll(profile.getExperience());

                    // --- SHOW COUNT ---
                    tvHeaderExperience.setText("Experience (" + experienceList.size() + ")");

                    experienceAdapter.setLimit(INITIAL_ITEM_LIMIT);
                    experienceAdapter.notifyDataSetChanged();
                    manageListVisibility(experienceList.size(), rvExperience, tvNoExperience, tvExperienceCollapseToggle);

                    // Achievements
                    achievementList.clear();
                    if (profile.getAchievement() != null) achievementList.addAll(profile.getAchievement());

                    // --- SHOW COUNT ---
                    tvHeaderAchievements.setText("Achievements (" + achievementList.size() + ")");

                    achievementAdapter.setLimit(INITIAL_ITEM_LIMIT);
                    achievementAdapter.notifyDataSetChanged();
                    manageListVisibility(achievementList.size(), rvAchievements, tvNoAchievements, tvAchievementsCollapseToggle);

                    // Avatar
                    int resId = AvatarManager.getAvatarResourceId(profile.getProfileImageUrl());
                    imgProfile.setImageResource(resId != 0 ? resId : R.drawable.avatar_1);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getSafeDisplayString(String value) {
        return (value == null || value.trim().isEmpty() || value.equals("-") || value.equals("N/A")) ? "N/A" : value.trim();
    }

    private void manageListVisibility(int listSize, RecyclerView rv, TextView tvNoItems, TextView tvToggle) {
        if (listSize == 0) {
            rv.setVisibility(View.GONE);
            tvNoItems.setVisibility(View.VISIBLE);
            tvToggle.setVisibility(View.GONE);
        } else {
            rv.setVisibility(View.VISIBLE);
            tvNoItems.setVisibility(View.GONE);
            if (listSize > INITIAL_ITEM_LIMIT) {
                tvToggle.setVisibility(View.VISIBLE);
                tvToggle.setText("View All");
            } else {
                tvToggle.setVisibility(View.GONE);
            }
        }
    }

    // Open detail method
    private void openLessonDetail(FreeLesson lesson) {
        FreeLessonDetailFragment fragment = new FreeLessonDetailFragment();
        Bundle args = new Bundle();
        args.putString("lessonId", lesson.getLessonId());
        args.putString("tutorId", lesson.getTutorId());
        args.putString("title", lesson.getTitle());
        args.putString("desc", lesson.getDescription());
        args.putString("videoUrl", lesson.getVideoLink());
        args.putString("matUrl", lesson.getMaterialUrl());
        args.putString("matName", lesson.getMaterialName());
        fragment.setArguments(args);
        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
    }
}