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
// NOTE: You must uncomment and ensure these adapters and the setLimit() method exist.
// import com.example.mad_edumatch.recycleAdapters.StringListAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TutorProfileFragment extends Fragment {

    // Views
    private ImageView imgProfile;
    private TextView tvName, tvQual, tvDesc, tvSubjects, tvFee, tvArea, tvContact, tvEmail;
    private Button btnEdit;

    // Hosted Lessons Views
    private RecyclerView rvLessons;
    private TextView tvLessonCollapseToggle; // NEW FIELD

    // Experience Views
    private RecyclerView rvExperience; // NEW FIELD
    private TextView tvExperienceCollapseToggle; // NEW FIELD
    private TextView tvNoExperience; // NEW FIELD

    // Achievements Views
    private RecyclerView rvAchievements; // NEW FIELD
    private TextView tvAchievementsCollapseToggle; // NEW FIELD
    private TextView tvNoAchievements; // NEW FIELD

    // Data
    private ArrayList<FreeLesson> hostedLessonList;
    private List<String> experienceList = new ArrayList<>(); // Initialized List
    private List<String> achievementList = new ArrayList<>(); // Initialized List

    // Adapters (Placeholder names - replace with your actual adapter types)
    private FreeLessonAdapter freeLessonAdapter;
    private AchievementAdapter experienceAdapter; // USE AchievementAdapter for Experience!
    private AchievementAdapter achievementAdapter;

    // State & Constants
    private boolean isLessonsExpanded = true;
    private boolean isExperienceExpanded = false;
    private boolean isAchievementsExpanded = false;
    private static final int INITIAL_ITEM_LIMIT = 3;
    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tutor_fragment_profile, container, false);

        bindViews(view);
        setupRecyclerViews();

        loadHostedLessons();
        loadProfile();

        btnEdit.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TutorEditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void bindViews(View view) {
        // Core Profile Views (Existing)
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

        // Hosted Lessons Views (Modified/New)
        rvLessons = view.findViewById(R.id.rvTutorHostedLessons);
        tvLessonCollapseToggle = view.findViewById(R.id.tvLessonCollapseToggle); // NEW BINDING

        // Experience Views (New Bindings)
        rvExperience = view.findViewById(R.id.rvTutorExperience);
        tvExperienceCollapseToggle = view.findViewById(R.id.tvExperienceCollapseToggle);
        tvNoExperience = view.findViewById(R.id.tvNoExperience);

        // Achievements Views (New Bindings)
        rvAchievements = view.findViewById(R.id.rvTutorAchievements);
        tvAchievementsCollapseToggle = view.findViewById(R.id.tvAchievementsCollapseToggle);
        tvNoAchievements = view.findViewById(R.id.tvNoAchievements);
    }

    private void setupRecyclerViews() {
        // 1. Hosted Lessons Setup
        hostedLessonList = new ArrayList<>();
        rvLessons.setLayoutManager(new LinearLayoutManager(getContext()));
        freeLessonAdapter = new FreeLessonAdapter(hostedLessonList, this::openLessonDetail);
        rvLessons.setAdapter(freeLessonAdapter);

        // 2. Experience Setup
        // experienceList is initialized as empty
         experienceAdapter = new AchievementAdapter(experienceList);
         rvExperience.setLayoutManager(new LinearLayoutManager(getContext()));
         experienceAdapter.setLimit(INITIAL_ITEM_LIMIT);
         rvExperience.setAdapter(experienceAdapter);

        // 3. Achievements Setup (Uses AchievementAdapter)
        achievementAdapter = new AchievementAdapter(achievementList);
        rvAchievements.setLayoutManager(new LinearLayoutManager(getContext()));
        achievementAdapter.setLimit(INITIAL_ITEM_LIMIT);
        rvAchievements.setAdapter(achievementAdapter);

        // Set up Toggles (Existing Logic)
        if (tvLessonCollapseToggle != null) tvLessonCollapseToggle.setOnClickListener(v -> toggleLessonView());
        if (tvExperienceCollapseToggle != null) tvExperienceCollapseToggle.setOnClickListener(v -> toggleExperienceView());
        if (tvAchievementsCollapseToggle != null) tvAchievementsCollapseToggle.setOnClickListener(v -> toggleAchievementView());
    }

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

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // --- TOGGLE LOGIC FOR ALL THREE LISTS ---

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
        experienceAdapter.setLimit(limit); // Now works!
        experienceAdapter.notifyDataSetChanged();
        tvExperienceCollapseToggle.setText(isExperienceExpanded ? "Collapse" : "View All");
    }

    private void toggleAchievementView() {
        isAchievementsExpanded = !isAchievementsExpanded;
        int limit = isAchievementsExpanded ? achievementList.size() : INITIAL_ITEM_LIMIT;
        achievementAdapter.setLimit(limit); // Now works!
        achievementAdapter.notifyDataSetChanged();
        tvAchievementsCollapseToggle.setText(isAchievementsExpanded ? "Collapse" : "View All");
    }

    // --- DATA LOADING & VISIBILITY MANAGEMENT ---

    private void loadHostedLessons() {
        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("free_lessons");

        ref.orderByChild("tutorId").equalTo(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!isAdded()) return;

                List<FreeLesson> tempLessons = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        FreeLesson lesson = ds.getValue(FreeLesson.class);
                        if (lesson != null) {
                            lesson.setLessonId(ds.getKey());
                            tempLessons.add(lesson);
                        }
                    } catch (Exception e) {
                        Log.e("HostedLessons", "TYPE MISMATCH ERROR at key: " + ds.getKey(), e);
                    }
                }

                hostedLessonList.clear();
                hostedLessonList.addAll(tempLessons);
                freeLessonAdapter.notifyDataSetChanged();

                boolean isEmpty = hostedLessonList.isEmpty();

                // Manage visibility of the entire block (header, toggle, and RV)
                TextView tvHeaderLessons = getView() != null ? getView().findViewById(R.id.tvHeaderLessons) : null;
                View lessonHeaderContainer = (View) tvLessonCollapseToggle.getParent();

                if (isEmpty) {
                    // If empty, hide everything
                    if (lessonHeaderContainer != null) lessonHeaderContainer.setVisibility(View.GONE);
                    rvLessons.setVisibility(View.GONE);
                } else {
                    // If not empty, ensure the header block is visible
                    if (lessonHeaderContainer != null) lessonHeaderContainer.setVisibility(View.VISIBLE);

                    // Set initial state to Expanded/Visible
                    isLessonsExpanded = true;
                    rvLessons.setVisibility(View.VISIBLE);
                    tvLessonCollapseToggle.setText("Collapse");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Load hosted lessons cancelled: " + error.getMessage());
            }
        });
    }

    private void loadProfile() {
        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("tutor_profiles")
                .child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                TutorProfile profile = snapshot.getValue(TutorProfile.class);

                if (profile != null) {
                    // 1. Basic Fields and Subject List Formatting
                    tvName.setText(getSafeDisplayString(profile.getUsername()));
                    tvQual.setText(getSafeDisplayString(profile.getQualification()));
                    tvDesc.setText(getSafeDisplayString(profile.getDescription()));
                    tvContact.setText("Contact: " + getSafeDisplayString(profile.getContact()));
                    tvArea.setText("Area: " + getSafeDisplayString(profile.getArea()));
                    tvEmail.setText("Email: " + getSafeDisplayString(profile.getEmail()));

                    String fee = getSafeDisplayString(profile.getFee());
                    tvFee.setText(fee.equals("N/A") ? "Hourly Fee: N/A" : "Hourly Fee: RM " + fee + "/hr");

                    // Subject List Formatting
                    String subjectsText = "N/A";
                    if (profile.getSubjects() != null && !profile.getSubjects().isEmpty()) {
                        subjectsText = TextUtils.join(", ", profile.getSubjects());
                    }
                    tvSubjects.setText("Subjects: " + subjectsText);

                    // 2. Load and Manage Experience List
                    experienceList.clear();
                    if (profile.getExperience() != null) experienceList.addAll(profile.getExperience());

                    isExperienceExpanded = false;
                    // if (experienceAdapter != null) experienceAdapter.setLimit(INITIAL_ITEM_LIMIT);
                    // if (experienceAdapter != null) experienceAdapter.notifyDataSetChanged();
                    manageListVisibility(experienceList.size(), rvExperience, tvNoExperience, tvExperienceCollapseToggle, "Experience");

                    // 3. Load and Manage Achievement List
                    achievementList.clear();
                    if (profile.getAchievement() != null) achievementList.addAll(profile.getAchievement());

                    isAchievementsExpanded = false;
                    // if (achievementAdapter != null) achievementAdapter.setLimit(INITIAL_ITEM_LIMIT);
                    // if (achievementAdapter != null) achievementAdapter.notifyDataSetChanged();
                    manageListVisibility(achievementList.size(), rvAchievements, tvNoAchievements, tvAchievementsCollapseToggle, "Achievement");

                    // 4. Avatar
                    String avatarName = profile.getProfileImageUrl();
                    int resId = AvatarManager.getAvatarResourceId(avatarName);
                    if (resId != 0) imgProfile.setImageResource(resId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- UNIVERSAL HELPERS ---

    private String getSafeDisplayString(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("-") || value.equals("N/A")) {
            return "N/A";
        }
        return value.trim();
    }

    /**
     * Manages the visibility of the RecyclerView, the "No Items" message, and the toggle button.
     */
    private void manageListVisibility(int listSize, RecyclerView rv, TextView tvNoItems, TextView tvToggle, String listType) {
        if (listSize == 0) {
            rv.setVisibility(View.GONE);
            tvNoItems.setVisibility(View.VISIBLE);
            tvNoItems.setText("No " + listType.toLowerCase() + " recorded.");
            tvToggle.setVisibility(View.GONE);
        } else {
            rv.setVisibility(View.VISIBLE);
            tvNoItems.setVisibility(View.GONE);

            if (listSize > INITIAL_ITEM_LIMIT) {
                tvToggle.setVisibility(View.VISIBLE);
                tvToggle.setText("View All"); // Always start in collapsed state
            } else {
                tvToggle.setVisibility(View.GONE);
            }
        }
    }
}