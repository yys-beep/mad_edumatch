package com.example.mad_edumatch.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

public class StudentViewRequestsFragment extends Fragment {

    private RecyclerView recyclerView;
    private StudentRequestAdapter adapter;
    private List<StudentRequest> requestList;
    private DatabaseReference databaseRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.student_fragment_view_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Setup RecyclerView
        recyclerView = view.findViewById(R.id.rvStudentRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        requestList = new ArrayList<>();

        // 2. Initialize Adapter CORRECTLY
        // Arguments: Context, List, FragmentManager (for the Edit Dialog)
        adapter = new StudentRequestAdapter(getContext(), requestList, getChildFragmentManager());

        recyclerView.setAdapter(adapter);

        // 3. Get User ID
        String currentUserId = CurrentUser.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Firebase Ref
        databaseRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_requests");

        loadMyRequests(currentUserId);
    }

    private void loadMyRequests(String userId) {
        databaseRef.orderByChild("studentId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        requestList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            StudentRequest request = data.getValue(StudentRequest.class);
                            if (request != null) {
                                requestList.add(request);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (requestList.isEmpty()) {
                            Toast.makeText(getContext(), "No requests found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to load: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Removed confirmDelete() and deleteRequest()
    // because the logic is now handled inside StudentRequestAdapter.
}