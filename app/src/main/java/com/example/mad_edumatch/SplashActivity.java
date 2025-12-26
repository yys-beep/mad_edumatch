package com.example.mad_edumatch;

import android.content. Intent;
import android.os. Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation. AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.appcompat.app. AppCompatActivity;

import com.example.mad_edumatch.authentication.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 2500; // 2.5 seconds
    private ImageView splashLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        splashLogo = findViewById(R.id.splash_logo);

        // Start animations
        startSplashAnimations();

        // Delay and transition to next screen
        new Handler(Looper.getMainLooper()).postDelayed(this::transitionToNextScreen, SPLASH_DURATION);
    }

    /**
     * Start animations for splash screen elements
     */
    private void startSplashAnimations() {
        if (splashLogo != null) {
            // Logo scale and fade in animation
            AnimationSet logoAnimation = createLogoAnimation();
            splashLogo.startAnimation(logoAnimation);
        }
    }

    /**
     * Create animation set for logo (scale + fade in)
     */
    private AnimationSet createLogoAnimation() {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setDuration(1500);

        // Scale animation
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.8f, 1.1f,  // scaleX:  from 0.8 to 1.1
                0.8f, 1.1f,  // scaleY: from 0.8 to 1.1
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(1500);

        // Fade in animation
        AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
        alphaAnimation.setDuration(1500);

        animationSet. addAnimation(scaleAnimation);
        animationSet.addAnimation(alphaAnimation);

        return animationSet;
    }

    /**
     * Transition to the next screen based on authentication state
     */
    private void transitionToNextScreen() {
        // Fade out animation
        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(500);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Check if user is already logged in
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                Intent intent;

                if (currentUser != null) {
                    // User is logged in, go to HomeActivity
                    intent = new Intent(SplashActivity.this, HomeActivity. class);
                } else {
                    // User is not logged in, go to LoginActivity
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }

                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim. fade_out);
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        findViewById(R.id.splash_container).startAnimation(fadeOut);
    }
}