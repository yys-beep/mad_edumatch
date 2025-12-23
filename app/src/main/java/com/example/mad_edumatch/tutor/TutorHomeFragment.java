package com.example.mad_edumatch.tutor;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.mad_edumatch.recycleAdapters.FreeLessonAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class TutorHomeFragment extends Fragment {

    // Views
    private View cardSearchStudent, cardPostListing, cardViewListings, cardUploadLesson;
    private TextView tvTutorWelcome, tvNoLessons;
    private ImageView imgHomeAvatar;
    private RecyclerView rvFreeLessons;
    private EditText etSearchLesson;
    private Button btnViewMore;

    // Dashboard Views
    private TextView tvDashViews, tvDashHelped, tvDashScore;
    private ProgressBar pbContribution;
    private LinearLayout layoutBadgeContainer; // New container for badges

    // Data
    private FreeLessonAdapter freeLessonAdapter;
    private ArrayList<FreeLesson> allFreeLessons = new ArrayList<>();
    private ArrayList<FreeLesson> displayList = new ArrayList<>();
    private DatabaseReference freeLessonsRef;

    // Pagination & Search
    private int currentLimit = 10;
    private static final int LOAD_STEP = 10;
    private String currentSearchText = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tutor_fragment_home, container, false);

        bindViews(view);
        setupRecyclerView();
        setupClickListeners();
        setupSearchListener();

        loadUserInfo();
        loadFreeLessons();
        loadDashboard(); // Loads the gamification stats

        return view;
    }

    private void bindViews(View view) {
        // Basic Views
        tvTutorWelcome = view.findViewById(R.id.tvTutorWelcome);
        imgHomeAvatar = view.findViewById(R.id.imgHomeAvatar);

        cardSearchStudent = view.findViewById(R.id.cardSearchStudent);
        cardPostListing = view.findViewById(R.id.cardPostListing);
        cardViewListings = view.findViewById(R.id.cardViewListings);
        cardUploadLesson = view.findViewById(R.id.cardUploadLesson);

        rvFreeLessons = view.findViewById(R.id.rvFreeLessons);
        etSearchLesson = view.findViewById(R.id.etSearchLesson);
        tvNoLessons = view.findViewById(R.id.tvNoLessons);
        btnViewMore = view.findViewById(R.id.btnViewMore);

        // Dashboard Views
        tvDashViews = view.findViewById(R.id.tvDashViews);
        tvDashHelped = view.findViewById(R.id.tvDashHelped);
        tvDashScore = view.findViewById(R.id.tvDashScore);
        pbContribution = view.findViewById(R.id.pbContribution);
        layoutBadgeContainer = view.findViewById(R.id.layoutBadgeContainer); // Bind the new container
    }

    private void setupRecyclerView() {
        freeLessonAdapter = new FreeLessonAdapter(displayList, this::openLessonDetail);
        rvFreeLessons.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFreeLessons.setAdapter(freeLessonAdapter);
        rvFreeLessons.setNestedScrollingEnabled(false);

        freeLessonsRef = FirebaseDatabase.getInstance(
                "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference("free_lessons");
    }

    private void setupClickListeners() {
        cardSearchStudent.setOnClickListener(v -> navigateToFragment(new TutorSearchStudentRequestsFragment()));
        cardPostListing.setOnClickListener(v -> navigateToFragment(new TutorPostListingFragment()));
        cardViewListings.setOnClickListener(v -> navigateToFragment(new TutorViewListingFragment()));
        cardUploadLesson.setOnClickListener(v -> navigateToFragment(new TutorUploadFreeLessonFragment()));
        imgHomeAvatar.setOnClickListener(v -> navigateToFragment(new TutorProfileFragment()));

        btnViewMore.setOnClickListener(v -> {
            currentLimit += LOAD_STEP;
            filterAndDisplayList();
        });
    }

    private void setupSearchListener() {
        etSearchLesson.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString().toLowerCase().trim();
                currentLimit = LOAD_STEP;
                filterAndDisplayList();
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterAndDisplayList() {
        if (!isAdded()) return;

        ArrayList<FreeLesson> filtered = new ArrayList<>();
        if (currentSearchText.isEmpty()) {
            filtered.addAll(allFreeLessons);
        } else {
            for (FreeLesson lesson : allFreeLessons) {
                if (lesson.getTitle() != null && lesson.getTitle().toLowerCase().contains(currentSearchText)) {
                    filtered.add(lesson);
                }
            }
        }

        int total = filtered.size();
        int end = Math.min(currentLimit, total);

        displayList.clear();
        if (total > 0) {
            displayList.addAll(filtered.subList(0, end));
        }
        freeLessonAdapter.notifyDataSetChanged();

        if (displayList.isEmpty()) {
            tvNoLessons.setVisibility(View.VISIBLE);
            rvFreeLessons.setVisibility(View.GONE);
        } else {
            tvNoLessons.setVisibility(View.GONE);
            rvFreeLessons.setVisibility(View.VISIBLE);
        }

        btnViewMore.setVisibility(end < total ? View.VISIBLE : View.GONE);
    }

    private void loadFreeLessons() {
        freeLessonsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allFreeLessons.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FreeLesson lesson = ds.getValue(FreeLesson.class);
                    if (lesson != null) allFreeLessons.add(lesson);
                }
                Collections.reverse(allFreeLessons);
                filterAndDisplayList();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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
        args.putLong("duration", lesson.getDurationMinutes());
        fragment.setArguments(args);
        navigateToFragment(fragment);
    }

    private void navigateToFragment(Fragment fragment) {
        if (isAdded() && getActivity() != null) {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void loadUserInfo() {
        String uid = CurrentUser.getInstance().getUid();
        if (uid != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("tutor_profiles")
                    .child(uid);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) return;
                    String realName = "Tutor";
                    if (snapshot.hasChild("username")) {
                        realName = snapshot.child("username").getValue(String.class);
                    }
                    tvTutorWelcome.setText("Welcome back,\n" + realName + " !");
                    if (snapshot.hasChild("profileImageUrl")) {
                        String avatarName = snapshot.child("profileImageUrl").getValue(String.class);
                        int resId = AvatarManager.getAvatarResourceId(avatarName);
                        if(resId != 0) imgHomeAvatar.setImageResource(resId);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    // --- NEW: Load Dashboard Stats & Badges ---
    private void loadDashboard() {
        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        // Recalculate score every time home loads
        GamificationHelper.calculateScore(uid);

        DatabaseReference ref = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_profiles").child(uid);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                TutorProfile p = snapshot.getValue(TutorProfile.class);
                if (p != null) {
                    tvDashViews.setText("👀 Total Views: " + p.getTotalViews());
                    tvDashHelped.setText("🎓 Students Helped: " + p.getStudentsHelped());
                    tvDashScore.setText(p.getContributionScore() + "/100");
                    pbContribution.setProgress(p.getContributionScore());

                    // Render Badges Dynamically
                    renderBadges(p.getBadges());
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void renderBadges(Map<String, Boolean> badges) {
        layoutBadgeContainer.removeAllViews(); // Clear previous badges

        if (badges == null) return;

        // 1. TIER BADGES (Always show one)
        if (badges.containsKey("tier_top")) addBadgeIcon("tier_top", "🏆");
        else if (badges.containsKey("tier_gold")) addBadgeIcon("tier_gold", "🥇");
        else if (badges.containsKey("tier_silver")) addBadgeIcon("tier_silver", "🥈");
        else addBadgeIcon("tier_bronze", "🥉"); // Default

        // 2. ACHIEVEMENT BADGES
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
        tv.setTextSize(26); // Large Icon
        tv.setPadding(24, 16, 24, 16);
        tv.setBackgroundResource(R.drawable.circle_bg_light); // Ensure you created this drawable!
        tv.setGravity(Gravity.CENTER);

        // Add margin between badges
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 16, 0);
        tv.setLayoutParams(params);

        // Click Listener -> Show Dialog
        tv.setOnClickListener(v -> showBadgeInfo(badgeKey));

        layoutBadgeContainer.addView(tv);
    }

    private void showBadgeInfo(String key) {
        String title = "";
        String desc = "";
        String howTo = "";
        String icon = "";

        switch (key) {
            case "tier_bronze":
                icon = "🥉"; title = "New Tutor"; desc = "Just started."; howTo = "Contributions score: 0 - 20"; break;
            case "tier_silver":
                icon = "🥈"; title = "Active Contributor"; desc = "Regular participation."; howTo = "Contributions score: 21 - 50"; break;
            case "tier_gold":
                icon = "🥇"; title = "High Impact"; desc = "Very helpful community member."; howTo = "Contributions score: 51 - 80"; break;
            case "tier_top":
                icon = "🏆"; title = "Top Rated Educator"; desc = "The elite top 5% of tutors."; howTo = "Contributions score: 81 - 100"; break;
            case "ach_starter":
                icon = "📹"; title = "Lesson Starter"; desc = "3 lessons uploaded."; howTo = "Upload 3 Free Lessons."; break;
            case "ach_favorite":
                icon = "❤️"; title = "Crowd Favorite"; desc = "Receive 50 Total Likes on lessons."; howTo = "Receive 50 Total Likes on lessons."; break;
            case "ach_viral":
                icon = "🚀"; title = "Viral Educator"; desc = "Achieve 500 Total Views."; howTo = "Achieve 500 Total Views across all lessons."; break;
            case "ach_helper":
                icon = "✋"; title = "Helper Hand"; desc = "Post 3 Answers in Q&A."; howTo = "Post 3 Answers in Q&A."; break;
            case "ach_solver":
                icon = "🧠"; title = "Problem Solver"; desc = "Post 20 Answers in Q&A."; howTo = "Post 20 Answers in Q&A."; break;
            case "ach_expert":
                icon = "✨"; title = "Verified Expert"; desc = "Get 10 Upvotes on answers."; howTo = "Get 10 'Upvotes/Likes' on answers."; break;
        }

        new AlertDialog.Builder(getContext())
                .setTitle(icon + " " + title)
                .setMessage(desc + "\n\n💡 Description:\n" + howTo)
                .setPositiveButton("Awesome!", null)
                .show();
    }
}