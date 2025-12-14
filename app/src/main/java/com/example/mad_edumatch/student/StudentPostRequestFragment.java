package com.example.mad_edumatch.student;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.StudentRequest;
import com.example.mad_edumatch.helper.CurrentUser;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup; // Import this!
import com.google.firebase.database.FirebaseDatabase;

public class StudentPostRequestFragment extends Fragment {

    // UI Components
    private EditText etSubject, etArea, etBudget, etDescription;

    // CHANGED: Use ChipGroup instead of Spinner
    private ChipGroup chipGroupLevel;

    private RadioGroup rgLearningMode, rgDeliveryMode;
    private Button btnPostRequest;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.student_fragment_post_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        etSubject = view.findViewById(R.id.etSubject);
        etArea = view.findViewById(R.id.etArea);
        etBudget = view.findViewById(R.id.etBudget);
        etDescription = view.findViewById(R.id.etDescription);

        // CHANGED: Find view as ChipGroup
        chipGroupLevel = view.findViewById(R.id.chipGroupLevel);

        rgLearningMode = view.findViewById(R.id.rgLearningMode);
        rgDeliveryMode = view.findViewById(R.id.rgDeliveryMode);

        btnPostRequest = view.findViewById(R.id.btnPostRequest);

        // Note: No setupLevelSpinner() needed because chips are in XML

        btnPostRequest.setOnClickListener(v -> saveStudentRequest(view));
    }

    private void saveStudentRequest(View view) { // Pass View to find chip
        String subject = etSubject.getText().toString().trim();
        String area = etArea.getText().toString().trim();
        String budgetStr = etBudget.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // CHANGED: Get Level from ChipGroup
        String level = "";
        int selectedChipId = chipGroupLevel.getCheckedChipId();
        if (selectedChipId != View.NO_ID) {
            Chip selectedChip = view.findViewById(selectedChipId);
            level = selectedChip.getText().toString();
        }

        // Validation
        if (TextUtils.isEmpty(subject)) { etSubject.setError("Subject is required"); return; }

        // Check if level is empty
        if (level.isEmpty()) {
            Toast.makeText(requireContext(), "Please select an Academic Level", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(area)) { etArea.setError("Area is required"); return; }
        if (TextUtils.isEmpty(budgetStr)) { etBudget.setError("Budget is required"); return; }
        if (TextUtils.isEmpty(description)) { etDescription.setError("Description is required"); return; }

        double budget;
        try {
            budget = Double.parseDouble(budgetStr);
        } catch (NumberFormatException e) {
            etBudget.setError("Invalid budget format");
            return;
        }

        // Learning Mode
        int selectedModeId = rgLearningMode.getCheckedRadioButtonId();
        if (selectedModeId == -1) {
            Toast.makeText(requireContext(), "Please select a learning mode", Toast.LENGTH_SHORT).show();
            return;
        }
        String learningMode = (selectedModeId == R.id.rbOneToOne) ? "One-to-One" : "One-to-Many";

        // Delivery Mode
        int selectedDeliveryId = rgDeliveryMode.getCheckedRadioButtonId();
        if (selectedDeliveryId == -1) {
            Toast.makeText(requireContext(), "Please select a delivery mode", Toast.LENGTH_SHORT).show();
            return;
        }
        String deliveryMode = (selectedDeliveryId == R.id.rbPhysical) ? "Physical" : "Online";

        // Auth Check
        String studentId = CurrentUser.getInstance().getUid();
        if (studentId == null) {
            Toast.makeText(requireContext(), "You must be logged in to post", Toast.LENGTH_SHORT).show();
            return;
        }

        // Progress Dialog
        ProgressDialog dialog = new ProgressDialog(requireContext());
        dialog.setMessage("Posting request...");
        dialog.setCancelable(false);
        dialog.show();

        // Firebase Logic
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app");
        String requestId = database.getReference("student_requests").push().getKey();

        if (requestId == null) {
            dialog.dismiss();
            return;
        }

        StudentRequest request = new StudentRequest(
                requestId,
                studentId,
                subject,
                level,
                area,
                learningMode,
                deliveryMode,
                budget,
                description
        );

        database.getReference("student_requests")
                .child(requestId)
                .setValue(request)
                .addOnSuccessListener(unused -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Request posted successfully!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Failed to post request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}