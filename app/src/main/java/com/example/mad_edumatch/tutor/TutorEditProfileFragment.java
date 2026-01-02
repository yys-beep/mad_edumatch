package com.example.mad_edumatch.tutor;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.TutorProfile;
import com.example.mad_edumatch.helper.AvatarManager;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.recycleAdapters.AvatarAdapter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TutorEditProfileFragment extends Fragment implements AvatarAdapter.AvatarClickListener {

    private EditText etUsername, etEmail, etContact, etFee, etArea, etQualification, etDescription;
    private Button btnSaveProfile;

    // Dynamic List Containers
    private LinearLayout llSubjectsContainer, llExperienceContainer, llAchievementContainer;
    private Button btnAddSubject, btnAddExperience, btnAddAchievement;

    // Avatar Selection
    private RecyclerView rvEditAvatarSelect;
    private AvatarAdapter avatarAdapter;
    private String selectedAvatarName = AvatarManager.getAvatarName(0);

    private View progressOverlay;
    private String tutorId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tutor_fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tutorId = CurrentUser.getInstance().getUid();
        bindViews(view);
        setupAvatarRecyclerView();
        setupDynamicListeners();
        loadTutorProfile();

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void bindViews(View view) {
        etUsername = view.findViewById(R.id.etTutorName);
        etEmail = view.findViewById(R.id.etTutorEmail);
        etContact = view.findViewById(R.id.etTutorContact);
        etFee = view.findViewById(R.id.etTutorFee);
        etArea = view.findViewById(R.id.etTutorArea);
        etQualification = view.findViewById(R.id.etTutorQualification);
        etDescription = view.findViewById(R.id.etTutorDescription);

        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        progressOverlay = view.findViewById(R.id.progressOverlay);

        // Containers
        llSubjectsContainer = view.findViewById(R.id.llSubjectsContainer);
        llExperienceContainer = view.findViewById(R.id.llExperienceContainer);
        llAchievementContainer = view.findViewById(R.id.llAchievementContainer);

        // Add Buttons
        btnAddSubject = view.findViewById(R.id.btnAddSubject);
        btnAddExperience = view.findViewById(R.id.btnAddExperience);
        btnAddAchievement = view.findViewById(R.id.btnAddAchievement);

        // Avatar Recycler
        rvEditAvatarSelect = view.findViewById(R.id.rvEditAvatarSelect);
    }

    private void setupAvatarRecyclerView() {
        rvEditAvatarSelect.setLayoutManager(new GridLayoutManager(requireContext(), 5));
        avatarAdapter = new AvatarAdapter(AvatarManager.AVATAR_DRAWABLES, this);
        rvEditAvatarSelect.setAdapter(avatarAdapter);
    }

    @Override
    public void onAvatarSelected(int index) {
        selectedAvatarName = AvatarManager.getAvatarName(index);
    }

    private void setupDynamicListeners() {
        // Replace hardcoded "Subject", "Experience", "Achievement"
        btnAddSubject.setOnClickListener(v -> addDynamicItemInput(llSubjectsContainer, getString(R.string.hint_subject)));
        btnAddExperience.setOnClickListener(v -> addDynamicItemInput(llExperienceContainer, getString(R.string.hint_experience)));
        btnAddAchievement.setOnClickListener(v -> addDynamicItemInput(llAchievementContainer, getString(R.string.hint_achievement)));
    }

    private void loadTutorProfile() {
        progressOverlay.setVisibility(View.VISIBLE);
        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_profiles")
                .child(tutorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressOverlay.setVisibility(View.GONE);

                        // Fallback: If tutor profile is missing, try to get Name/Email from Users node
                        if (!snapshot.exists()) {
                            loadBasicUserFallback();
                            return;
                        }

                        TutorProfile profile = snapshot.getValue(TutorProfile.class);
                        if (profile != null) {
                            // 1. Name & Email (Always set these)
                            etUsername.setText(profile.getUsername());
                            etEmail.setText(profile.getEmail());

                            // 2. Contact: Clear "N/A" or "-" for cleaner editing
                            String contact = profile.getContact();
                            if (contact != null && !contact.equals("N/A") && !contact.equals("-")) {
                                etContact.setText(contact);
                            } else {
                                etContact.setText("");
                            }

                            // 3. Fee: Clear "N/A", "0", or "-"
                            String fee = profile.getFee();
                            if (fee != null && !fee.equals("N/A") && !fee.equals("0") && !fee.equals("-")) {
                                etFee.setText(fee);
                            } else {
                                etFee.setText("");
                            }

                            // 4. Area: Clear "N/A"
                            String area = profile.getArea();
                            if (area != null && !area.equals("N/A") && !area.equals("-")) {
                                etArea.setText(area);
                            } else {
                                etArea.setText("");
                            }

                            // 5. Qualification: Clear "N/A"
                            String qual = profile.getQualification();
                            if (qual != null && !qual.equals("N/A") && !qual.equals("-")) {
                                etQualification.setText(qual);
                            } else {
                                etQualification.setText("");
                            }

                            // 6. Description: Clear "N/A" or "-"
                            String desc = profile.getDescription();
                            if (desc != null && !desc.equals("N/A") && !desc.equals("-")) {
                                etDescription.setText(desc);
                            } else {
                                etDescription.setText("");
                            }

                            // 7. Avatar Selection
                            selectedAvatarName = profile.getProfileImageUrl();
                            // Default to avatar_1 if missing
                            if (selectedAvatarName == null || selectedAvatarName.isEmpty()) {
                                selectedAvatarName = "avatar_1";
                            }
                            int avatarIndex = getAvatarIndexFromName(selectedAvatarName);

                            // Update the RecyclerView selection
                            if (avatarAdapter != null) {
                                avatarAdapter.setSelectedPosition(avatarIndex);
                            }

                            // 8. Load Dynamic Lists
                            // Replace hardcoded hints here as well
                            populateDynamicList(llSubjectsContainer, profile.getSubjects(), getString(R.string.hint_subject));
                            populateDynamicList(llExperienceContainer, profile.getExperience(), getString(R.string.hint_experience));
                            populateDynamicList(llAchievementContainer, profile.getAchievement(), getString(R.string.hint_achievement));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressOverlay.setVisibility(View.GONE);
                        // Replace "Failed to load data"
                        if (getContext() != null) {
                            Toast.makeText(getContext(), getString(R.string.error_load_data), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Helper: Loads basic info from the main 'Users' node if the Tutor profile is incomplete
    private void loadBasicUserFallback() {
        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users")
                .child(tutorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String name = snapshot.child("name").getValue(String.class);
                            String email = snapshot.child("email").getValue(String.class);
                            if (name != null) etUsername.setText(name);
                            if (email != null) etEmail.setText(email);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void saveProfile() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String fee = etFee.getText().toString().trim();
        String area = etArea.getText().toString().trim();
        String qualification = etQualification.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Collect Lists from UI
        ArrayList<String> subjectsList = getItemsFromContainer(llSubjectsContainer);
        ArrayList<String> experienceList = getItemsFromContainer(llExperienceContainer);
        ArrayList<String> achievementList = getItemsFromContainer(llAchievementContainer);

        if (TextUtils.isEmpty(username)) {
            Toast.makeText(requireContext(), getString(R.string.error_name_required), Toast.LENGTH_SHORT).show();
            return;
        }

        progressOverlay.setVisibility(View.VISIBLE);

        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_profiles")
                .child(tutorId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    TutorProfile existing = snapshot.getValue(TutorProfile.class);

                    // Preserve uneditable data from the existing profile
                    long regTime = existing != null ? existing.getRegisterTime() : System.currentTimeMillis();
                    String currentAvatar = existing != null ? existing.getProfileImageUrl() : AvatarManager.getAvatarName(0);
                    boolean isVerifiedStatus = existing != null && existing.isVerified(); // NEW: Preserve isVerified status

                    // Determine final avatar
                    String avatarToSave = selectedAvatarName != null ? selectedAvatarName : currentAvatar;

                    // Create the NEW TutorProfile object using collected data and preserved data
                    TutorProfile profile = new TutorProfile(
                            tutorId,
                            username,
                            email,
                            contact,
                            subjectsList,      // Saved as ArrayList<String>
                            fee,
                            area,
                            qualification,
                            achievementList,   // Saved as ArrayList<String>
                            experienceList,    // Saved as ArrayList<String>
                            description,
                            regTime,
                            avatarToSave
                    );

                    // Manually set isVerified as it doesn't have a setter in the current model
                    // To save boolean field correctly, we need a Map or a setter/constructor update.
                    Map<String, Object> profileUpdates = new HashMap<>();
                    profileUpdates.put("userId", profile.getUserId());
                    profileUpdates.put("username", profile.getUsername());
                    profileUpdates.put("email", profile.getEmail());
                    profileUpdates.put("contact", profile.getContact());
                    profileUpdates.put("subjects", profile.getSubjects());
                    profileUpdates.put("fee", profile.getFee());
                    profileUpdates.put("area", profile.getArea());
                    profileUpdates.put("qualification", profile.getQualification());
                    profileUpdates.put("achievement", profile.getAchievement());
                    profileUpdates.put("experience", profile.getExperience());
                    profileUpdates.put("description", profile.getDescription());
                    profileUpdates.put("registerTime", profile.getRegisterTime());
                    profileUpdates.put("profileImageUrl", profile.getProfileImageUrl());
                    profileUpdates.put("verified", isVerifiedStatus); // Save isVerified correctly

                    // --- SAVE THE DATA ---
                    FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("tutor_profiles")
                            .child(tutorId)
                            .updateChildren(profileUpdates) // Using updateChildren is safer than setValue
                            .addOnCompleteListener(task -> {
                                progressOverlay.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    updateMainUserNode(username, avatarToSave);

                                    Toast.makeText(requireContext(), getString(R.string.msg_profile_updated), Toast.LENGTH_SHORT).show();
                                    getParentFragmentManager().popBackStack();
                                } else {
                                    Toast.makeText(requireContext(), getString(R.string.error_update_profile), Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    // Helper to sync to Users node
    private void updateMainUserNode(String newName, String newAvatar) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("profileImageUrl", newAvatar);

        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users")
                .child(tutorId)
                .updateChildren(updates);
    }

    // --- Dynamic List Helpers ---

    // Inside TutorEditProfileFragment.java

// --- Dynamic List Helpers ---

    // Helper that creates a new input block for lists (Subject, Experience, etc.)
    private void addDynamicItemInput(LinearLayout container, String hint) {
        addDynamicItemInput(container, hint, ""); // Default to empty string
    }

    private void addDynamicItemInput(LinearLayout container, String hint, String initialValue) {
        Context context = requireContext();

        // 1. Row Layout (Horizontal)
        LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 8, 0, 8); // Add vertical margin between blocks
        rowLayout.setLayoutParams(rowParams);

        // 2. TextInputEditText wrapped in TextInputLayout for styling
        // We create the TextInputLayout first
        com.google.android.material.textfield.TextInputLayout textInputLayout =
                new com.google.android.material.textfield.TextInputLayout(context, null, com.google.android.material.R.attr.textInputStyle);
        textInputLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        textInputLayout.setHint(hint);

        LinearLayout.LayoutParams etLayout = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        etLayout.setMarginEnd(8);
        textInputLayout.setLayoutParams(etLayout);

        TextInputEditText editText = new TextInputEditText(context);
        editText.setText(initialValue);

        textInputLayout.addView(editText); // Add EditText into TextInputLayout

        // 3. Remove Button
        ImageView removeButton = new ImageView(context);
        removeButton.setImageResource(R.drawable.outline_delete_24); // Ensure this drawable exists
        removeButton.setPadding(16, 16, 16, 16);
        removeButton.setOnClickListener(v -> container.removeView(rowLayout));

        // 4. Add components to the row
        rowLayout.addView(textInputLayout);
        rowLayout.addView(removeButton);
        container.addView(rowLayout);
    }

    // Loads existing data from ArrayList into the UI container
    private void populateDynamicList(LinearLayout container, ArrayList<String> items, String hint) {
        // IMPORTANT: Clear container first before adding (in case of re-load logic)
        container.removeAllViews();
        if (items != null) {
            for (String item : items) {
                if (!item.trim().isEmpty()) {
                    addDynamicItemInput(container, hint, item);
                }
            }
        }
    }

    // Extracts data from the UI container (same logic as before, but accessing the correct view type)
    private ArrayList<String> getItemsFromContainer(LinearLayout container) {
        ArrayList<String> items = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            if (row instanceof LinearLayout) {
                // The EditText is nested inside the TextInputLayout, which is the first child of the row
                View textInputLayout = ((LinearLayout) row).getChildAt(0);
                if (textInputLayout instanceof com.google.android.material.textfield.TextInputLayout) {
                    TextInputEditText editText = (TextInputEditText) ((com.google.android.material.textfield.TextInputLayout) textInputLayout).getEditText();
                    if (editText != null) {
                        String text = editText.getText().toString().trim();
                        if (!text.isEmpty()) {
                            items.add(text);
                        }
                    }
                }
            }
        }
        return items;
    }

    private int getAvatarIndexFromName(String avatarName) {
        if (avatarName == null || avatarName.isEmpty()) return 0;
        try {
            String[] parts = avatarName.split("_");
            if (parts.length > 1) return Integer.parseInt(parts[1]) - 1;
        } catch (Exception ignored) {}
        return 0;
    }
}