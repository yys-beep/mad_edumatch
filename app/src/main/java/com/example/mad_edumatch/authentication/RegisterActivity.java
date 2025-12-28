package com.example.mad_edumatch.authentication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.StudentProfile;
import com.example.mad_edumatch.firebaseModels.TutorProfile;
import com.example.mad_edumatch.firebaseModels.User;
import com.example.mad_edumatch.helper.AvatarManager;
import com.example.mad_edumatch.recycleAdapters.AvatarAdapter;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity implements AvatarAdapter.AvatarClickListener {

    private EditText etName, etEmail, etPassword, etAcademicLevel;
    private RadioGroup rgRole;
    private LinearLayout layoutStudentFields, layoutTutorFields;
    private Button btnRegister;
    private TextView tvLoginLink;
    private ImageButton btnBack;
    private RecyclerView rvAvatarSelect;
    private FirebaseAuth mAuth;
    private String selectedAvatarName = AvatarManager.getAvatarName(0); // Default avatar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_register);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etAcademicLevel = findViewById(R.id.etAcademicLevel);
        rgRole = findViewById(R.id.rgRole);
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        layoutTutorFields = findViewById(R.id.layoutTutorFields);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        btnBack = findViewById(R.id.btnBack);

        // Initialize Avatar RecyclerView
        rvAvatarSelect = findViewById(R.id.rvAvatarSelect);
        rvAvatarSelect.setLayoutManager(new GridLayoutManager(this, 5));
        rvAvatarSelect.setAdapter(new AvatarAdapter(AvatarManager.AVATAR_DRAWABLES, this));
        rvAvatarSelect.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int padding = -5; // Increase this number to make the avatars smaller
                outRect.left = padding;
                outRect.right = padding;
                outRect.top = padding;
                outRect.bottom = padding;
            }
        });

        final ScrollView scrollView = findViewById(R.id.registerScrollView);

        View.OnFocusChangeListener autoScrollListener = (v, hasFocus) -> {
            if (hasFocus) {
                // Wait for keyboard animation to start
                scrollView.postDelayed(() -> {
                    // Get the position of the focused EditText
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    int yPos = location[1];

                    // Scroll the view up so the EditText is near the top of the visible area
                    // Adjust the '200' value to control how high it scrolls
                    scrollView.smoothScrollBy(0, yPos - 200);
                }, 300);
            }
        };

        // Apply to all input fields
        etName.setOnFocusChangeListener(autoScrollListener);
        etEmail.setOnFocusChangeListener(autoScrollListener);
        etPassword.setOnFocusChangeListener(autoScrollListener);
        etAcademicLevel.setOnFocusChangeListener(autoScrollListener);
    }

    private void setupListeners() {
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbStudent) {
                layoutStudentFields.setVisibility(View.VISIBLE);
                layoutTutorFields.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbTutor) {
                layoutStudentFields.setVisibility(View.GONE);
                layoutTutorFields.setVisibility(View.VISIBLE);
            }
        });

        btnRegister.setOnClickListener(v -> registerUser());
        tvLoginLink.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    public void onAvatarSelected(int index) {
        selectedAvatarName = AvatarManager.getAvatarName(index);
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        long registerTime = System.currentTimeMillis();

        int selectedRoleId = rgRole.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rbRole = findViewById(selectedRoleId);
        String role = rbRole.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Create Auth User
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = mAuth.getCurrentUser().getUid();

                    // 2. Create User Object for "Users" node (Includes Avatar URL)
                    User newUser = new User(uid, name, email, role, registerTime, selectedAvatarName);

                    FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("Users")
                            .child(uid)
                            .setValue(newUser)
                            .addOnCompleteListener(dbTask -> {
                                if (dbTask.isSuccessful()) {
                                    // 3. Create Specific Profile (Also save avatar URL for consistency)
                                    if (role.equalsIgnoreCase("Student")) {
                                        createStudentProfile(uid, name, email, registerTime, selectedAvatarName);
                                    } else {
                                        createTutorProfile(uid, name, email, registerTime, selectedAvatarName);
                                    }
                                } else {
                                    Toast.makeText(this, "Failed to save primary user data", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private void createTutorProfile(String uid, String name, String email, long registerTime, String avatarUrl) {
        // Defaults
        String contact = "N/A";
        String fee = "N/A";
        String area = "N/A";
        String qualification = "";
        String description = "";

        TutorProfile profile = new TutorProfile(
                uid, name, email, contact,
                new ArrayList<>(), // Subjects
                fee, area, qualification,
                new ArrayList<>(), // Achievements
                new ArrayList<>(), // Experience
                description,
                registerTime,
                avatarUrl // Save avatar URL
        );

        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_profiles")
                .child(uid)
                .setValue(profile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registration Successful! Complete your profile.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createStudentProfile(String uid, String name, String email, long registerTime, String avatarUrl) {
        String academicLevel = etAcademicLevel.getText().toString().trim();
        if(academicLevel.isEmpty()) academicLevel = "N/A";

        String contact = "N/A";
        int age = 0;
        String description = "";

        StudentProfile profile = new StudentProfile(
                uid, name, email, contact,
                age, academicLevel, description,
                registerTime,
                avatarUrl, // Save avatar URL
                new ArrayList<>(), // Achievements
                new ArrayList<>()  // Free Lessons
        );

        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_profiles")
                .child(uid)
                .setValue(profile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registration Successful! Complete your profile.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}