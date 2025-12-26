package com.example.mad_edumatch;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.mad_edumatch.chat.ChatListFragment; // Import ChatListFragment
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
    private DatabaseReference chatRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_main);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        String userName = getIntent().getStringExtra("userName");
        String userRole = getIntent().getStringExtra("userRole");
        userViewModel.setUserName(userName);
        userViewModel.setUserRole(userRole);

        tvAppTitle = findViewById(R.id.tvAppTitle);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNav = findViewById(R.id.bottom_navigation);

        BottomNavigationView innerNav = findViewById(R.id.bottomNavigationView);
        if (innerNav != null) bottomNav = innerNav;

        tvAppTitle.setText("EduMatch");

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        setupOnBackPressed();

        if (savedInstanceState == null) {
            loadFragment(getHomeFragmentForRole(userRole), R.id.nav_home);
        }

        setupBottomNavigation();
        setupBadgeListener();

        checkNotificationPermission();
        updateFCMToken();
        // Check if we arrived here from a Notification click
        if (getIntent().hasExtra("action_type")) {
            handleNotificationClick(getIntent());
        }
        // 1. Handle notification if the app was COMPLETELY CLOSED
        if (getIntent() != null && getIntent().hasExtra("action_type")) {
            handleNotificationClick(getIntent());
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // CRITICAL: Updates the activity with the new notification data

        // 2. Handle notification if the app was ALREADY OPEN (background)
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
                bottomNav.removeBadge(R.id.nav_notifications); // Clear badge immediately on click
            } else if (itemId == R.id.nav_chat) {
                // Load Chat List Fragment ---
                fragmentToLoad = new ChatListFragment();
            } else if (itemId == R.id.nav_qna) {
                fragmentToLoad = new QnaForumFragment();
            } else if (itemId == R.id.nav_settings) {
                // Settings logic (e.g., SettingsFragment or Activity)
                // For now, leaving it null or creating a placeholder
            }

            // PREVENT CRASH: Only load if fragment is NOT null and NOT the same as current
            if (fragmentToLoad != null) {
                // Check if we are already showing this fragment type to prevent "Duplicate ID" crashes
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
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss(); // Prevents crashes during rapid navigation

        if (bottomNav.getSelectedItemId() != itemId) {
            bottomNav.getMenu().findItem(itemId).setChecked(true);
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

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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

        // 1. CHAT BADGE LISTENER
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

        // 2. GENERAL NOTIFICATIONS BADGE LISTENER (Q&A, Kudos, etc.)
        DatabaseReference notifRef = db.getReference("notifications").child(currentUid);
        notifRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                int unreadNotifCount = 0;
                for (com.google.firebase.database.DataSnapshot data : snapshot.getChildren()) {
                    // We check 'isRead' field for general notifications
                    if (data.hasChild("isRead") && Boolean.FALSE.equals(data.child("isRead").getValue(Boolean.class))) {
                        unreadNotifCount++;
                    }
                }
                updateBottomNavBadge(R.id.nav_notifications, unreadNotifCount);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Helper to keep the UI logic dry
     */
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
                    .getReference("users")
                    .child(currentUid);

            // Save token so backend knows where to send push notifications
            userRef.child("fcmToken").setValue(token);
        });
    }

    private void handleNotificationClick(Intent intent) {
        String actionType = intent.getStringExtra("action_type");
        String sourceId = intent.getStringExtra("sourceId"); // Retrieve the Question ID

        if ("OPEN_QUESTION".equals(actionType) || "NEW_SOLUTION".equals(actionType)) {
                QnaDetailFragment fragment = new QnaDetailFragment();
                Bundle args = new Bundle();
                args.putString("answerId", sourceId);
                args.putString("sourceId", sourceId); // Pass as sourceId to match Fragment logic
                fragment.setArguments(args);
                loadFragment(fragment, R.id.nav_qna);
                bottomNav.setSelectedItemId(R.id.nav_qna);
        }
        else if ("OPEN_LESSON".equals(actionType)) {
            if (sourceId != null) {
                FreeLessonDetailFragment fragment = new FreeLessonDetailFragment();
                Bundle args = new Bundle();
                args.putString("lessonId", sourceId); // Pass the ID to the fragment
                fragment.setArguments(args);
                loadFragment(fragment, R.id.nav_home);
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        }
        else if ("OPEN_COMMENT".equals(actionType)) {
            QnaAnswerCommentFragment fragment = new QnaAnswerCommentFragment();
            Bundle args = new Bundle();
            args.putString("sourceId", sourceId); // Pass the answerId as sourceId
            fragment.setArguments(args);

            // Load into the Q&A section
            loadFragment(fragment, R.id.nav_qna);
            bottomNav.setSelectedItemId(R.id.nav_qna);
        }
        else if ("OPEN_CHAT".equals(actionType)) {
            String senderId = intent.getStringExtra("senderId");
            String listingId = intent.getStringExtra("sourceId");

            ChatDetailFragment fragment = new ChatDetailFragment();
            Bundle args = new Bundle();

            // Pass the senderId from the notification as the targetUserId for the chat
            args.putString("targetUserId", senderId);
            args.putString("listingId", listingId);

            // Optional: You can fetch the name in the fragment,
            // but passing a placeholder prevents empty headers
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