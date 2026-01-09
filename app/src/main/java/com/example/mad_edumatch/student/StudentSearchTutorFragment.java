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
import com.example.mad_edumatch.helper.LocalizationHelper; // Import Localization Helper
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

    // LIST A: Holds all data from Firebase (The "Master" list)
    private List<TutorListing> allTutorList = new ArrayList<>();
    // LIST B: Holds only what matches your search (The "Display" list)
    private List<TutorListing> displayedTutorList = new ArrayList<>();

    private DatabaseReference dbRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.student_fragment_search_tutor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1.Initialize Views
        etSearchSubject = view.findViewById(R.id.etSearchSubject);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        rvSearchTutors = view.findViewById(R.id.rvSearchTutors);
        tvNoTutorFound = view.findViewById(R.id.tvNoTutorFound);

        // 2.Setup RecyclerView
        rvSearchTutors.setLayoutManager(new LinearLayoutManager(requireContext()));
        // Initialize adapter with the display list
        adapter = new StudentSearchTutorAdapter(displayedTutorList);
        rvSearchTutors.setAdapter(adapter);

        // 3.Setup Firebase
        dbRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_listings");

        // 4.Load Data (ONCE)
        loadAllData();

        // 5.Setup Listeners
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
                // Once data is loaded, run the filter immediately
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
        // Listener 1: Search Bar (TextWatcher for instant search)
        etSearchSubject.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performFilter(); // Filter as you type
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Listener 2: Chips
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            performFilter(); // Filter when chip changes
        });
    }

    // --- KEY LOGIC IS HERE ---
    private void performFilter() {
        String query = etSearchSubject.getText() != null ? etSearchSubject.getText().toString().trim().toLowerCase() : "";

        // Get selected Chip Text (This might be in Malay or English depending on app language)
        String selectedLevel = "";
        int checkedChipId = chipGroupFilter.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip chip = chipGroupFilter.findViewById(checkedChipId);
            selectedLevel = chip.getText().toString().toLowerCase(); // e.g., "primary" or "rendah"
        }

        // Clear previous results
        displayedTutorList.clear();

        // Loop through the MASTER list
        for (TutorListing tutor : allTutorList) {

            // 1. CHECK SEARCH TEXT (Subject OR Name)
            boolean matchSearch = false;
            if (query.isEmpty()) {
                matchSearch = true; // If search is empty, everything matches
            } else {
                String subject = tutor.getSubject() != null ? tutor.getSubject().toLowerCase() : "";
                String name = tutor.getName() != null ? tutor.getName().toLowerCase() : "";

                if (subject.contains(query) || name.contains(query)) {
                    matchSearch = true;
                }
            }

            // 2. CHECK LEVEL (Chip) - With Localization Support
            boolean matchLevel = false;
            if (selectedLevel.isEmpty()) {
                matchLevel = true; // If no chip selected, everything matches
            } else {
                // Check against the tutor's list of levels
                if (tutor.getAcademicLevels() != null) {
                    for (String dbLevel : tutor.getAcademicLevels()) {
                        // CRITICAL: Translate DB value (e.g. "Primary") to Current Locale (e.g. "Rendah")
                        // so it matches the Chip text.
                        String localizedDbLevel = dbLevel;
                        int resId = LocalizationHelper.getLevelStringId(dbLevel);
                        if (resId != 0 && isAdded()) {
                            localizedDbLevel = getString(resId);
                        }

                        if (localizedDbLevel.toLowerCase().contains(selectedLevel)) {
                            matchLevel = true;
                            break;
                        }
                    }
                }
                // Fallback: If levels are stored as a single comma-separated string
                else if (tutor.getLevelsAsString() != null) {
                    // Try to translate the whole string or check raw
                    if (tutor.getLevelsAsString().toLowerCase().contains(selectedLevel)) {
                        matchLevel = true;
                    }
                }
            }

            // 3. STRICT "AND" LOGIC: Both must be true
            if (matchSearch && matchLevel) {
                displayedTutorList.add(tutor);
            }
        }

        // 4. SORT (Newest first)
        Collections.sort(displayedTutorList, (o1, o2) ->
                Long.compare(o2.getTimestamp(), o1.getTimestamp())
        );

        // 5. UPDATE UI
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
