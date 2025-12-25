package com.example.mad_edumatch;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * SplashActivity - Application splash screen with animation and Firebase authentication check
 * Updated: 2025-12-25 20:19:24 UTC
 * Last Modified By: bohan-guo
 */
public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private static final int SPLASH_DURATION = 3000; // 3 seconds
    private ImageView splashLogo;
    private TextView splashText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        splashLogo = findViewById(R.id.splash_logo);
        splashText = findViewById(R.id.splash_text);

        // Load and start animations
        startAnimations();

        // Check Firebase authentication and navigate accordingly
        new Handler().postDelayed(this::checkAuthenticationAndNavigate, SPLASH_DURATION);
    }

    /**
     * Starts animation for splash screen elements
     */
    private void startAnimations() {
        if (splashLogo != null) {
            Animation fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            fadeInAnimation.setDuration(1500);
            splashLogo.startAnimation(fadeInAnimation);
        }

        if (splashText != null) {
            Animation slideInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
            slideInAnimation.setDuration(1500);
            slideInAnimation.setStartOffset(500);
            splashText.startAnimation(slideInAnimation);
        }
    }

    /**
     * Checks Firebase authentication status and navigates to appropriate activity
     */
    private void checkAuthenticationAndNavigate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        Intent intent;
        if (currentUser != null) {
            // User is logged in, navigate to main activity
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // User is not logged in, navigate to login activity
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
