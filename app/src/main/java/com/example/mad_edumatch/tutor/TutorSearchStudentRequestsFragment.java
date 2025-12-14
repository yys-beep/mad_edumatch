package com.example.mad_edumatch.tutor;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import com.example.mad_edumatch.recycleAdapters.TutorStudentRequestAdapter; // Ensure this adapter exists/is correct
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

public class TutorSearchStudentRequestsFragment extends Fragment {

    private TextInputEditText etSearchSubject; // Changed to TextInputEditText
    private ChipGroup chipGroupFilter;         // Added ChipGroup
    private RecyclerView recyclerView;
    private TextView tvNoRequestFound;         // Added "No Results" text

    private TutorStudentRequestAdapter adapter;
    private List<StudentRequest> fullRequestList;
    private List<StudentRequest> displayList;

    private DatabaseReference databaseRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tutor_fragment_search_student_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Views
        etSearchSubject = view.findViewById(R.id.etSearchSubject);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter); // Bind Chips
        recyclerView = view.findViewById(R.id.rvStudentRequests);
        tvNoRequestFound = view.findViewById(R.id.tvNoRequestFound); // Bind No Result Text

        // 2. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        fullRequestList = new ArrayList<>();
        displayList = new ArrayList<>();

        // Initialize Adapter (Use the correct adapter for Tutor viewing requests)
        adapter = new TutorStudentRequestAdapter(displayList);
        recyclerView.setAdapter(adapter);

        // 3. Setup Firebase
        databaseRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_requests");

        // 4. Load initial data
        loadAllRequests();

        // 5. Setup Search Actions

        // A. Handle Keyboard "Search" Button
        etSearchSubject.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // B. Handle Chip Selection Change
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            performSearch();
        });
    }

    private void loadAllRequests() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullRequestList.clear();
                displayList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    StudentRequest request = data.getValue(StudentRequest.class);
                    if (request != null) {
                        fullRequestList.add(request);
                    }
                }

                // Show Latest first
                Collections.reverse(fullRequestList);

                // Update display list
                displayList.addAll(fullRequestList);
                adapter.notifyDataSetChanged();

                updateEmptyView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performSearch() {
        // 1. Hide Keyboard
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // 2. Get Search Query
        String query = etSearchSubject.getText() != null ? etSearchSubject.getText().toString().trim().toLowerCase() : "";

        // 3. Get Selected Level from Chips
        String selectedLevel = "";
        int checkedChipId = chipGroupFilter.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip chip = chipGroupFilter.findViewById(checkedChipId);
            selectedLevel = chip.getText().toString().toLowerCase();
        }

        displayList.clear();

        // 4. Filtering Logic
        for (StudentRequest request : fullRequestList) {
            boolean matchesSubject = false;
            boolean matchesLevel = false;

            // Check Subject
            String reqSubject = request.getSubject() != null ? request.getSubject().toLowerCase() : "";
            if (TextUtils.isEmpty(query) || reqSubject.contains(query)) {
                matchesSubject = true;
            }

            // Check Level (Exact match for level is usually best)
            String reqLevel = request.getLevel() != null ? request.getLevel().toLowerCase() : "";
            // If no chip selected, we ignore level filtering (matches = true)
            // If chip is selected, we check if request level contains the chip text
            if (TextUtils.isEmpty(selectedLevel) || reqLevel.contains(selectedLevel)) {
                matchesLevel = true;
            }

            // Add if BOTH match
            if (matchesSubject && matchesLevel) {
                displayList.add(request);
            }
        }

        // 5. Update UI
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