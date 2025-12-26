package com.example.mad_edumatch.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.TutorListing;
import com.example.mad_edumatch.recycleAdapters.StudentSearchTutorAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentSearchTutorFragment extends Fragment {

    private TextInputEditText etSearchSubject;
    private ChipGroup chipGroupFilter;
    private RecyclerView rvSearchTutors;
    private TextView tvNoTutorFound;

    private StudentSearchTutorAdapter adapter;
    private List<TutorListing> tutorList = new ArrayList<>();
    private DatabaseReference dbRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.student_fragment_search_tutor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearchSubject = view.findViewById(R.id.etSearchSubject);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        rvSearchTutors = view.findViewById(R.id.rvSearchTutors);
        tvNoTutorFound = view.findViewById(R.id.tvNoTutorFound);

        rvSearchTutors.setLayoutManager(new LinearLayoutManager(requireContext()));
        dbRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("tutor_listings");

        // --- FIX 1: Restore the Adapter immediately if it exists ---
        if (adapter != null) {
            rvSearchTutors.setAdapter(adapter);
        } else {
            // Initialize empty adapter so the list isn't null
            adapter = new StudentSearchTutorAdapter(tutorList);
            rvSearchTutors.setAdapter(adapter);
        }

        // Listeners
        etSearchSubject.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            performSearch();
        });

        // Always perform search to ensure data is fresh
        performSearch();
    }

    // --- FIX 2: Ensure data refreshes when returning ---
    @Override
    public void onResume() {
        super.onResume();
        if (tutorList.isEmpty()) {
            performSearch();
        }
    }

    private void performSearch() {
        String query = etSearchSubject.getText() != null ? etSearchSubject.getText().toString().trim().toLowerCase() : "";

        String selectedLevel = "";
        int checkedChipId = chipGroupFilter.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip chip = chipGroupFilter.findViewById(checkedChipId);
            selectedLevel = chip.getText().toString().toLowerCase();
        }

        String finalLevel = selectedLevel;

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tutorList.clear();
                List<ScoredTutor> scoredList = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    TutorListing tutor = data.getValue(TutorListing.class);
                    if (tutor == null) continue;

                    tutor.setKey(data.getKey());

                    int score = 0;
                    String tSubject = tutor.getSubject() != null ? tutor.getSubject().toLowerCase() : ""; // Use Getter

                    // A. Subject Match
                    if (query.isEmpty()) {
                        score += 10;
                    } else if (tSubject.equals(query)) {
                        score += 50;
                    } else if (tSubject.contains(query)) {
                        score += 30;
                    }

                    // B. Level Filter
                    if (!finalLevel.isEmpty()) {
                        boolean levelMatch = false;

                        if (tutor.getAcademicLevels() != null) {
                            for (String dbLevel : tutor.getAcademicLevels()) {
                                // Check if they match, IGNORING Upper/Lower case differences
                                if (dbLevel.equalsIgnoreCase(finalLevel)) {
                                    levelMatch = true;
                                    break;
                                }
                            }
                        }

                        if (levelMatch) {
                            score += 20;
                        } else {
                            score = 0; // Strict filter: if level doesn't match, hide it.
                        }
                    }

                    if (score > 0) {
                        scoredList.add(new ScoredTutor(tutor, score));
                    }
                }

                Collections.sort(scoredList, (o1, o2) -> {
                    // 1. First priority: Filter Score (Subject Match)
                    int filterComparison = Integer.compare(o2.score, o1.score);
                    if (filterComparison != 0) return filterComparison;

                    // 2. Second priority: Contribution Score (Reputation)
                    return Integer.compare(o2.tutor.contributionScore, o1.tutor.contributionScore);
                });

                for (ScoredTutor st : scoredList) {
                    tutorList.add(st.tutor);
                }

                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUI() {
        if (tutorList.isEmpty()) {
            tvNoTutorFound.setVisibility(View.VISIBLE);
            rvSearchTutors.setVisibility(View.GONE);
        } else {
            tvNoTutorFound.setVisibility(View.GONE);
            rvSearchTutors.setVisibility(View.VISIBLE);

            // --- FIX 3: Safety check ---
            // If adapter became null or detached, re-attach it
            if (rvSearchTutors.getAdapter() == null) {
                rvSearchTutors.setAdapter(adapter);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private static class ScoredTutor {
        TutorListing tutor;
        int score;
        ScoredTutor(TutorListing tutor, int score) { this.tutor = tutor; this.score = score; }
    }
}