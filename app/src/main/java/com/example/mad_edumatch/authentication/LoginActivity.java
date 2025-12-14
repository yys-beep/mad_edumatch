package com.example.mad_edumatch.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mad_edumatch.HomeActivity;
import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.User;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_login);

        // Debug AppCheck is intentionally omitted here for brevity, assuming it's configured elsewhere.

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users");

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                return;
            }

            User user = task.getResult().getValue(User.class);
            if (user != null) {
                // Pass role and name to HomeActivity
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                intent.putExtra("userName", user.getName());
                intent.putExtra("userRole", user.getRole());

                // You can also pass the image URL if HomeActivity needs to display it immediately
                // intent.putExtra("userProfileImage", user.getProfileImageUrl());

                startActivity(intent);
                finish();
                Toast.makeText(this, "Welcome back, " + user.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "User data corrupt", Toast.LENGTH_SHORT).show();
            }
        });
    }
}