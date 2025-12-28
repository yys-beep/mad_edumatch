package com.example.mad_edumatch.tutor;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.FreeLesson;
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

public class TutorHomeFragment extends Fragment {

    // Main Navigation Views
    private View cardSearchStudent, cardPostListing, cardViewListings, cardUploadLesson;
    private TextView tvTutorWelcome, tvNoLessons;
    private ImageView imgHomeAvatar;

    // Lessons List Views
    private RecyclerView rvFreeLessons;
    private EditText etSearchLesson;
    private Button btnViewMore;

    // Data Management
    private FreeLessonAdapter freeLessonAdapter;
    private ArrayList<FreeLesson> allFreeLessons = new ArrayList<>();
    private ArrayList<FreeLesson> displayList = new ArrayList<>();
    private DatabaseReference freeLessonsRef;

    // Pagination & Search State
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

        // Load data on startup
        loadUserInfo();
        loadFreeLessons();
        loadDashboard(view); // Initialize the modular Impact Dashboard

        return view;
    }

    private void bindViews(View view) {
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
    }

    /**
     * Delegates all Impact Score and Badge logic to the ImpactManager.
     */
    private void loadDashboard(View rootView) {
        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        // Force a recalculation of the contribution score
        GamificationHelper.calculateScore(uid);

        // Find the included layout and bind it via the Manager
        View impactView = rootView.findViewById(R.id.layoutDashboard);
        if (impactView != null) {
            ImpactManager.bindImpact(impactView, uid, this);
        }
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
                currentLimit = LOAD_STEP; // Reset pagination on new search
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

        // Toggle empty state visibility
        tvNoLessons.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);
        rvFreeLessons.setVisibility(displayList.isEmpty() ? View.GONE : View.VISIBLE);

        // Show "View More" button only if there is more data to load
        btnViewMore.setVisibility(end < total ? View.VISIBLE : View.GONE);
    }

    private void loadFreeLessons() {
        freeLessonsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                allFreeLessons.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FreeLesson lesson = ds.getValue(FreeLesson.class);
                    if (lesson != null) {
                        lesson.setLessonId(ds.getKey());
                        allFreeLessons.add(lesson);
                    }
                }
                Collections.reverse(allFreeLessons); // Show newest lessons first
                filterAndDisplayList();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
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
        if (uid == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_profiles").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                String realName = snapshot.child("username").getValue(String.class);
                if (realName == null) realName = "Tutor";

                tvTutorWelcome.setText(getString(R.string.welcome_back_comma) + "\n" + realName + " !");

                String avatarName = snapshot.child("profileImageUrl").getValue(String.class);
                int resId = AvatarManager.getAvatarResourceId(avatarName);
                if(resId != 0) imgHomeAvatar.setImageResource(resId);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}