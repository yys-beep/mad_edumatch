package com.example.mad_edumatch.authentication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity implements AvatarAdapter.AvatarClickListener {

    private EditText etName, etEmail, etPassword, etAcademicLevel;
    private RadioGroup rgRole;
    private LinearLayout layoutStudentFields, layoutTutorFields;
    private Button btnRegister;
    private TextView tvLoginLink;

    // REMOVED: private MaterialButton btnLanguageSwitch; (Old button caused crash)

    private RecyclerView rvAvatarSelect;
    private FirebaseAuth mAuth;
    private String selectedAvatarName = AvatarManager.getAvatarName(0);

    // New Buttons
    private Button btnRegEn, btnRegBm, btnRegZh;
    private final int SELECTED_COLOR = Color.parseColor("#021289");
    private final int UNSELECTED_COLOR = Color.parseColor("#AAAAAA");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Load Language BEFORE setContentView
        loadLocale();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_register);

        // 2. Init Language Buttons
        btnRegEn = findViewById(R.id.btnRegLangEn);
        btnRegBm = findViewById(R.id.btnRegLangBm);
        btnRegZh = findViewById(R.id.btnRegLangZh);

        // 3. Set Listeners
        btnRegEn.setOnClickListener(v -> setLocale("en"));
        btnRegBm.setOnClickListener(v -> setLocale("ms"));
        btnRegZh.setOnClickListener(v -> setLocale("zh"));

        // 4. FIX: Load the correct color based on saved preference
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String currentLang = prefs.getString("My_Lang", "en");
        updateColors(currentLang);

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

        // Initialize Avatar RecyclerView
        rvAvatarSelect = findViewById(R.id.rvAvatarSelect);
        rvAvatarSelect.setLayoutManager(new GridLayoutManager(this, 5));
        rvAvatarSelect.setAdapter(new AvatarAdapter(AvatarManager.AVATAR_DRAWABLES, this));
        rvAvatarSelect.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int padding = -5;
                outRect.left = padding;
                outRect.right = padding;
                outRect.top = padding;
                outRect.bottom = padding;
            }
        });

        // Auto-scroll logic
        final ScrollView scrollView = findViewById(R.id.registerScrollView);
        View.OnFocusChangeListener autoScrollListener = (v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() -> {
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    int yPos = location[1];
                    scrollView.smoothScrollBy(0, yPos - 200);
                }, 300);
            }
        };

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

        // REMOVED: btnLanguageSwitch listener (This was causing the crash)
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", lang);
        editor.apply();

        recreate(); // Restart Activity
    }

    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "en");
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
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
            Toast.makeText(this, getString(R.string.toast_select_role), Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rbRole = findViewById(selectedRoleId);
        // Note: rbRole.getText() might be translated (e.g., "Pelajar"), so handle roles carefully.
        // It is safer to rely on the ID to determine the role for the database.
        String roleStr = (selectedRoleId == R.id.rbStudent) ? "Student" : "Tutor";

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.toast_fill_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown";
                        Toast.makeText(this, getString(R.string.toast_reg_failed, error), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = mAuth.getCurrentUser().getUid();
                    User newUser = new User(uid, name, email, roleStr, registerTime, selectedAvatarName);

                    FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("Users")
                            .child(uid)
                            .setValue(newUser)
                            .addOnCompleteListener(dbTask -> {
                                if (dbTask.isSuccessful()) {
                                    if (roleStr.equalsIgnoreCase("Student")) {
                                        createStudentProfile(uid, name, email, registerTime, selectedAvatarName);
                                    } else {
                                        createTutorProfile(uid, name, email, registerTime, selectedAvatarName);
                                    }
                                } else {
                                    Toast.makeText(this, getString(R.string.toast_primary_fail), Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private void createTutorProfile(String uid, String name, String email, long registerTime, String avatarUrl) {
        String contact = "N/A";
        String fee = "N/A";
        String area = "N/A";
        String qualification = "";
        String description = "";

        TutorProfile profile = new TutorProfile(
                uid, name, email, contact,
                new ArrayList<>(),
                fee, area, qualification,
                new ArrayList<>(),
                new ArrayList<>(),
                description,
                registerTime,
                avatarUrl
        );

        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_profiles")
                .child(uid)
                .setValue(profile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, getString(R.string.toast_reg_success), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.toast_profile_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
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
                avatarUrl,
                new ArrayList<>(),
                new ArrayList<>()
        );

        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_profiles")
                .child(uid)
                .setValue(profile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, getString(R.string.toast_reg_success), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.toast_profile_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateColors(String lang) {
        btnRegEn.setTextColor(UNSELECTED_COLOR);
        btnRegBm.setTextColor(UNSELECTED_COLOR);
        btnRegZh.setTextColor(UNSELECTED_COLOR);

        if (lang.equals("en")) btnRegEn.setTextColor(SELECTED_COLOR);
        else if (lang.equals("ms")) btnRegBm.setTextColor(SELECTED_COLOR);
        else if (lang.equals("zh")) btnRegZh.setTextColor(SELECTED_COLOR);
    }
}