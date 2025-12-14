package com.example.mad_edumatch;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.authentication.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_splash); // your splash XML

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}
