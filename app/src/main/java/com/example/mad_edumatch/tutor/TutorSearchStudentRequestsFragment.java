package com.example.mad_edumatch.tutor;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.StudentRequest;
import com.example.mad_edumatch.recycleAdapters.TutorStudentRequestAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TutorSearchStudentRequestsFragment extends Fragment {

    private TextInputEditText etSearchSubject;
    private ChipGroup chipGroupFilter;
    private RecyclerView recyclerView;
    private TextView tvNoRequestFound;

    private TutorStudentRequestAdapter adapter;
    private List<StudentRequest> fullRequestList;
    private List<StudentRequest> displayList;

    private DatabaseReference databaseRef;

    // 1. Variable to hold the listener (Prevents crash on logout)
    private ValueEventListener requestsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tutor_fragment_search_student_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        etSearchSubject = view.findViewById(R.id.etSearchSubject);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        recyclerView = view.findViewById(R.id.rvStudentRequests);
        tvNoRequestFound = view.findViewById(R.id.tvNoRequestFound);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        fullRequestList = new ArrayList<>();
        displayList = new ArrayList<>();

        adapter = new TutorStudentRequestAdapter(displayList);
        recyclerView.setAdapter(adapter);

        // Setup Firebase
        databaseRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_requests");

        // Load data
        loadAllRequests();

        // Setup Listeners
        setupSearchListeners();
    }

    private void loadAllRequests() {
        // 2. Assign listener to variable
        requestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullRequestList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    StudentRequest request = data.getValue(StudentRequest.class);
                    if (request != null) {
                        request.setRequestId(data.getKey());
                        fullRequestList.add(request);
                    }
                }

                // --- 3. FIX: SORT BY TIMESTAMP (Newest First) ---
                Collections.sort(fullRequestList, (r1, r2) ->
                        Long.compare(r2.getTimestamp(), r1.getTimestamp())
                );
                // ------------------------------------------------

                // Refresh the search view with the new data
                performSearch();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Prevent crash if auth is null
                if (FirebaseAuth.getInstance().getCurrentUser() != null && getContext() != null) {
                    Toast.makeText(getContext(), "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        databaseRef.addValueEventListener(requestsListener);
    }

    // 4. Cleanup listener to prevent crash on Logout
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseRef != null && requestsListener != null) {
            databaseRef.removeEventListener(requestsListener);
        }
    }

    private void setupSearchListeners() {
        // A. Real-time Text Search (Better than EditorActionListener)
        etSearchSubject.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // B. Chip Selection
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            performSearch();
        });
    }

    private void performSearch() {
        String query = etSearchSubject.getText() != null ? etSearchSubject.getText().toString().trim().toLowerCase() : "";

        String selectedLevel = "";
        int checkedChipId = chipGroupFilter.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip chip = chipGroupFilter.findViewById(checkedChipId);
            // This gets "Rendah" if app is in Malay, or "Primary" if English
            selectedLevel = chip.getText().toString().toLowerCase();
        }

        displayList.clear();

        for (StudentRequest request : fullRequestList) {
            boolean matchesSubject = false;
            boolean matchesLevel = false;

            // 1. SUBJECT CHECK (Assume subject names might be English in DB)
            // If you have a LocalizationHelper for subjects, apply it here too.
            String reqSubject = request.getSubject() != null ? request.getSubject().toLowerCase() : "";
            if (TextUtils.isEmpty(query) || reqSubject.contains(query)) {
                matchesSubject = true;
            }

            // 2. LEVEL CHECK (CRITICAL FIX)
            // Get raw DB value: "Primary"
            String rawDbLevel = request.getLevel();
            String localizedDbLevel = rawDbLevel;

            // Use Helper to translate "Primary" -> "Rendah" (if app is in Malay)
            // Make sure you import your LocalizationHelper class
            int resId = com.example.mad_edumatch.helper.LocalizationHelper.getLevelStringId(rawDbLevel);
            if (resId != 0 && isAdded()) {
                localizedDbLevel = getString(resId);
            }

            // Now compare "rendah" (from DB translated) with "rendah" (from Chip)
            String reqLevelForSearch = localizedDbLevel != null ? localizedDbLevel.toLowerCase() : "";

            if (TextUtils.isEmpty(selectedLevel) || reqLevelForSearch.contains(selectedLevel)) {
                matchesLevel = true;
            }

            if (matchesSubject && matchesLevel) {
                displayList.add(request);
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyView();
    }
    private void updateEmptyView() {
        if (displayList.isEmpty()) {
            tvNoRequestFound.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoRequestFound.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}