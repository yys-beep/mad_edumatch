package com.example.mad_edumatch;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.mad_edumatch.authentication.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Install System Splash Screen
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_splash);

        // --- ERROR WAS HERE ---
        // REMOVED: splashScreen.setKeepOnScreenCondition(() -> true );
        // REASON: We want the system icon to disappear so your custom XML can be seen.

        // 2. Set your custom logo (optional, if not set in XML)
        ImageView splashLogo = findViewById(R.id.splash_logo);
        if (splashLogo != null) {
            splashLogo.setImageResource(R.drawable.ic_splash_logo);
        }

        // 3. Wait 2 seconds, then move to Login
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}