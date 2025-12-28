package com.example.mad_edumatch.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.StudentRequest;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.recycleAdapters.StudentRequestAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentViewRequestsFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout layoutNoRequests; // Variable for the empty state
    private StudentRequestAdapter adapter;
    private List<StudentRequest> requestList;
    private DatabaseReference databaseRef;

    private ValueEventListener myRequestsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.student_fragment_view_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Views
        recyclerView = view.findViewById(R.id.rvStudentRequests);
        layoutNoRequests = view.findViewById(R.id.layoutNoRequests);

        // 2. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        requestList = new ArrayList<>();
        // Pass FragmentManager to adapter for Edit/Delete dialogs
        adapter = new StudentRequestAdapter(getContext(), requestList, getChildFragmentManager());
        recyclerView.setAdapter(adapter);

        // 3. Check Auth
        String currentUserId = CurrentUser.getInstance().getUid();
        if (currentUserId == null) {
            if (getContext() != null) {
                // Use resource string: "User not logged in" / "Pengguna tidak log masuk"
                Toast.makeText(getContext(), R.string.user_not_logged_in, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 4. Setup Firebase
        databaseRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_requests");

        // 5. Load Data
        loadMyRequests(currentUserId);
    }

    private void loadMyRequests(String userId) {
        myRequestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) {
                    return;
                }

                requestList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    StudentRequest request = data.getValue(StudentRequest.class);
                    if (request != null) {
                        requestList.add(request);
                    }
                }

                // Sort by Timestamp (Newest first)
                Collections.sort(requestList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                adapter.notifyDataSetChanged();

                // --- TOGGLE EMPTY STATE VISIBILITY ---
                if (requestList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    layoutNoRequests.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    layoutNoRequests.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore error if user logged out
                if (CurrentUser.getInstance().getUid() == null) return;

                if (!isAdded() || getContext() == null) return;

                // Use resource string with format: "Failed to load: %s"
                String errorMsg = getString(R.string.failed_to_load_format, error.getMessage());
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        };

        // Query: Only get requests where studentId matches current user
        databaseRef.orderByChild("studentId").equalTo(userId).addValueEventListener(myRequestsListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listener to prevent crashes/memory leaks
        if (databaseRef != null && myRequestsListener != null) {
            databaseRef.removeEventListener(myRequestsListener);
        }
    }
}