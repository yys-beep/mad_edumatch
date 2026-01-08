package com.example.mad_edumatch.tutor;

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
import com.example.mad_edumatch.firebaseModels.TutorViewListing;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditTutorListingDialog extends DialogFragment {

    private TutorViewListing listing;
    private EditText etSubjects, etFee, etArea, etContact, etQualification;
    private ChipGroup chipGroupLevels;
    private RadioGroup rgLearningMode, rgDeliveryMode;

    public EditTutorListingDialog(TutorViewListing listing) {
        this.listing = listing;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_tutor_listing, null);

        // Bind Views
        etSubjects = view.findViewById(R.id.etEditSubjects);
        etFee = view.findViewById(R.id.etEditFee);
        etArea = view.findViewById(R.id.etEditArea);
        etContact = view.findViewById(R.id.etEditContact);
        etQualification = view.findViewById(R.id.etEditQualification);

        chipGroupLevels = view.findViewById(R.id.chipGroupEditLevels);
        rgLearningMode = view.findViewById(R.id.rgEditLearningMode);
        rgDeliveryMode = view.findViewById(R.id.rgEditDeliveryMode);

        Button btnSave = view.findViewById(R.id.btnSaveEdit);
        Button btnCancel = view.findViewById(R.id.btnCancelEdit);

        // Pre-fill Data
        if (listing != null) {
            etSubjects.setText(listing.getSubject());
            etFee.setText(String.valueOf(listing.getFee()));
            etArea.setText(listing.getArea());
            etContact.setText(listing.getContact());
            etQualification.setText(listing.getQualification());

            // Pre-select Chips
            preSelectLevels(listing.getAcademicLevels());

            // Pre-select Radio Buttons
            // Use string resources for comparison values
            String oneToOneVal = getString(R.string.val_one_to_one); // "One-to-One"
            String physicalVal = getString(R.string.val_physical);   // "Physical"

            selectRadioButton(rgLearningMode, listing.getLearningMode(), oneToOneVal, R.id.rbEditOneToOne, R.id.rbEditOneToMany);
            selectRadioButton(rgDeliveryMode, listing.getDeliveryMode(), physicalVal, R.id.rbEditPhysical, R.id.rbEditOnline);
        }

        btnSave.setOnClickListener(v -> updateListing());
        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }

    // Helper to select correct radio button based on string value
    private void selectRadioButton(RadioGroup group, String value, String compareValue, int id1, int id2) {
        if (value == null) return;
        if (value.equalsIgnoreCase(compareValue)) {
            group.check(id1);
        } else {
            group.check(id2);
        }
    }

    private void preSelectLevels(List<String> selectedLevels) {
        if (selectedLevels == null || selectedLevels.isEmpty()) return;
        for (int i = 0; i < chipGroupLevels.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupLevels.getChildAt(i);
            if (selectedLevels.contains(chip.getText().toString())) {
                chip.setChecked(true);
            }
        }
    }

    private ArrayList<String> getSelectedLevels() {
        ArrayList<String> selected = new ArrayList<>();
        for (int id : chipGroupLevels.getCheckedChipIds()) {
            Chip chip = chipGroupLevels.findViewById(id);
            selected.add(chip.getText().toString());
        }
        return selected;
    }

    private String getSelectedRadioText(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return getString(R.string.val_na); // "N/A"
        RadioButton btn = group.findViewById(id);
        return btn.getText().toString();
    }

    private void updateListing() {
        String newSubjects = etSubjects.getText().toString().trim();
        String newFeeStr = etFee.getText().toString().trim();
        String newArea = etArea.getText().toString().trim();
        String newContact = etContact.getText().toString().trim();
        String newQual = etQualification.getText().toString().trim();

        ArrayList<String> newLevels = getSelectedLevels();
        String newLMode = getSelectedRadioText(rgLearningMode);
        String newDMode = getSelectedRadioText(rgDeliveryMode);

        if (TextUtils.isEmpty(newSubjects) || TextUtils.isEmpty(newFeeStr) || newLevels.isEmpty() || TextUtils.isEmpty(newQual)) {
            Toast.makeText(getContext(), getString(R.string.error_fill_required_fields_edit), Toast.LENGTH_SHORT).show();
            return;
        }

        double newFee;
        try {
            newFee = Double.parseDouble(newFeeStr);
        } catch (NumberFormatException e) {
            etFee.setError(getString(R.string.error_invalid_fee_edit));
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_listings")
                .child(listing.getKey());

        Map<String, Object> updates = new HashMap<>();
        updates.put("subject", newSubjects);
        updates.put("fee", newFee);
        updates.put("area", newArea);
        updates.put("contact", newContact);
        updates.put("qualification", newQual);
        updates.put("learningMode", newLMode);
        updates.put("deliveryMode", newDMode);
        updates.put("academicLevels", newLevels);
        updates.put("timestamp", System.currentTimeMillis());

        ref.updateChildren(updates).addOnSuccessListener(unused -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.msg_listing_updated), Toast.LENGTH_SHORT).show();
            }
            dismiss();
        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                // Using format string for dynamic error message
                String errorMsg = getString(R.string.error_update_failed_2, e.getMessage());
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}