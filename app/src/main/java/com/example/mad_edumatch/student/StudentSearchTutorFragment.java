package com.example.mad_edumatch.student;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.TutorListing;
import com.example.mad_edumatch.helper.ListingDataHelper; // Import New Helper
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
    private List<TutorListing> allTutorList = new ArrayList<>();
    private List<TutorListing> displayedTutorList = new ArrayList<>();
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
        adapter = new StudentSearchTutorAdapter(displayedTutorList);
        rvSearchTutors.setAdapter(adapter);

        dbRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_listings");

        loadAllData();
        setupListeners();
    }

    private void loadAllData() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTutorList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    TutorListing tutor = data.getValue(TutorListing.class);
                    if (tutor != null) {
                        tutor.setKey(data.getKey());
                        allTutorList.add(tutor);
                    }
                }
                performFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), R.string.error_loading_data, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupListeners() {
        etSearchSubject.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> performFilter());
    }

    private void performFilter() {
        String query = etSearchSubject.getText() != null ? etSearchSubject.getText().toString().trim().toLowerCase() : "";

        // --- STEP 1: Get the KEY instead of the TEXT ---
        String selectedKey = null;
        int checkedChipId = chipGroupFilter.getCheckedChipId();

        if (checkedChipId != View.NO_ID) {
            View chip = chipGroupFilter.findViewById(checkedChipId);
            // Get the index of the selected chip (0, 1, 2...)
            int index = chipGroupFilter.indexOfChild(chip);

            // Map the index to the correct DB Key (e.g., Index 1 -> "LOWER_SEC")
            if (index >= 0 && index < ListingDataHelper.LEVEL_KEYS.length) {
                selectedKey = ListingDataHelper.LEVEL_KEYS[index];
            }
        }

        displayedTutorList.clear();

        for (TutorListing tutor : allTutorList) {

            // 1. Search Text Match
            boolean matchSearch = false;
            if (query.isEmpty()) {
                matchSearch = true;
            } else {
                String subject = tutor.getSubject() != null ? tutor.getSubject().toLowerCase() : "";
                String name = tutor.getName() != null ? tutor.getName().toLowerCase() : "";
                if (subject.contains(query) || name.contains(query)) {
                    matchSearch = true;
                }
            }

            // 2. Level Match (Key Comparison)
            boolean matchLevel = false;
            if (selectedKey == null) {
                matchLevel = true; // No filter selected
            } else {
                // Check if the tutor's list of keys contains our selected KEY
                // e.g. Does [PRIMARY, LOWER_SEC] contain "LOWER_SEC"?
                if (tutor.getAcademicLevels() != null && tutor.getAcademicLevels().contains(selectedKey)) {
                    matchLevel = true;
                }
                // Fallback for old data (if stored as string)
                else if (tutor.getLevelsAsString() != null) {
                    // This is less reliable but keeps old data working
                    if (tutor.getLevelsAsString().contains(selectedKey)) {
                        matchLevel = true;
                    }
                }
            }

            if (matchSearch && matchLevel) {
                displayedTutorList.add(tutor);
            }
        }

        // Sort Newest First
        Collections.sort(displayedTutorList, (o1, o2) ->
                Long.compare(o2.getTimestamp(), o1.getTimestamp())
        );

        updateUI();
    }

    private void updateUI() {
        if (displayedTutorList.isEmpty()) {
            tvNoTutorFound.setVisibility(View.VISIBLE);
            rvSearchTutors.setVisibility(View.GONE);
        } else {
            tvNoTutorFound.setVisibility(View.GONE);
            rvSearchTutors.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }
}