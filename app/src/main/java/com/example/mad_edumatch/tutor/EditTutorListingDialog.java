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
import com.example.mad_edumatch.helper.ListingDataHelper; // Ensure this exists
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
    private EditText etName, etSubjects, etFee, etArea, etContact, etQualification;
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

        // Bind Views matching your XML
        etName = view.findViewById(R.id.etEditName);
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
            etName.setText(listing.getName());
            etSubjects.setText(listing.getSubject());
            etFee.setText(String.valueOf(listing.getFee()));
            etArea.setText(listing.getArea());
            etContact.setText(listing.getContact());
            etQualification.setText(listing.getQualification());

            // --- 1. PRE-SELECT CHIPS USING KEYS ---
            preSelectLevels(listing.getAcademicLevels());

            // --- 2. PRE-SELECT RADIOS USING KEYS ---
            selectRadioButton(rgLearningMode, listing.getLearningMode(),
                    ListingDataHelper.KEY_ONE_TO_ONE, // Compare with Key
                    R.id.rbEditOneToOne, R.id.rbEditOneToMany);

            selectRadioButton(rgDeliveryMode, listing.getDeliveryMode(),
                    ListingDataHelper.KEY_PHYSICAL,   // Compare with Key
                    R.id.rbEditPhysical, R.id.rbEditOnline);
        }

        btnSave.setOnClickListener(v -> updateListing());
        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }

    private void preSelectLevels(List<String> selectedLevelKeys) {
        if (selectedLevelKeys == null || selectedLevelKeys.isEmpty()) return;

        // Iterate UI chips and match with keys by index
        for (int i = 0; i < chipGroupLevels.getChildCount(); i++) {
            if (i >= ListingDataHelper.LEVEL_KEYS.length) break;

            String keyForThisChip = ListingDataHelper.LEVEL_KEYS[i];

            if (selectedLevelKeys.contains(keyForThisChip)) {
                Chip chip = (Chip) chipGroupLevels.getChildAt(i);
                chip.setChecked(true);
            }
        }
    }

    private void selectRadioButton(RadioGroup group, String dbValue, String targetKey, int idMatch, int idMismatch) {
        if (dbValue == null) return;
        // Compare DB value against KEY
        if (dbValue.equals(targetKey)) {
            group.check(idMatch);
        } else {
            group.check(idMismatch);
        }
    }

    private void updateListing() {
        String newName = etName.getText().toString().trim();
        String newSubjects = etSubjects.getText().toString().trim();
        String newFeeStr = etFee.getText().toString().trim();
        String newArea = etArea.getText().toString().trim();
        String newContact = etContact.getText().toString().trim();
        String newQual = etQualification.getText().toString().trim();

        // --- 3. GET KEYS TO SAVE ---
        String newLModeKey = (rgLearningMode.getCheckedRadioButtonId() == R.id.rbEditOneToOne)
                ? ListingDataHelper.KEY_ONE_TO_ONE
                : ListingDataHelper.KEY_GROUP;

        String newDModeKey = (rgDeliveryMode.getCheckedRadioButtonId() == R.id.rbEditPhysical)
                ? ListingDataHelper.KEY_PHYSICAL
                : ListingDataHelper.KEY_ONLINE;

        ArrayList<String> newLevelKeys = new ArrayList<>();
        for (int i = 0; i < chipGroupLevels.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupLevels.getChildAt(i);
            if (chip.isChecked() && i < ListingDataHelper.LEVEL_KEYS.length) {
                newLevelKeys.add(ListingDataHelper.LEVEL_KEYS[i]);
            }
        }

        // Validation
        if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newSubjects) || TextUtils.isEmpty(newFeeStr) || newLevelKeys.isEmpty() || TextUtils.isEmpty(newQual)) {
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
        updates.put("name", newName);
        updates.put("subject", newSubjects);
        updates.put("fee", newFee);
        updates.put("area", newArea);
        updates.put("contact", newContact);
        updates.put("qualification", newQual);
        updates.put("learningMode", newLModeKey); // Save Key
        updates.put("deliveryMode", newDModeKey); // Save Key
        updates.put("academicLevels", newLevelKeys); // Save Keys
        updates.put("timestamp", System.currentTimeMillis());

        ref.updateChildren(updates).addOnSuccessListener(unused -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.msg_listing_updated), Toast.LENGTH_SHORT).show();
            }
            dismiss();
        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                String errorMsg = getString(R.string.error_update_failed_2, e.getMessage());
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}