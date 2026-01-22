package com.example.mad_edumatch;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.app_activity_main);

       
        Intent intent = new Intent(MainActivity.this, com.example.mad_edumatch.SplashActivity.class);
        startActivity(intent);
        finish(); 
    }
}
