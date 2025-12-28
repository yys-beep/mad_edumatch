package com.example.mad_edumatch;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mad_edumatch.authentication.LoginActivity;
import com.example.mad_edumatch.chat.ChatDetailFragment;
import com.example.mad_edumatch.chat.ChatListFragment;
import com.example.mad_edumatch.freeLesson.FreeLessonDetailFragment;
import com.example.mad_edumatch.helper.UserViewModel;
import com.example.mad_edumatch.qna.QnaAnswerCommentFragment;
import com.example.mad_edumatch.qna.QnaDetailFragment;
import com.example.mad_edumatch.qna.QnaForumFragment;
import com.example.mad_edumatch.student.StudentHomeFragment;
import com.example.mad_edumatch.student.StudentNotificationFragment;
import com.example.mad_edumatch.student.StudentProfileFragment;
import com.example.mad_edumatch.tutor.TutorHomeFragment;
import com.example.mad_edumatch.tutor.TutorNotificationFragment;
import com.example.mad_edumatch.tutor.TutorProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class HomeActivity extends AppCompatActivity {

    private Fragment currentFragment;
    private UserViewModel userViewModel;
    private BottomNavigationView bottomNav;
    private TextView tvAppTitle;
    private ImageButton btnLogout;
    private ImageButton btnBack; // 1. ADD VARIABLE
    private DatabaseReference chatRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.content.SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "en");
        java.util.Locale locale = new java.util.Locale(language);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);

        // Status Bar Styling
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(android.graphics.Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        setContentView(R.layout.app_activity_main);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        String userName = getIntent().getStringExtra("userName");
        String userRole = getIntent().getStringExtra("userRole");
        userViewModel.setUserName(userName);
        userViewModel.setUserRole(userRole);

        // Initialize Views
        tvAppTitle = findViewById(R.id.tvAppTitle);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack); // 2. BIND VIEW
        bottomNav = findViewById(R.id.bottom_navigation);

        BottomNavigationView innerNav = findViewById(R.id.bottomNavigationView);
        if (innerNav != null) bottomNav = innerNav;

        tvAppTitle.setText("EduMatch");

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        // 3. SET BACK BUTTON LISTENER (Behaves exactly like hardware back button)
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        setupOnBackPressed();

        // 4. ADD LISTENER FOR FRAGMENT CHANGES
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment visibleFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (visibleFragment != null) {
                currentFragment = visibleFragment;
                // Update the toolbar (Show/Hide Back button) whenever the fragment changes
                updateToolbarUI(visibleFragment);
            }
        });

        if (savedInstanceState == null) {
            loadFragment(getHomeFragmentForRole(userRole), R.id.nav_home);
        }

        setupBottomNavigation();
        setupBadgeListener();
        checkNotificationPermission();
        updateFCMToken();

        if (getIntent().hasExtra("action_type")) {
            handleNotificationClick(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.hasExtra("action_type")) {
            handleNotificationClick(intent);
        }
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragmentToLoad = null;
            String role = userViewModel.getUserRole();
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragmentToLoad = getHomeFragmentForRole(role);
            } else if (itemId == R.id.nav_notifications) {
                fragmentToLoad = getNotificationFragmentForRole(role);
                // bottomNav.removeBadge(R.id.nav_notifications); // <--- DELETE THIS LINE
            } else if (itemId == R.id.nav_chat) {
                fragmentToLoad = new ChatListFragment();
            } else if (itemId == R.id.nav_qna) {
                fragmentToLoad = new QnaForumFragment();
            }

            if (fragmentToLoad != null) {
                if (currentFragment != null && currentFragment.getClass().equals(fragmentToLoad.getClass())) {
                    return true;
                }
                loadFragment(fragmentToLoad, itemId);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment, int itemId) {
        if (fragment == null) return;
        currentFragment = fragment;

        // 5. UPDATE UI WHEN LOADING A FRAGMENT
        updateToolbarUI(fragment);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();

        if (bottomNav.getSelectedItemId() != itemId) {
            bottomNav.getMenu().findItem(itemId).setChecked(true);
        }
    }

    // 6. HELPER LOGIC: Toggle Back vs Logout
    private void updateToolbarUI(Fragment fragment) {
        // Define which fragments are "Root" (Main Tabs) where Back button should be HIDDEN
        boolean isRootFragment =
                fragment instanceof StudentHomeFragment ||
                        fragment instanceof TutorHomeFragment ||
                        fragment instanceof StudentNotificationFragment ||
                        fragment instanceof TutorNotificationFragment ||
                        fragment instanceof ChatListFragment ||
                        fragment instanceof QnaForumFragment;

        if (isRootFragment) {
            // Main Tab: Show Logout, Hide Back
            btnBack.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
            tvAppTitle.setText("EduMatch");
        } else {
            // Detail Page (e.g. Profile, Chat Detail): Show Back, Hide Logout
            btnBack.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.GONE);
        }
    }

    private void setupOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }

                String role = userViewModel.getUserRole();
                boolean isTutor = "Tutor".equalsIgnoreCase(role);
                boolean isHome = isTutor ? (currentFragment instanceof TutorHomeFragment)
                        : (currentFragment instanceof StudentHomeFragment);

                if (!isHome) {
                    loadFragment(getHomeFragmentForRole(role), R.id.nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // ... (rest of your methods: showLogoutConfirmation, getHomeFragmentForRole, badge listeners, etc.) ...

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);

                    // --- THIS IS THE FIX ---
                    // These flags clear the old activity stack so Login starts fresh
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    // -----------------------

                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Fragment getHomeFragmentForRole(String role) {
        Bundle args = new Bundle();
        args.putString("userName", userViewModel.getUserName());
        return "Tutor".equalsIgnoreCase(role) ? new TutorHomeFragment() : new StudentHomeFragment();
    }

    private Fragment getProfileFragmentForRole(String role) {
        return "Tutor".equalsIgnoreCase(role) ? new TutorProfileFragment() : new StudentProfileFragment();
    }

    private Fragment getNotificationFragmentForRole(String role) {
        return "Tutor".equalsIgnoreCase(role) ? new TutorNotificationFragment() : new StudentNotificationFragment();
    }

    private void setupBadgeListener() {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        FirebaseDatabase db = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app");

        chatRef = db.getReference("chatlist").child(currentUid);
        chatRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                int unreadChatCount = 0;
                for (com.google.firebase.database.DataSnapshot data : snapshot.getChildren()) {
                    if (data.hasChild("isSeen") && Boolean.FALSE.equals(data.child("isSeen").getValue(Boolean.class))) {
                        unreadChatCount++;
                    }
                }
                updateBottomNavBadge(R.id.nav_chat, unreadChatCount);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        DatabaseReference notifRef = db.getReference("notifications").child(currentUid);
        notifRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                int unreadNotifCount = 0;
                for (com.google.firebase.database.DataSnapshot data : snapshot.getChildren()) {
                    if (data.hasChild("isRead") && Boolean.FALSE.equals(data.child("isRead").getValue(Boolean.class))) {
                        unreadNotifCount++;
                    }
                }
                updateBottomNavBadge(R.id.nav_notifications, unreadNotifCount);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateBottomNavBadge(int menuId, int count) {
        if (count > 0) {
            var badge = bottomNav.getOrCreateBadge(menuId);
            badge.setVisible(true);
            badge.setNumber(count);
        } else {
            bottomNav.removeBadge(menuId);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void updateFCMToken() {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String token = task.getResult();
            DatabaseReference userRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("users").child(currentUid);
            userRef.child("fcmToken").setValue(token);
        });
    }

    private void handleNotificationClick(Intent intent) {
        String actionType = intent.getStringExtra("action_type");
        String sourceId = intent.getStringExtra("sourceId");

        if ("OPEN_QUESTION".equals(actionType) || "NEW_SOLUTION".equals(actionType)) {
            QnaDetailFragment fragment = new QnaDetailFragment();
            Bundle args = new Bundle();
            args.putString("answerId", sourceId);
            args.putString("sourceId", sourceId);
            fragment.setArguments(args);
            loadFragment(fragment, R.id.nav_qna);
            bottomNav.setSelectedItemId(R.id.nav_qna);
        }
        else if ("OPEN_LESSON".equals(actionType)) {
            if (sourceId != null) {
                FreeLessonDetailFragment fragment = new FreeLessonDetailFragment();
                Bundle args = new Bundle();
                args.putString("lessonId", sourceId);
                fragment.setArguments(args);
                loadFragment(fragment, R.id.nav_home);
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        }
        else if ("OPEN_COMMENT".equals(actionType)) {
            QnaAnswerCommentFragment fragment = new QnaAnswerCommentFragment();
            Bundle args = new Bundle();
            args.putString("sourceId", sourceId);
            fragment.setArguments(args);
            loadFragment(fragment, R.id.nav_qna);
            bottomNav.setSelectedItemId(R.id.nav_qna);
        }
        else if ("OPEN_CHAT".equals(actionType)) {
            String senderId = intent.getStringExtra("senderId");
            String listingId = intent.getStringExtra("sourceId");
            ChatDetailFragment fragment = new ChatDetailFragment();
            Bundle args = new Bundle();
            args.putString("targetUserId", senderId);
            args.putString("listingId", listingId);
            args.putString("targetUserName", "User");
            fragment.setArguments(args);
            loadFragment(fragment, R.id.nav_chat);
            bottomNav.setSelectedItemId(R.id.nav_chat);
        }
        else if ("KUDOS".equals(actionType)) {
            FreeLessonDetailFragment fragment = new FreeLessonDetailFragment();
            Bundle args = new Bundle();
            args.putString("lessonId", sourceId);
            fragment.setArguments(args);
            loadFragment(fragment, R.id.nav_home);
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}