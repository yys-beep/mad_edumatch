package com.example.mad_edumatch.tutor;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.TutorViewListing;
import com.example.mad_edumatch.helper.CurrentUser;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class TutorPostListingFragment extends Fragment {

    private EditText etName, etSubjects, etFee, etArea, etContact, etQualification;
    private RadioGroup rgLearningMode, rgDeliveryMode;
    private ChipGroup chipGroupLevels;
    private Button btnSubmit;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tutor_fragment_post_tutor_listing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        etName = view.findViewById(R.id.etTutorName);
        etSubjects = view.findViewById(R.id.etTutorSubjects);
        etFee = view.findViewById(R.id.etTutorFee);
        etArea = view.findViewById(R.id.etTutorArea);
        etContact = view.findViewById(R.id.etTutorContact);
        etQualification = view.findViewById(R.id.etTutorQualification);

        rgLearningMode = view.findViewById(R.id.rgLearningMode);
        rgDeliveryMode = view.findViewById(R.id.rgDeliveryMode);
        chipGroupLevels = view.findViewById(R.id.chipGroupLevels);

        btnSubmit = view.findViewById(R.id.btnSubmitTutorListing);

        btnSubmit.setOnClickListener(v -> postListing());
    }

    private void postListing() {
        String name = etName.getText().toString().trim();
        String subject = etSubjects.getText().toString().trim();
        String feeStr = etFee.getText().toString().trim();
        String area = etArea.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String qualification = etQualification.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(subject) || TextUtils.isEmpty(feeStr) || TextUtils.isEmpty(qualification)) {
            Toast.makeText(getContext(), getString(R.string.error_fill_required_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        double fee = 0;
        try {
            fee = Double.parseDouble(feeStr);
        } catch (NumberFormatException e) {
            etFee.setError(getString(R.string.error_invalid_fee));
            return;
        }

        // Get Radio Button Selections
        String learningMode = getSelectedRadioText(rgLearningMode);
        String deliveryMode = getSelectedRadioText(rgDeliveryMode);

        // Get Academic Levels from Chips
        ArrayList<String> selectedLevels = new ArrayList<>();
        for (int i = 0; i < chipGroupLevels.getChildCount(); i++) {
            View child = chipGroupLevels.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    selectedLevels.add(chip.getText().toString());
                }
            }
        }

        long timestamp = System.currentTimeMillis();
        String tutorId = CurrentUser.getInstance().getUid();
        String notAvailable = getString(R.string.text_not_applicable);

        // Create Object
        TutorViewListing listing = new TutorViewListing(
                tutorId, name, subject, fee, area, contact,
                learningMode, deliveryMode,
                qualification,
                notAvailable, // achievement passed as "N/A"
                timestamp,
                selectedLevels
        );

        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_listings")
                .push()
                .setValue(listing)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), getString(R.string.msg_listing_posted), Toast.LENGTH_SHORT).show();
                        if (getParentFragmentManager() != null) {
                            getParentFragmentManager().popBackStack();
                        }
                    } else {
                        Toast.makeText(getContext(), getString(R.string.error_post_failed), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getSelectedRadioText(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return getString(R.string.text_not_applicable);
        RadioButton btn = group.findViewById(id);
        return btn.getText().toString();
    }
}