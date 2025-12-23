package com.example.mad_edumatch. tutor;

import android.os.Bundle;
import android.text.TextUtils;
import android. util.Log;
import android. view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view. Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android. widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.FreeLesson;
import com. example.mad_edumatch. firebaseModels.TutorProfile;
import com.example. mad_edumatch.freeLesson.FreeLessonDetailFragment;
import com.example.mad_edumatch.helper.AvatarManager;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.helper.GamificationHelper;
import com.example.mad_edumatch.recycleAdapters.AchievementAdapter;
import com.example.mad_edumatch.recycleAdapters.FreeLessonAdapter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database. DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase. database.FirebaseDatabase;
import com.google.firebase.database. ValueEventListener;

import java. util.ArrayList;
import java.util.Map;


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

    private TextView tvDashViews, tvDashHelped, tvDashScore;
    private ProgressBar pbContribution;
    private LinearLayout layoutBadgeContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tutor_fragment_profile, container, false);

        bindViews(view);
        setupRecyclerViews();

        // Trigger Score Calculation for Dashboard
        if (profileUserId != null) {
            GamificationHelper.calculateScore(profileUserId);
        }

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

        loadHostedLessons();
        loadProfile();
        loadDashboardData(); // Add this line

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

        tvDashViews = view.findViewById(R.id.tvDashViews);
        tvDashHelped = view.findViewById(R.id.tvDashHelped);
        tvDashScore = view.findViewById(R.id. tvDashScore);
        pbContribution = view.findViewById(R. id.pbContribution);
        layoutBadgeContainer = view.findViewById(R.id.layoutBadgeContainer);

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

    private void loadDashboardData() {
        if (profileUserId == null) return;

        // Read from tutor_profiles (same as StudentViewTutorProfileFragment)
        DatabaseReference profileRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("tutor_profiles")
                .child(profileUserId);

        profileRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (! isAdded()) return;

                // Use the same field names as in TutorProfile model
                int views = 0;
                int helped = 0;
                int score = 0;

                // Read totalViews
                if (snapshot. hasChild("totalViews")) {
                    Object viewsObj = snapshot.child("totalViews").getValue();
                    if (viewsObj instanceof Long) {
                        views = ((Long) viewsObj).intValue();
                    } else if (viewsObj instanceof Integer) {
                        views = (Integer) viewsObj;
                    }
                }

                // Read studentsHelped
                if (snapshot.hasChild("studentsHelped")) {
                    Object helpedObj = snapshot.child("studentsHelped").getValue();
                    if (helpedObj instanceof Long) {
                        helped = ((Long) helpedObj).intValue();
                    } else if (helpedObj instanceof Integer) {
                        helped = (Integer) helpedObj;
                    }
                }

                // Read contributionScore
                if (snapshot.hasChild("contributionScore")) {
                    Object scoreObj = snapshot.child("contributionScore").getValue();
                    if (scoreObj instanceof Long) {
                        score = ((Long) scoreObj).intValue();
                    } else if (scoreObj instanceof Integer) {
                        score = (Integer) scoreObj;
                    }
                }

                // Update UI
                tvDashViews.setText("👀 Views: " + views);
                tvDashHelped.setText("🎓 Helped:  " + helped);
                tvDashScore.setText(score + "/100");
                pbContribution. setProgress(score);

                // Load badges
                if (snapshot. hasChild("badges")) {
                    try {
                        Map<String, Object> badgesObj = (Map<String, Object>) snapshot.child("badges").getValue();
                        if (badgesObj != null) {
                            // Convert to Map<String, Boolean>
                            Map<String, Boolean> badges = new java.util.HashMap<>();
                            for (Map.Entry<String, Object> entry : badgesObj. entrySet()) {
                                if (entry.getValue() instanceof Boolean) {
                                    badges.put(entry.getKey(), (Boolean) entry.getValue());
                                }
                            }
                            renderBadges(badges);
                        }
                    } catch (Exception e) {
                        Log. e("TutorProfile", "Error loading badges:  " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TutorProfile", "Failed to load dashboard: " + error. getMessage());
            }
        });
    }

    private void renderBadges(Map<String, Boolean> badges) {
        layoutBadgeContainer.removeAllViews();

        for (Map.Entry<String, Boolean> entry : badges.entrySet()) {
            if (entry.getValue()) {
                String key = entry.getKey();
                String emoji = "";

                switch (key) {
                    case "firstLesson":  emoji = "🎉"; break;
                    case "fiveLessons": emoji = "🔥"; break;
                    case "tenLessons": emoji = "⭐"; break;
                    case "helpedTen": emoji = "💪"; break;
                    case "helpedFifty": emoji = "🏆"; break;
                    case "scoreAbove50": emoji = "🎖️"; break;
                    default: emoji = "🏅"; break;
                }

                addBadgeIcon(key, emoji);
            }
        }
    }

    private void addBadgeIcon(String badgeKey, String iconEmoji) {
        TextView badge = new TextView(getContext());
        badge.setText(iconEmoji);
        badge.setTextSize(28);
        badge.setPadding(12, 12, 12, 12);
        badge.setOnClickListener(v -> showBadgeInfo(badgeKey));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        badge.setLayoutParams(params);

        layoutBadgeContainer.addView(badge);
    }

    private void showBadgeInfo(String key) {
        String title = "";
        String message = "";

        switch (key) {
            case "firstLesson":
                title = "🎉 First Lesson Badge";
                message = "Congratulations on uploading your first free lesson!";
                break;
            case "fiveLessons":
                title = "🔥 Five Lessons Badge";
                message = "You've uploaded 5 free lessons! Keep it up!";
                break;
            case "tenLessons":
                title = "⭐ Ten Lessons Badge";
                message = "Amazing!  You've reached 10 free lessons!";
                break;
            case "helpedTen":
                title = "💪 Helped Ten Badge";
                message = "You've helped 10 students.  Great work!";
                break;
            case "helpedFifty":
                title = "🏆 Helped Fifty Badge";
                message = "Incredible! You've helped 50 students! ";
                break;
            case "scoreAbove50":
                title = "🎖️ Top Contributor Badge";
                message = "Your contribution score is above 50! ";
                break;
            default:
                title = "🏅 Badge Earned";
                message = "You've earned this achievement!";
                break;
        }

        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }


}