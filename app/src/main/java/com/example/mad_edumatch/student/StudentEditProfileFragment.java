package com.example.mad_edumatch.student;

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
import com.example.mad_edumatch.firebaseModels.StudentProfile;
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

public class StudentEditProfileFragment extends Fragment implements AvatarAdapter.AvatarClickListener {

    // Input fields (for basic data)
    private EditText etUsername, etEmail, etContact, etAge, etAcademicLevel, etDescription;

    // Dynamic list containers (replacing the old list EditTexts)
    private LinearLayout layoutAchievementsContainer, layoutLessonsContainer;
    private Button btnAddAchievement;

    // Avatar Selection
    private RecyclerView rvEditAvatarSelect;
    private AvatarAdapter avatarAdapter;
    private String selectedAvatarName = AvatarManager.getAvatarName(0); // Default

    private Button btnSaveProfile;
    private String studentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.student_fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        studentId = CurrentUser.getInstance().getUid();
        bindViews(view);
        setupAvatarRecyclerView();
        setupListListeners();
        loadStudentProfile();

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void bindViews(View view) {
        // Basic Info Fields
        etUsername = view.findViewById(R.id.etStudentName);
        etEmail = view.findViewById(R.id.etStudentEmail);
        etContact = view.findViewById(R.id.etStudentContact);
        etAge = view.findViewById(R.id.etStudentAge);
        etAcademicLevel = view.findViewById(R.id.etStudentAcademicLevel);
        etDescription = view.findViewById(R.id.etStudentDescription);

        // Dynamic List Elements
        layoutAchievementsContainer = view.findViewById(R.id.layoutAchievementsContainer);
        btnAddAchievement = view.findViewById(R.id.btnAddAchievement);

        // Avatar
        rvEditAvatarSelect = view.findViewById(R.id.rvEditAvatarSelect);

        // Save Button
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
    }

    private void setupAvatarRecyclerView() {
        rvEditAvatarSelect.setLayoutManager(new GridLayoutManager(requireContext(), 5));
        avatarAdapter = new AvatarAdapter(AvatarManager.AVATAR_DRAWABLES, this);
        rvEditAvatarSelect.setAdapter(avatarAdapter);
    }

    private void setupListListeners() {
        btnAddAchievement.setOnClickListener(v -> addDynamicItemInput(layoutAchievementsContainer, "Achievement"));
    }

    // Implementation for AvatarAdapter.AvatarClickListener
    @Override
    public void onAvatarSelected(int index) {
        selectedAvatarName = AvatarManager.getAvatarName(index);
    }

    private void loadStudentProfile() {
        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_profiles")
                .child(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            // Fallback: If student profile is missing, try to get Name/Email from Users node
                            loadBasicUserFallback();
                            return;
                        }

                        StudentProfile profile = snapshot.getValue(StudentProfile.class);
                        if (profile != null) {
                            // 1. Name & Email (Always Show)
                            etUsername.setText(profile.getUsername());
                            etEmail.setText(profile.getEmail());

                            // 2. Contact: Hide "N/A" or "-"
                            String contact = profile.getContact();
                            if (contact != null && !contact.equals("N/A") && !contact.equals("-")) {
                                etContact.setText(contact);
                            } else {
                                etContact.setText(""); // Leave blank for cleaner editing
                            }

                            // 3. Age: Hide "0"
                            if (profile.getAge() > 0) {
                                etAge.setText(String.valueOf(profile.getAge()));
                            } else {
                                etAge.setText("");
                            }

                            // 4. Academic Level: Hide "N/A"
                            String academic = profile.getAcademicLevel();
                            if (academic != null && !academic.equals("N/A")) {
                                etAcademicLevel.setText(academic);
                            } else {
                                etAcademicLevel.setText("");
                            }

                            // 5. Description: Hide "-"
                            String desc = profile.getDescription();
                            if (desc != null && !desc.equals("-")) {
                                etDescription.setText(desc);
                            } else {
                                etDescription.setText("");
                            }

                            // 6. Avatar Selection
                            selectedAvatarName = profile.getProfileImageUrl();
                            // If avatar is missing, default to avatar_1
                            if (selectedAvatarName == null || selectedAvatarName.isEmpty()) {
                                selectedAvatarName = "avatar_1";
                            }
                            int avatarIndex = getAvatarIndexFromName(selectedAvatarName);
                            // Update the RecyclerView selection
                            if(avatarAdapter != null) {
                                avatarAdapter.setSelectedPosition(avatarIndex);
                            }

                            // 7. Dynamic Lists
                            populateDynamicList(layoutAchievementsContainer, profile.getAchievements(), "Achievement");
                            populateDynamicList(layoutLessonsContainer, profile.getParticipatedFreeLessons(), "Lesson Name");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Failed to load profile.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper: Loads basic info if the specific StudentProfile is empty/missing
    private void loadBasicUserFallback() {
        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users")
                .child(studentId)
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

        // 1. Calculate age in a temporary variable first
        int tempAge = 0;
        try {
            tempAge = Integer.parseInt(etAge.getText().toString().trim());
        } catch (NumberFormatException ignored) {}

        // 2. Create a 'final' copy to use inside the Lambda
        final int finalAge = tempAge;

        String academicLevel = etAcademicLevel.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Get list data
        ArrayList<String> achievementsList = getItemsFromContainer(layoutAchievementsContainer);
        ArrayList<String> participatedLessonsList = getItemsFromContainer(layoutLessonsContainer);

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email)) {
            Toast.makeText(requireContext(), "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_profiles")
                .child(studentId)
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    StudentProfile existingProfile = dataSnapshot.getValue(StudentProfile.class);
                    // Handle case where profile might not exist yet
                    long registerTime = existingProfile != null ? existingProfile.getRegisterTime() : System.currentTimeMillis();
                    String currentAvatar = existingProfile != null ? existingProfile.getProfileImageUrl() : AvatarManager.getAvatarName(0);

                    // Use the selected avatar, or fallback to existing
                    String avatarToSave = selectedAvatarName != null ? selectedAvatarName : currentAvatar;

                    StudentProfile updatedProfile = new StudentProfile(
                            studentId,
                            username,
                            email,
                            contact,
                            finalAge, // <--- Use the final variable here
                            academicLevel,
                            description,
                            registerTime,
                            avatarToSave,
                            achievementsList,
                            participatedLessonsList
                    );

                    FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("student_profiles")
                            .child(studentId)
                            .setValue(updatedProfile)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    updateMainUserNode(username, avatarToSave);
                                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                                    getParentFragmentManager().popBackStack();
                                } else {
                                    Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    // Helper to sync specific fields to the "Users" node
    private void updateMainUserNode(String newName, String newAvatar) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("profileImageUrl", newAvatar);

        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users")
                .child(studentId)
                .updateChildren(updates);
    }

    // ===============================================
    // DYNAMIC LIST UTILITIES (Achievements/Lessons)
    // ===============================================

    /**
     * Adds an EditText field and a remove button to the specified container.
     */
    private void addDynamicItemInput(LinearLayout container, String hint) {
        Context context = requireContext();
        LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);

        // EditText (takes up most space)
        TextInputEditText editText = new TextInputEditText(context);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        etParams.setMargins(0, 8, 8, 8);
        editText.setLayoutParams(etParams);
        editText.setHint(hint);

        // Remove Button (Small icon)
        ImageView removeButton = new ImageView(context);
        removeButton.setImageResource(R.drawable.outline_delete_24); // You need a delete icon drawable
        removeButton.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 8, 0, 8);
        removeButton.setLayoutParams(btnParams);

        // Set listener to remove the row
        removeButton.setOnClickListener(v -> container.removeView(rowLayout));

        rowLayout.addView(editText);
        rowLayout.addView(removeButton);
        container.addView(rowLayout);
    }

    /**
     * Populates the dynamic container with existing data.
     */
    private void populateDynamicList(LinearLayout container, ArrayList<String> items, String hint) {
        if (items != null) {
            for (String item : items) {
                if (!item.trim().isEmpty()) {
                    addDynamicItemInput(container, hint, item);
                }
            }
        }
    }

    // Overloaded helper for populating data
    private void addDynamicItemInput(LinearLayout container, String hint, String initialValue) {
        addDynamicItemInput(container, hint);
        // Find the last EditText added and set its text
        View lastRow = container.getChildAt(container.getChildCount() - 1);
        if (lastRow instanceof LinearLayout) {
            View editText = ((LinearLayout) lastRow).getChildAt(0);
            if (editText instanceof TextInputEditText) {
                ((TextInputEditText) editText).setText(initialValue);
            }
        }
    }

    /**
     * Extracts all non-empty text values from the TextInputEditTexts within the container.
     */
    private ArrayList<String> getItemsFromContainer(LinearLayout container) {
        ArrayList<String> items = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View childRow = container.getChildAt(i);
            if (childRow instanceof LinearLayout) {
                // The EditText is the first child (index 0) of the row LinearLayout
                View editText = ((LinearLayout) childRow).getChildAt(0);
                if (editText instanceof TextInputEditText) {
                    String text = ((TextInputEditText) editText).getText().toString().trim();
                    if (!text.isEmpty()) {
                        items.add(text);
                    }
                }
            }
        }
        return items;
    }

    /**
     * Finds the index of the selected avatar name for setting the RecyclerView position.
     */
    private int getAvatarIndexFromName(String avatarName) {
        if (avatarName == null || avatarName.isEmpty()) return 0;
        try {
            // avatarName is like "avatar_5". We need index 4.
            String[] parts = avatarName.split("_");
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]) - 1;
            }
        } catch (Exception ignored) {}
        return 0; // Default index
    }
}