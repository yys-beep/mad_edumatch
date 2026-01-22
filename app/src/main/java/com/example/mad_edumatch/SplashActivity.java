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

        ImageView splashLogo = findViewById(R.id.splash_logo);
        if (splashLogo != null) {
            splashLogo.setImageResource(R.drawable.ic_splash_logo);
        }

    
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}