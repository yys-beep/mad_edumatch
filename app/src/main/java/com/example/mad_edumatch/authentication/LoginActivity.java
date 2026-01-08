package com.example.mad_edumatch.authentication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mad_edumatch.HomeActivity;
import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private Button btnEn, btnBm, btnZh;

    private final int SELECTED_COLOR = Color.parseColor("#021289"); // Blue
    private final int UNSELECTED_COLOR = Color.parseColor("#AAAAAA"); // Grey

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Load Language BEFORE setContentView (Crucial for translation)
        loadLocale();

        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.auth_activity_login);

        // 2. Initialize Views
        btnEn = findViewById(R.id.btnLangEn);
        btnBm = findViewById(R.id.btnLangBm);
        btnZh = findViewById(R.id.btnLangZh);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);

        // 3. Setup Language Listeners
        btnEn.setOnClickListener(v -> setLocale("en"));
        btnBm.setOnClickListener(v -> setLocale("ms"));
        btnZh.setOnClickListener(v -> setLocale("zh"));

        // 4. FIX: Get the ACTUAL saved language so the correct button lights up
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String currentLang = prefs.getString("My_Lang", "en");
        updateLanguageColors(currentLang);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users");

        setupListeners();
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> loginUser());

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }

    // ---------------- LANGUAGE LOGIC ----------------
    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Save preference
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", lang);
        editor.apply();

        // Restart activity to apply changes
        recreate();
    }

    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "en"); // Default English

        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void updateLanguageColors(String langCode) {
        // Reset all to Grey first
        btnEn.setTextColor(UNSELECTED_COLOR);
        btnBm.setTextColor(UNSELECTED_COLOR);
        btnZh.setTextColor(UNSELECTED_COLOR);

        // Set the selected one to Blue
        if (langCode.equals("en")) {
            btnEn.setTextColor(SELECTED_COLOR);
        } else if (langCode.equals("ms")) {
            btnBm.setTextColor(SELECTED_COLOR);
        } else if (langCode.equals("zh")) {
            btnZh.setTextColor(SELECTED_COLOR);
        }
    }
    // ------------------------------------------------

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.toast_enter_creds), Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown";
                        Toast.makeText(this, getString(R.string.toast_login_failed, errorMsg), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) {
                        fetchUserInfo(currentUser.getUid());
                    }
                });
    }

    private void fetchUserInfo(String uid) {
        usersRef.child(uid).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(this, getString(R.string.toast_fetch_error), Toast.LENGTH_SHORT).show();
                return;
            }

            User user = task.getResult().getValue(User.class);
            if (user != null) {
                com.example.mad_edumatch.helper.CurrentUser.getInstance().setUid(uid);
                com.example.mad_edumatch.helper.CurrentUser.getInstance().setName(user.getName());

                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                intent.putExtra("userName", user.getName());
                intent.putExtra("userRole", user.getRole());

                startActivity(intent);
                finish();
                Toast.makeText(this, getString(R.string.toast_welcome_user, user.getName()), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.toast_data_corrupt), Toast.LENGTH_SHORT).show();
            }
        });
    }
}