package com.example.mad_edumatch.student;

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
import android.widget.Toast;

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
import com.example.mad_edumatch.recycleAdapters.FreeLessonAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentHomeFragment extends Fragment {

    // Views
    private View cardSearchTutor, cardPostRequest, cardViewRequests;
    private TextView tvStudentWelcome, tvNoLessons;
    private ImageView imgHomeAvatar;
    private RecyclerView rvFreeLessons;
    private EditText etSearchLesson;
    private Button btnViewMore;

    // Data
    private FreeLessonAdapter freeLessonAdapter;
    private ArrayList<FreeLesson> allFreeLessons = new ArrayList<>(); // Store everything from DB
    private ArrayList<FreeLesson> displayList = new ArrayList<>();    // Store what is currently shown
    private DatabaseReference freeLessonsRef;

    // Pagination & Search State
    private int currentLimit = 10;
    private static final int LOAD_STEP = 10;
    private String currentSearchText = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.student_fragment_home, container, false);

        bindViews(view);
        setupRecyclerView();
        setupClickListeners();
        setupSearchListener();

        loadUserInfo();
        loadFreeLessons();

        return view;
    }

    private void bindViews(View view) {
        tvStudentWelcome = view.findViewById(R.id.tvStudentWelcome);
        imgHomeAvatar = view.findViewById(R.id.imgHomeAvatar);
        cardSearchTutor = view.findViewById(R.id.cardSearchTutor);
        cardPostRequest = view.findViewById(R.id.cardPostRequest);
        cardViewRequests = view.findViewById(R.id.cardViewRequests);
        rvFreeLessons = view.findViewById(R.id.rvFreeLessons);

        // New Views
        etSearchLesson = view.findViewById(R.id.etSearchLesson);
        tvNoLessons = view.findViewById(R.id.tvNoLessons);
        btnViewMore = view.findViewById(R.id.btnViewMore);
    }

    private void setupRecyclerView() {
        freeLessonAdapter = new FreeLessonAdapter(displayList, this::openLessonDetail);
        rvFreeLessons.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFreeLessons.setAdapter(freeLessonAdapter);
        // Important for nested scrolling
        rvFreeLessons.setNestedScrollingEnabled(false);

        freeLessonsRef = FirebaseDatabase.getInstance(
                "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference("free_lessons");
    }

    private void setupClickListeners() {
        cardSearchTutor.setOnClickListener(v -> navigateToFragment(new StudentSearchTutorFragment()));
        cardPostRequest.setOnClickListener(v -> navigateToFragment(new StudentPostRequestFragment()));
        cardViewRequests.setOnClickListener(v -> navigateToFragment(new StudentViewRequestsFragment()));
        imgHomeAvatar.setOnClickListener(v -> navigateToFragment(new StudentProfileFragment()));

        // VIEW MORE LOGIC
        btnViewMore.setOnClickListener(v -> {
            currentLimit += LOAD_STEP;
            filterAndDisplayList();
        });
    }

    private void setupSearchListener() {
        etSearchLesson.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString().toLowerCase().trim();
                currentLimit = LOAD_STEP; // Reset pagination on search
                filterAndDisplayList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // --- CORE LOGIC: Filter + Pagination ---
    private void filterAndDisplayList() {
        if (!isAdded()) return;

        ArrayList<FreeLesson> filtered = new ArrayList<>();

        // 1. Filter logic
        if (currentSearchText.isEmpty()) {
            filtered.addAll(allFreeLessons);
        } else {
            for (FreeLesson lesson : allFreeLessons) {
                if (lesson.getTitle() != null && lesson.getTitle().toLowerCase().contains(currentSearchText)) {
                    filtered.add(lesson);
                }
            }
        }

        // 2. Pagination Logic
        int totalFilteredCount = filtered.size();
        int end = Math.min(currentLimit, totalFilteredCount);

        displayList.clear();
        if (totalFilteredCount > 0) {
            displayList.addAll(filtered.subList(0, end));
        }

        // 3. UI Updates
        freeLessonAdapter.notifyDataSetChanged();

        // Toggle "No Lessons" message
        if (displayList.isEmpty()) {
            tvNoLessons.setVisibility(View.VISIBLE);
            rvFreeLessons.setVisibility(View.GONE);
        } else {
            tvNoLessons.setVisibility(View.GONE);
            rvFreeLessons.setVisibility(View.VISIBLE);
        }

        // Toggle "View More" button
        if (end < totalFilteredCount) {
            btnViewMore.setVisibility(View.VISIBLE);
        } else {
            btnViewMore.setVisibility(View.GONE);
        }
    }

    private void loadFreeLessons() {
        freeLessonsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allFreeLessons.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FreeLesson lesson = ds.getValue(FreeLesson.class);
                    if (lesson != null) {
                        allFreeLessons.add(lesson);
                    }
                }
                Collections.reverse(allFreeLessons); // Newest first
                filterAndDisplayList(); // Apply initial display logic
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ... loadUserInfo and navigateToFragment remain the same ...

    private void loadUserInfo() {
        String uid = CurrentUser.getInstance().getUid();
        if (uid != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("student_profiles")
                    .child(uid);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) return;
                    String realName = "Student";
                    if (snapshot.hasChild("username")) {
                        realName = snapshot.child("username").getValue(String.class);
                    }
                    tvStudentWelcome.setText("Welcome back,\n" + realName + " !");

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
}