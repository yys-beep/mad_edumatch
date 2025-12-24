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
import com.example.mad_edumatch.chat.ChatListFragment; // Import ChatListFragment
import com.example.mad_edumatch.helper.UserViewModel;
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

public class HomeActivity extends AppCompatActivity {

    private Fragment currentFragment;
    private UserViewModel userViewModel;
    private BottomNavigationView bottomNav;
    private TextView tvAppTitle;
    private ImageButton btnLogout;
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
            } else if (itemId == R.id.nav_chat) {
                // --- NEW: Load Chat List Fragment ---
                fragmentToLoad = new ChatListFragment();
            } else if (itemId == R.id.nav_qna) {
                fragmentToLoad = new QnaForumFragment();
            }

            if (fragmentToLoad != null) {
                if (currentFragment == null || !currentFragment.getClass().equals(fragmentToLoad.getClass())) {
                    loadFragment(fragmentToLoad, itemId);
                }
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment, int itemId) {
        currentFragment = fragment;


            tvAppTitle.setText("EduMatch");

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

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

        chatRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("chatlist")
                .child(currentUid);

        chatRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                int unreadCount = 0;

                // Loop through all chats to count unread ones
                for (com.google.firebase.database.DataSnapshot data : snapshot.getChildren()) {
                    // Check if "isSeen" exists and is false
                    if (data.hasChild("isSeen") &&
                            Boolean.FALSE.equals(data.child("isSeen").getValue(Boolean.class))) {
                        unreadCount++;
                    }
                }

                // Update UI
                if (unreadCount > 0) {
                    var badge = bottomNav.getOrCreateBadge(R.id.nav_chat);
                    badge.setVisible(true);
                    badge.setNumber(unreadCount);
                } else {
                    bottomNav.removeBadge(R.id.nav_chat);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}