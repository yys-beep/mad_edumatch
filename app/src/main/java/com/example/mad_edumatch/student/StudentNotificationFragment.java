package com.example.mad_edumatch.student;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mad_edumatch.R;
import com.example.mad_edumatch.chat.ChatDetailFragment;
import com.example.mad_edumatch.firebaseModels.Notification;
import com.example.mad_edumatch.freeLesson.FreeLessonDetailFragment;
import com.example.mad_edumatch.qna.QnaAnswerCommentFragment;
import com.example.mad_edumatch.qna.QnaDetailFragment;
import com.example.mad_edumatch.recycleAdapters.NotificationAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentNotificationFragment extends Fragment {
    private TextView btnClearAll;
    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private TextView tvEmptyState;
    private static final String DB_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        return inflater.inflate(R.layout.student_fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        btnClearAll = view.findViewById(R.id.btnClearAll);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificationAdapter(notificationList, "Student", notification -> {
            markNotificationAsRead(notification.getId());

            String actionType = notification.getAction_type() != null ? notification.getAction_type().toUpperCase() : "";
            String targetId = notification.getSourceId(); // Standardized key

            if (actionType == null || targetId == null) return;

            if (actionType.equals("OPEN_LESSON") || actionType.equals("KUDOS")) {
                FreeLessonDetailFragment fragment = new FreeLessonDetailFragment();
                Bundle args = new Bundle();
                args.putString("sourceId", targetId);
                fragment.setArguments(args);
                navigateTo(fragment);
            }
            else if (actionType.equals("OPEN_QUESTION") || actionType.equals("OPEN_QUESTION_DETAIL") || actionType.equals("MY_QUESTIONS_NOTIF")) {
                FirebaseDatabase.getInstance(DB_URL).getReference("forum_questions")
                        .child(targetId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!isAdded()) return;

                                // 2. CHECK: If the question is gone, show toast and DON'T navigate
                                if (!snapshot.exists()) {
                                    Toast.makeText(getContext(), "This question has been deleted.", Toast.LENGTH_SHORT).show();

                                    // Optional: Delete the "dead" notification so it's not clicked again
                                    String uid = FirebaseAuth.getInstance().getUid();
                                    if (uid != null) {
                                        FirebaseDatabase.getInstance(DB_URL).getReference("notifications")
                                                .child(uid).child(notification.getId()).removeValue();
                                    }
                                } else {
                                    // 3. SUCCESS: Navigate only if the data exists
                                    QnaDetailFragment fragment = new QnaDetailFragment();
                                    Bundle args = new Bundle();
                                    args.putString("sourceId", targetId);
                                    fragment.setArguments(args);
                                    navigateTo(fragment);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }
            else if (actionType.equals("OPEN_COMMENT") || "NEW_SOLUTION".equals(actionType)) {                // We must check if the ANSWER still exists
                FirebaseDatabase.getInstance(DB_URL).getReference("forum_answers")
                        .child(targetId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!isAdded()) return;

                                if (!snapshot.exists()) {
                                    // If the solution was deleted, don't navigate
                                    Toast.makeText(getContext(), "This solution was deleted.", Toast.LENGTH_SHORT).show();
                                    // Clean up the dead notification
                                    FirebaseDatabase.getInstance(DB_URL).getReference("notifications")
                                            .child(FirebaseAuth.getInstance().getUid()).child(notification.getId()).removeValue();
                                } else {
                                    // SOLUTION EXISTS: Navigate to comment fragment
                                    QnaAnswerCommentFragment fragment = new QnaAnswerCommentFragment();
                                    Bundle args = new Bundle();
                                    args.putString("answerId", targetId); // Pass the answerId
                                    fragment.setArguments(args);
                                    navigateTo(fragment);
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }
            else if ("OPEN_CHAT".equals(actionType)) {
                // Navigation for "Listing Inquiry"
                ChatDetailFragment fragment = new ChatDetailFragment();
                Bundle args = new Bundle();
                args.putString("targetUserId", notification.getSenderId());
                args.putString("listingId", notification.getSourceId());
                fragment.setArguments(args);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        rvNotifications.setAdapter(adapter);
        btnClearAll.setOnClickListener(v -> clearAllNotifications());
        fetchData();
        setupSwipeToDelete();
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void fetchData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseDatabase.getInstance(DB_URL).getReference("notifications").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        notificationList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Notification n = ds.getValue(Notification.class);
                            if (n != null) { n.setId(ds.getKey()); notificationList.add(n); }
                        }
                        Collections.sort(notificationList, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                        adapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void markNotificationAsRead(String notifId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) FirebaseDatabase.getInstance(DB_URL).getReference("notifications")
                .child(uid).child(notifId).child("isRead").setValue(true);
    }

    private void clearAllNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All")
                .setMessage("Delete all notifications?")
                .setPositiveButton("Clear", (d, w) -> {
                    FirebaseDatabase.getInstance(DB_URL).getReference("notifications").child(uid).removeValue();
                }).setNegativeButton("Cancel", null).show();
    }

    private void setupSwipeToDelete() {
        // ALLOW BOTH DIRECTIONS: ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getBindingAdapterPosition();

                if (pos >= 0 && pos < notificationList.size()) {
                    String nid = notificationList.get(pos).getId();

                    // 1. Remove from Adapter (Visual)
                    adapter.removeItem(pos);

                    // 2. Remove from Firebase (Data)
                    String uid = FirebaseAuth.getInstance().getUid();
                    if (uid != null) {
                        FirebaseDatabase.getInstance(DB_URL).getReference("notifications")
                                .child(uid).child(nid).removeValue();
                    }

                    if (notificationList.isEmpty()) tvEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint p = new Paint();
                    p.setColor(Color.parseColor("#FF5252")); // Red Delete Color

                    Drawable icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
                    if (icon != null) icon.setTint(Color.WHITE);

                    // --- SWIPE RIGHT (dX > 0) ---
                    if (dX > 0) {
                        // Draw Red Background on Left Side
                        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(),
                                dX, (float) itemView.getBottom(), p);

                        if (icon != null) {
                            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            int iconBottom = iconTop + icon.getIntrinsicHeight();

                            // Icon on the Left
                            int iconLeft = itemView.getLeft() + iconMargin;
                            int iconRight = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();

                            // Only draw if swipe is big enough
                            if (dX > iconMargin + icon.getIntrinsicWidth()) {
                                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                                icon.draw(c);
                            }
                        }
                    }
                    // --- SWIPE LEFT (dX < 0) ---
                    else if (dX < 0) {
                        // Draw Red Background on Right Side
                        c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                                (float) itemView.getRight(), (float) itemView.getBottom(), p);

                        if (icon != null) {
                            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            int iconBottom = iconTop + icon.getIntrinsicHeight();

                            // Icon on the Right
                            int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                            int iconRight = itemView.getRight() - iconMargin;

                            // Only draw if swipe is big enough
                            if (Math.abs(dX) > iconMargin + icon.getIntrinsicWidth()) {
                                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                                icon.draw(c);
                            }
                        }
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(rvNotifications);
    }
}