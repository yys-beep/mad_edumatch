package com.example.mad_edumatch.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout; // Import LinearLayout
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

        recyclerView = view.findViewById(R.id.rvStudentRequests);
        layoutNoRequests = view.findViewById(R.id.layoutNoRequests); // Bind the empty view

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        requestList = new ArrayList<>();
        adapter = new StudentRequestAdapter(getContext(), requestList, getChildFragmentManager());
        recyclerView.setAdapter(adapter);

        String currentUserId = CurrentUser.getInstance().getUid();
        if (currentUserId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        databaseRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_requests");

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

                // Sorting
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
                // FIX: Check if user is null (Logged out). If so, ignore the error.
                if (CurrentUser.getInstance().getUid() == null) return;

                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Failed to load: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        databaseRef.orderByChild("studentId").equalTo(userId).addValueEventListener(myRequestsListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseRef != null && myRequestsListener != null) {
            databaseRef.removeEventListener(myRequestsListener);
        }
    }
}