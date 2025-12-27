package com.example.mad_edumatch.student;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

    // Text Fields
    private EditText etSubject, etArea, etBudget, etDescription;
    // Chips
    private ChipGroup chipGroupLevel;
    // Radio Groups & Buttons
    private RadioGroup rgLearningMode, rgDeliveryMode;
    private RadioButton rbOneToOne, rbGroup, rbPhysical, rbOnline;

    public EditStudentRequestDialog(StudentRequest request) {
        this.request = request;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Ensure this layout filename matches your XML file
        View view = inflater.inflate(R.layout.dialog_edit_student_request, null);

        // 1. Initialize Views
        etSubject = view.findViewById(R.id.etEditSubject);
        etArea = view.findViewById(R.id.etEditArea);
        etBudget = view.findViewById(R.id.etEditBudget);
        etDescription = view.findViewById(R.id.etEditDescription);

        chipGroupLevel = view.findViewById(R.id.chipGroupEditLevel);

        rgLearningMode = view.findViewById(R.id.rgEditLearningMode);
        rgDeliveryMode = view.findViewById(R.id.rgEditDeliveryMode);

        rbOneToOne = view.findViewById(R.id.rbEditOneToOne);
        rbGroup = view.findViewById(R.id.rbEditGroup);
        rbPhysical = view.findViewById(R.id.rbEditPhysical);
        rbOnline = view.findViewById(R.id.rbEditOnline);

        Button btnSave = view.findViewById(R.id.btnSaveChanges);

        // 2. Pre-fill Data
        if (request != null) {
            etSubject.setText(request.getSubject());
            etArea.setText(request.getArea());
            etBudget.setText(String.valueOf(request.getBudget()));
            etDescription.setText(request.getDescription());

            // Set Chips
            preSelectChip(request.getLevel());

            // Set Radio Buttons
            preSelectRadios();
        }

        btnSave.setOnClickListener(v -> updateRequest());

        builder.setView(view);
        return builder.create();
    }

    private void preSelectChip(String level) {
        if (level == null) return;
        for (int i = 0; i < chipGroupLevel.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupLevel.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(level)) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private void preSelectRadios() {
        // Learning Mode
        String lMode = request.getLearningMode(); // Ensure getter exists in StudentRequest model
        if ("One-to-One".equalsIgnoreCase(lMode)) {
            rbOneToOne.setChecked(true);
        } else if ("Group".equalsIgnoreCase(lMode)) {
            rbGroup.setChecked(true);
        }

        // Delivery Mode
        String dMode = request.getDeliveryMode(); // Ensure getter exists in StudentRequest model
        if ("Physical".equalsIgnoreCase(dMode)) {
            rbPhysical.setChecked(true);
        } else if ("Online".equalsIgnoreCase(dMode)) {
            rbOnline.setChecked(true);
        }
    }

    private void updateRequest() {
        // A. Validate Text Inputs
        String newSubject = etSubject.getText().toString().trim();
        String newArea = etArea.getText().toString().trim();
        String newBudgetStr = etBudget.getText().toString().trim();
        String newDesc = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(newSubject) || TextUtils.isEmpty(newArea) || TextUtils.isEmpty(newBudgetStr)) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // B. Get Selected Chip
        String newLevel = "";
        int checkedChipId = chipGroupLevel.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip chip = chipGroupLevel.findViewById(checkedChipId);
            newLevel = chip.getText().toString();
        } else {
            Toast.makeText(getContext(), "Please select an academic level", Toast.LENGTH_SHORT).show();
            return;
        }

        // C. Get Radio Selections
        String newLearningMode = "";
        if (rbOneToOne.isChecked()) newLearningMode = "One-to-One";
        else if (rbGroup.isChecked()) newLearningMode = "Group";

        String newDeliveryMode = "";
        if (rbPhysical.isChecked()) newDeliveryMode = "Physical";
        else if (rbOnline.isChecked()) newDeliveryMode = "Online";

        if (newLearningMode.isEmpty() || newDeliveryMode.isEmpty()) {
            Toast.makeText(getContext(), "Please select both Learning and Delivery modes", Toast.LENGTH_SHORT).show();
            return;
        }

        // D. Parse Budget
        double newBudget;
        try {
            newBudget = Double.parseDouble(newBudgetStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid budget format", Toast.LENGTH_SHORT).show();
            return;
        }

        // E. Update Firebase
        DatabaseReference ref = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_requests")
                .child(request.getRequestId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("subject", newSubject);
        updates.put("area", newArea);
        updates.put("budget", newBudget);
        updates.put("description", newDesc);
        updates.put("level", newLevel);
        updates.put("learningMode", newLearningMode);
        updates.put("deliveryMode", newDeliveryMode);

        // Update timestamp to show as "Just now" or move to top
        updates.put("timestamp", System.currentTimeMillis());

        ref.updateChildren(updates).addOnSuccessListener(unused -> {
            if (!isAdded() || getActivity() == null) return;
            Toast.makeText(getContext(), "Request Updated", Toast.LENGTH_SHORT).show();
            dismiss();
        }).addOnFailureListener(e -> {
            if (!isAdded() || getActivity() == null) return;
            Toast.makeText(getContext(), "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}