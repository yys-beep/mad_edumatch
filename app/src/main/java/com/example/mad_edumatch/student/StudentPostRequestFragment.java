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
import com.example.mad_edumatch.helper.ListingDataHelper; // Import Helper
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.FirebaseDatabase;

public class StudentPostRequestFragment extends Fragment {

    private EditText etSubject, etArea, etBudget, etDescription;
    private ChipGroup chipGroupLevel;
    private RadioGroup rgLearningMode, rgDeliveryMode;
    private Button btnPostRequest;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.student_fragment_post_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSubject = view.findViewById(R.id.etSubject);
        etArea = view.findViewById(R.id.etArea);
        etBudget = view.findViewById(R.id.etBudget);
        etDescription = view.findViewById(R.id.etDescription);
        chipGroupLevel = view.findViewById(R.id.chipGroupLevel);
        rgLearningMode = view.findViewById(R.id.rgLearningMode);
        rgDeliveryMode = view.findViewById(R.id.rgDeliveryMode);
        btnPostRequest = view.findViewById(R.id.btnPostRequest);

        btnPostRequest.setOnClickListener(v -> saveStudentRequest(view));
    }

    private void saveStudentRequest(View view) {
        String subject = etSubject.getText().toString().trim();
        String area = etArea.getText().toString().trim();
        String budgetStr = etBudget.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // --- 1. GET LEVEL KEY ---
        String levelKey = "";
        // We assume Chips are ordered: Primary(0), LowerSec(1)... matching LEVEL_KEYS array
        int selectedChipId = chipGroupLevel.getCheckedChipId();
        if (selectedChipId != View.NO_ID) {
            Chip selectedChip = view.findViewById(selectedChipId);
            // Find index of this chip in the group
            int index = chipGroupLevel.indexOfChild(selectedChip);
            if (index >= 0 && index < ListingDataHelper.LEVEL_KEYS.length) {
                levelKey = ListingDataHelper.LEVEL_KEYS[index]; // Save "PRIMARY"
            }
        }

        // Validation
        if (TextUtils.isEmpty(subject)) {
            etSubject.setError(getString(R.string.error_subject_required));
            return;
        }

        if (levelKey.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_select_academic_level), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(area)) {
            etArea.setError(getString(R.string.error_area_required));
            return;
        }
        if (TextUtils.isEmpty(budgetStr)) {
            etBudget.setError(getString(R.string.error_budget_required));
            return;
        }
        if (TextUtils.isEmpty(description)) {
            etDescription.setError(getString(R.string.error_description_required));
            return;
        }

        double budget;
        try {
            budget = Double.parseDouble(budgetStr);
        } catch (NumberFormatException e) {
            etBudget.setError(getString(R.string.error_invalid_budget_format));
            return;
        }

        // --- 2. GET LEARNING MODE KEY ---
        int selectedModeId = rgLearningMode.getCheckedRadioButtonId();
        if (selectedModeId == -1) {
            Toast.makeText(requireContext(), getString(R.string.error_select_learning_mode), Toast.LENGTH_SHORT).show();
            return;
        }
        String learningModeKey = (selectedModeId == R.id.rbOneToOne)
                ? ListingDataHelper.KEY_ONE_TO_ONE
                : ListingDataHelper.KEY_GROUP;

        // --- 3. GET DELIVERY MODE KEY ---
        int selectedDeliveryId = rgDeliveryMode.getCheckedRadioButtonId();
        if (selectedDeliveryId == -1) {
            Toast.makeText(requireContext(), getString(R.string.error_select_delivery_mode), Toast.LENGTH_SHORT).show();
            return;
        }
        String deliveryModeKey = (selectedDeliveryId == R.id.rbPhysical)
                ? ListingDataHelper.KEY_PHYSICAL
                : ListingDataHelper.KEY_ONLINE;

        // Auth Check
        String studentId = CurrentUser.getInstance().getUid();
        if (studentId == null) {
            Toast.makeText(requireContext(), getString(R.string.error_login_required_post), Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(requireContext());
        dialog.setMessage(getString(R.string.msg_posting_request));
        dialog.setCancelable(false);
        dialog.show();

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app");
        String requestId = database.getReference("student_requests").push().getKey();

        if (requestId == null) {
            dialog.dismiss();
            return;
        }

        // SAVE OBJECT WITH KEYS
        StudentRequest request = new StudentRequest(
                requestId,
                studentId,
                subject,
                levelKey,        // Saving Key
                area,
                learningModeKey, // Saving Key
                deliveryModeKey, // Saving Key
                budget,
                description,
                System.currentTimeMillis()
        );

        database.getReference("student_requests")
                .child(requestId)
                .setValue(request)
                .addOnSuccessListener(unused -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), getString(R.string.msg_request_posted_success), Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    String errorMsg = getString(R.string.error_post_request_failed, e.getMessage());
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
                });
    }
}