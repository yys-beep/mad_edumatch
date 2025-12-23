package com.example.mad_edumatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mad_edumatch.authentication.LoginActivity;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. LOAD SAVED LANGUAGE (MUST be before super.onCreate)
        android.content.SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "en"); // Default to English

        java.util.Locale locale = new java.util.Locale(language);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // 2. Now proceed with the splash logic
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_splash);

        new Handler().postDelayed(() -> {
            // Change this to MainActivity or LoginActivity depending on your flow
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}