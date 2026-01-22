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
import com.example.mad_edumatch.helper.ListingDataHelper; // Import Helper
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
        View view = inflater.inflate(R.layout.dialog_edit_student_request, null);

        // Bind Views
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

        if (request != null) {
            etSubject.setText(request.getSubject());
            etArea.setText(request.getArea());
            etBudget.setText(String.valueOf(request.getBudget()));
            etDescription.setText(request.getDescription());

            // 1. Pre-select Level (Using Key)
            preSelectChip(request.getLevel());

            // 2. Pre-select Radios (Using Key)
            preSelectRadios();
        }

        btnSave.setOnClickListener(v -> updateRequest());

        builder.setView(view);
        return builder.create();
    }

    private void preSelectChip(String levelKey) {
        if (levelKey == null) return;

        // Find index of this key in our Helper Array
        int indexToSelect = -1;
        for(int i=0; i < ListingDataHelper.LEVEL_KEYS.length; i++){
            if(ListingDataHelper.LEVEL_KEYS[i].equals(levelKey)){
                indexToSelect = i;
                break;
            }
        }

        // If found and within ChipGroup bounds, check it
        if (indexToSelect != -1 && indexToSelect < chipGroupLevel.getChildCount()) {
            Chip chip = (Chip) chipGroupLevel.getChildAt(indexToSelect);
            chip.setChecked(true);
        }
    }

    private void preSelectRadios() {
        // Learning Mode Key
        String lMode = request.getLearningMode();
        if (ListingDataHelper.KEY_ONE_TO_ONE.equals(lMode)) {
            rbOneToOne.setChecked(true);
        } else if (ListingDataHelper.KEY_GROUP.equals(lMode)) {
            rbGroup.setChecked(true);
        }

        // Delivery Mode Key
        String dMode = request.getDeliveryMode();
        if (ListingDataHelper.KEY_PHYSICAL.equals(dMode)) {
            rbPhysical.setChecked(true);
        } else if (ListingDataHelper.KEY_ONLINE.equals(dMode)) {
            rbOnline.setChecked(true);
        }
    }

    private void updateRequest() {
        String newSubject = etSubject.getText().toString().trim();
        String newArea = etArea.getText().toString().trim();
        String newBudgetStr = etBudget.getText().toString().trim();
        String newDesc = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(newSubject) || TextUtils.isEmpty(newArea) || TextUtils.isEmpty(newBudgetStr)) {
            Toast.makeText(getContext(), getString(R.string.error_fill_required_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        // --- 3. GET NEW LEVEL KEY ---
        String newLevelKey = "";
        int checkedChipId = chipGroupLevel.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip chip = chipGroupLevel.findViewById(checkedChipId);
            int index = chipGroupLevel.indexOfChild(chip);
            if(index >= 0 && index < ListingDataHelper.LEVEL_KEYS.length){
                newLevelKey = ListingDataHelper.LEVEL_KEYS[index];
            }
        }

        if (newLevelKey.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_select_academic_level), Toast.LENGTH_SHORT).show();
            return;
        }

        // --- 4. GET NEW MODE KEYS ---
        String newLearningModeKey = "";
        if (rbOneToOne.isChecked()) newLearningModeKey = ListingDataHelper.KEY_ONE_TO_ONE;
        else if (rbGroup.isChecked()) newLearningModeKey = ListingDataHelper.KEY_GROUP;

        String newDeliveryModeKey = "";
        if (rbPhysical.isChecked()) newDeliveryModeKey = ListingDataHelper.KEY_PHYSICAL;
        else if (rbOnline.isChecked()) newDeliveryModeKey = ListingDataHelper.KEY_ONLINE;

        if (newLearningModeKey.isEmpty() || newDeliveryModeKey.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_select_modes), Toast.LENGTH_SHORT).show();
            return;
        }

        double newBudget;
        try {
            newBudget = Double.parseDouble(newBudgetStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), getString(R.string.error_invalid_budget_format), Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_requests")
                .child(request.getRequestId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("subject", newSubject);
        updates.put("area", newArea);
        updates.put("budget", newBudget);
        updates.put("description", newDesc);
        updates.put("level", newLevelKey);         // Save Key
        updates.put("learningMode", newLearningModeKey); // Save Key
        updates.put("deliveryMode", newDeliveryModeKey); // Save Key
        updates.put("timestamp", System.currentTimeMillis());

        ref.updateChildren(updates).addOnSuccessListener(unused -> {
            if (!isAdded() || getActivity() == null) return;
            Toast.makeText(getContext(), getString(R.string.msg_request_updated), Toast.LENGTH_SHORT).show();
            dismiss();
        }).addOnFailureListener(e -> {
            if (!isAdded() || getActivity() == null) return;
            String errorMsg = getString(R.string.error_update_failed_2, e.getMessage());
            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
        });
    }
}