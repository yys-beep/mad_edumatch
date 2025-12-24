package com.example.mad_edumatch.tutor;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class TutorNotificationFragment extends Fragment {
    private TextView btnClearAll;
    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private TextView tvEmptyState;
    private static final String DB_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        return inflater.inflate(R.layout.tutor_fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        btnClearAll = view.findViewById(R.id.btnClearAll);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificationAdapter(notificationList, "Tutor", notification -> {
            markNotificationAsRead(notification.getId());

            String actionType = notification.getAction_type();
            String targetId = notification.getSourceId();

            if (actionType.isEmpty() || targetId == null) return;

            if (actionType.equals("OPEN_LESSON") || actionType.equals("KUDOS")) {
                FreeLessonDetailFragment fragment = new FreeLessonDetailFragment();
                Bundle args = new Bundle();
                args.putString("sourceId", targetId);
                fragment.setArguments(args);
                navigateTo(fragment);
            }

            else if (actionType.equals("OPEN_QUESTION") || actionType.equals("OPEN_QUESTION_DETAIL") || actionType.equals("MY_QUESTIONS_NOTIF")) {
                QnaDetailFragment fragment = new QnaDetailFragment();
                Bundle args = new Bundle();
                args.putString("sourceId", targetId);
                fragment.setArguments(args);
                navigateTo(fragment);
            }

            else if (actionType.equals("NEW_SOLUTION") || actionType.equals("OPEN_COMMENT")) {
                // We must check if the ANSWER still exists
                FirebaseDatabase.getInstance(DB_URL).getReference("forum_answers")
                        .child(targetId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!isAdded()) return;

                                if (!snapshot.exists()) {
                                    // If the solution was deleted, don't navigate
                                    Toast.makeText(getContext(), "This solution was deleted.", Toast.LENGTH_SHORT).show();
                                } else {
                                    // SOLUTION EXISTS: Navigate to comment fragment
                                    QnaAnswerCommentFragment fragment = new QnaAnswerCommentFragment();
                                    Bundle args = new Bundle();
                                    args.putString("answerId", targetId);
                                    args.putString("sourceId", targetId);
                                    fragment.setArguments(args);
                                    navigateTo(fragment);
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
            } else if ("OPEN_CHAT".equals(actionType)) {
                ChatDetailFragment fragment = new ChatDetailFragment();
                Bundle args = new Bundle();

                // In the notification node, 'senderId' is the person who messaged
                args.putString("targetUserId", notification.getSenderId());
                args.putString("listingId", notification.getSourceId());
                args.putString("targetUserName", "Student");
                args.putString("listingTitle", "Inquiry");
                fragment.setArguments(args);
                navigateTo(fragment);
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
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                String nid = notificationList.get(pos).getId();
                adapter.removeItem(pos);
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null) FirebaseDatabase.getInstance(DB_URL).getReference("notifications").child(uid).child(nid).removeValue();
                if (notificationList.isEmpty()) tvEmptyState.setVisibility(View.VISIBLE);
            }
        }).attachToRecyclerView(rvNotifications);
    }
}