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

    // Input fields
    private EditText etUsername, etEmail, etContact, etAge, etAcademicLevel, etDescription;

    // Dynamic list containers
    private LinearLayout layoutAchievementsContainer; // ONLY Achievements UI
    private Button btnAddAchievement;

    // Data Preservation (To keep lessons without showing them in UI)
    private ArrayList<String> preservedLessonsList = new ArrayList<>();

    // Avatar Selection
    private RecyclerView rvEditAvatarSelect;
    private AvatarAdapter avatarAdapter;
    private String selectedAvatarName = AvatarManager.getAvatarName(0);

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
        // NOTE: layoutLessonsContainer is NOT bound because it is not in XML

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
                            loadBasicUserFallback();
                            return;
                        }

                        StudentProfile profile = snapshot.getValue(StudentProfile.class);
                        if (profile != null) {
                            // 1. Basic Fields
                            etUsername.setText(profile.getUsername());
                            etEmail.setText(profile.getEmail());

                            String contact = profile.getContact();
                            etContact.setText((contact != null && !contact.equals("N/A")) ? contact : "");

                            if (profile.getAge() > 0) etAge.setText(String.valueOf(profile.getAge()));

                            String academic = profile.getAcademicLevel();
                            etAcademicLevel.setText((academic != null && !academic.equals("N/A")) ? academic : "");

                            String desc = profile.getDescription();
                            etDescription.setText((desc != null && !desc.equals("-")) ? desc : "");

                            // 2. Avatar
                            selectedAvatarName = profile.getProfileImageUrl();
                            if (selectedAvatarName == null || selectedAvatarName.isEmpty()) {
                                selectedAvatarName = "avatar_1";
                            }
                            if(avatarAdapter != null) {
                                avatarAdapter.setSelectedPosition(getAvatarIndexFromName(selectedAvatarName));
                            }

                            // 3. Dynamic Lists - Achievements (UI)
                            populateDynamicList(layoutAchievementsContainer, profile.getAchievements(), "Achievement");

                            // 4. Data Preservation - Lessons (No UI)
                            // We save them to a list so we can send them back when saving
                            preservedLessonsList.clear();
                            if (profile.getParticipatedFreeLessons() != null) {
                                preservedLessonsList.addAll(profile.getParticipatedFreeLessons());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Failed to load profile.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

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

        int tempAge = 0;
        try {
            tempAge = Integer.parseInt(etAge.getText().toString().trim());
        } catch (NumberFormatException ignored) {}
        final int finalAge = tempAge;

        String academicLevel = etAcademicLevel.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // 1. Get Achievements from UI
        ArrayList<String> achievementsList = getItemsFromContainer(layoutAchievementsContainer);

        // 2. Use PRESERVED Lessons (Don't try to get from UI)
        ArrayList<String> participatedLessonsList = new ArrayList<>(preservedLessonsList);

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
                    long registerTime = existingProfile != null ? existingProfile.getRegisterTime() : System.currentTimeMillis();
                    String currentAvatar = existingProfile != null ? existingProfile.getProfileImageUrl() : AvatarManager.getAvatarName(0);
                    String avatarToSave = selectedAvatarName != null ? selectedAvatarName : currentAvatar;

                    StudentProfile updatedProfile = new StudentProfile(
                            studentId,
                            username,
                            email,
                            contact,
                            finalAge,
                            academicLevel,
                            description,
                            registerTime,
                            avatarToSave,
                            achievementsList,
                            participatedLessonsList // Saving the preserved data back
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

    private void updateMainUserNode(String newName, String newAvatar) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("profileImageUrl", newAvatar);
        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users").child(studentId).updateChildren(updates);
    }

    // --- UTILITIES ---

    private void addDynamicItemInput(LinearLayout container, String hint) {
        if(container == null) return; // Safety check

        Context context = requireContext();
        LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextInputEditText editText = new TextInputEditText(context);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        etParams.setMargins(0, 8, 8, 8);
        editText.setLayoutParams(etParams);
        editText.setHint(hint);

        ImageView removeButton = new ImageView(context);
        removeButton.setImageResource(R.drawable.outline_delete_24);
        removeButton.setPadding(16, 16, 16, 16);
        removeButton.setOnClickListener(v -> container.removeView(rowLayout));

        rowLayout.addView(editText);
        rowLayout.addView(removeButton);
        container.addView(rowLayout);
    }

    private void populateDynamicList(LinearLayout container, ArrayList<String> items, String hint) {
        if (items != null && container != null) {
            for (String item : items) {
                if (!item.trim().isEmpty()) {
                    addDynamicItemInput(container, hint, item);
                }
            }
        }
    }

    private void addDynamicItemInput(LinearLayout container, String hint, String initialValue) {
        addDynamicItemInput(container, hint);
        if(container == null) return;
        View lastRow = container.getChildAt(container.getChildCount() - 1);
        if (lastRow instanceof LinearLayout) {
            View editText = ((LinearLayout) lastRow).getChildAt(0);
            if (editText instanceof TextInputEditText) {
                ((TextInputEditText) editText).setText(initialValue);
            }
        }
    }

    private ArrayList<String> getItemsFromContainer(LinearLayout container) {
        ArrayList<String> items = new ArrayList<>();
        // SAFETY CHECK: This is what prevents the crash if container is null
        if (container == null) return items;

        for (int i = 0; i < container.getChildCount(); i++) {
            View childRow = container.getChildAt(i);
            if (childRow instanceof LinearLayout) {
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

    private int getAvatarIndexFromName(String avatarName) {
        if (avatarName == null || avatarName.isEmpty()) return 0;
        try {
            String[] parts = avatarName.split("_");
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]) - 1;
            }
        } catch (Exception ignored) {}
        return 0;
    }
}