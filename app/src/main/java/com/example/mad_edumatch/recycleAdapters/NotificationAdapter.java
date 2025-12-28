package com.example.mad_edumatch.recycleAdapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.Notification;
import com.example.mad_edumatch.helper.TimeHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> list;
    private OnNotificationClickListener listener;
    private DatabaseReference userDbRef;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> list, String role, OnNotificationClickListener listener) {
        this.list = list;
        this.listener = listener;
        this.userDbRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification n = list.get(position);
        Context context = holder.itemView.getContext();

        // --- 1. TRANSLATE TITLE DYNAMICALLY ---
        // Using toUpperCase to ensure matching regardless of how it was saved
        String actionType = n.getAction_type() != null ? n.getAction_type().toUpperCase() : "";

        switch (actionType) {
            case "NEW_SOLUTION":
                holder.tvTitle.setText(context.getString(R.string.new_solution_notif_title));
                break;
            case "OPEN_CHAT":
                holder.tvTitle.setText(context.getString(R.string.listing_inquiry_title));
                break;
            case "LESSON_COMMENT": // Added for Free Lesson Comments
                holder.tvTitle.setText(context.getString(R.string.new_comment_notif_title));
                break;
            case "OPEN_COMMENT": // Standardized Q&A comment title
                holder.tvTitle.setText(context.getString(R.string.notif_new_comment_title));
                break;
            case "KUDOS":
            case "LIKE":
                holder.tvTitle.setText(context.getString(R.string.badge_loved_title));
                break;
            default:
                // If actionType is unknown, use a default localized string instead of database text
                holder.tvTitle.setText(context.getString(R.string.default_notif_title));
                break;
        }

        // --- 2. ASYNC MESSAGE LOADING ---
        // Tag the view to prevent recycling glitches
        if (n.getSenderId() != null && !n.getSenderId().isEmpty()) {
            holder.tvMsg.setTag(n.getSenderId());
            loadSenderNameAndFormatMessage(n.getSenderId(), actionType, holder.tvMsg);
        } else {
            holder.tvMsg.setText(n.getMessage());
        }

        // --- 3. UI STATE (Read/Unread) ---
        if (!n.isRead()) {
            holder.cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF0F5"));
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
        } else {
            holder.cardView.setCardBackgroundColor(android.graphics.Color.WHITE);
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
        }

        // --- 4. ICON LOGIC ---
        switch (actionType) {
            case "OPEN_CHAT":
                holder.ivIcon.setImageResource(R.drawable.ic_chat_message);
                break;
            case "KUDOS":
            case "LIKE":
                holder.ivIcon.setImageResource(R.drawable.baseline_thumb_up_24);
                break;
            case "LESSON_COMMENT":
            case "OPEN_COMMENT":
            case "NEW_SOLUTION":
                holder.ivIcon.setImageResource(R.drawable.ic_comment_icon);
                break;
            default:
                holder.ivIcon.setImageResource(R.drawable.outline_notifications_active_24);
                break;
        }

        // --- 5. TIME & CLICK ---
        if (n.getTimestamp() != 0) {
            holder.tvTime.setText(TimeHelper.getMalaysiaTime(n.getTimestamp()));
            holder.tvTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvTime.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!n.isRead()) {
                n.setRead(true);
                notifyItemChanged(position);
            }
            if (listener != null) listener.onNotificationClick(n);
        });
    }

    private void loadSenderNameAndFormatMessage(String senderId, String actionType, TextView tvMsg) {
        // Search student profiles first
        userDbRef.child("student_profiles").child(senderId).child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            safeSetText(tvMsg, senderId, snapshot.getValue(String.class), actionType);
                        } else {
                            // Search tutor profiles second
                            userDbRef.child("tutor_profiles").child(senderId).child("username")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot tutorSnap) {
                                            String name = tutorSnap.exists() ? tutorSnap.getValue(String.class) :
                                                    tvMsg.getContext().getString(R.string.user_fallback);
                                            safeSetText(tvMsg, senderId, name, actionType);
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void safeSetText(TextView tvMsg, String expectedSenderId, String realName, String actionType) {
        if (tvMsg.getTag() != null && tvMsg.getTag().equals(expectedSenderId)) {
            setDynamicMessage(realName, actionType, tvMsg);
        }
    }

    private void setDynamicMessage(String realName, String actionType, TextView tvMsg) {
        Context context = tvMsg.getContext();
        // Updated to handle 1-argument string resources to prevent crashes
        switch (actionType) {
            case "LESSON_COMMENT":
                tvMsg.setText(context.getString(R.string.notif_lesson_comment, realName));
                break;
            case "NEW_SOLUTION":
                tvMsg.setText(context.getString(R.string.notif_qna_solution, realName));
                break;
            case "OPEN_COMMENT":
                tvMsg.setText(context.getString(R.string.notif_qna_comment, realName));
                break;
            case "LIKE":
            case "KUDOS":
                tvMsg.setText(context.getString(R.string.notif_liked, realName));
                break;
            case "OPEN_CHAT":
                tvMsg.setText(context.getString(R.string.notif_chat, realName));
                break;
            default:
                tvMsg.setText(context.getString(R.string.notif_default, realName));
                break;
        }
    }

    @Override
    public int getItemCount() { return list != null ? list.size() : 0; }

    public void removeItem(int position) {
        if (position >= 0 && position < list.size()) {
            list.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMsg, tvTime;
        ImageView ivIcon;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMsg = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            cardView = itemView.findViewById(R.id.cardNotification);
        }
    }
}