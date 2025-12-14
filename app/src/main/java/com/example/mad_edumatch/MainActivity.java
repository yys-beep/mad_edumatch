package com.example.mad_edumatch;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // You can have a simple layout or leave it blank
        setContentView(R.layout.app_activity_main);

        // Start SplashActivity
        Intent intent = new Intent(MainActivity.this, com.example.mad_edumatch.SplashActivity.class);
        startActivity(intent);
        finish(); // Close MainActivity so back button doesn't return here
    }
}
