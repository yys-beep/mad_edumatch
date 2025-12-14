package com.example.mad_edumatch.student;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.StudentRequest;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class EditStudentRequestDialog extends DialogFragment {

    private StudentRequest request;
    private EditText etSubject, etArea, etBudget, etDescription;
    private ChipGroup chipGroupLevel;

    // Constructor to pass the request object
    public EditStudentRequestDialog(StudentRequest request) {
        this.request = request;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_student_request, null);

        etSubject = view.findViewById(R.id.etEditSubject);
        etArea = view.findViewById(R.id.etEditArea);
        etBudget = view.findViewById(R.id.etEditBudget);
        etDescription = view.findViewById(R.id.etEditDescription);
        chipGroupLevel = view.findViewById(R.id.chipGroupEditLevel);
        Button btnSave = view.findViewById(R.id.btnSaveChanges);

        // Pre-fill Data
        if (request != null) {
            etSubject.setText(request.getSubject());
            etArea.setText(request.getArea());
            etBudget.setText(String.valueOf(request.getBudget()));
            etDescription.setText(request.getDescription());

            // Pre-select the correct Chip
            preSelectChip(request.getLevel());
        }

        btnSave.setOnClickListener(v -> updateRequest());

        builder.setView(view);
        return builder.create();
    }

    private void preSelectChip(String level) {
        if (level == null) return;
        for (int i = 0; i < chipGroupLevel.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupLevel.getChildAt(i);
            if (chip.getText().toString().equals(level)) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private void updateRequest() {
        String newSubject = etSubject.getText().toString().trim();
        String newArea = etArea.getText().toString().trim();
        String newBudgetStr = etBudget.getText().toString().trim();
        String newDesc = etDescription.getText().toString().trim();

        // Get Level
        String newLevel = "";
        int checkedChipId = chipGroupLevel.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip chip = chipGroupLevel.findViewById(checkedChipId);
            newLevel = chip.getText().toString();
        }

        if (TextUtils.isEmpty(newSubject) || TextUtils.isEmpty(newArea) || TextUtils.isEmpty(newBudgetStr) || TextUtils.isEmpty(newLevel)) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double newBudget = Double.parseDouble(newBudgetStr);

        DatabaseReference ref = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_requests")
                .child(request.getRequestId());

        // Create update map (updates specific fields only)
        Map<String, Object> updates = new HashMap<>();
        updates.put("subject", newSubject);
        updates.put("area", newArea);
        updates.put("budget", newBudget);
        updates.put("description", newDesc);
        updates.put("level", newLevel);

        ref.updateChildren(updates).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Request Updated", Toast.LENGTH_SHORT).show();
            dismiss();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Update Failed", Toast.LENGTH_SHORT).show();
        });
    }
}