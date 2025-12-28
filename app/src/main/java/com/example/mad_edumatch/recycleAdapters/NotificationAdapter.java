package com.example.mad_edumatch.recycleAdapters;

import android.graphics.Color;
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

    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification n = list.get(position);

        holder.tvTitle.setText(n.getTitle());
        holder.tvMsg.setText(n.getMessage());

        // --- Color Logic ---
        if (!n.isRead()) {
            // UNSEEN: Light Pink
            holder.cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF0F5"));
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.itemView.setAlpha(1.0f);
        } else {
            // SEEN: White
            holder.cardView.setCardBackgroundColor(android.graphics.Color.WHITE);
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
        }

        // Sender Name Logic
        if (n.getSenderId() != null && !n.getSenderId().isEmpty()) {
            loadSenderNameAndFormatMessage(n.getSenderId(), n.getAction_type(), holder.tvMsg, n.getMessage());
        }

        // Icon Logic
        String actionType = n.getAction_type() != null ? n.getAction_type().toUpperCase() : "";
        switch (actionType) {
            case "OPEN_CHAT": holder.ivIcon.setImageResource(R.drawable.ic_chat_message); break;
            case "KUDOS":
            case "LIKE": holder.ivIcon.setImageResource(R.drawable.baseline_thumb_up_24); break;
            case "OPEN_QUESTION":
            case "NEW_SOLUTION":
            case "OPEN_COMMENT": holder.ivIcon.setImageResource(R.drawable.ic_comment_icon); break;
            default: holder.ivIcon.setImageResource(R.drawable.outline_notifications_active_24); break;
        }

        // Time Logic
        if (n.getTimestamp() != 0) {
            holder.tvTime.setText(TimeHelper.getMalaysiaTime(n.getTimestamp()));
            holder.tvTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvTime.setVisibility(View.GONE);
        }

        // --- CLICK LISTENER (FIXED) ---
        holder.itemView.setOnClickListener(v -> {
            // 1. INSTANTLY Change Color (Visual Trick)
            holder.cardView.setCardBackgroundColor(android.graphics.Color.WHITE);
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);

            // 2. Update Local Data
            if (!n.isRead()) {
                n.setRead(true);
                // We do NOT call notifyItemChanged here because we just updated the view manually above.
                // This prevents "flickering" or race conditions.
            }

            // 3. Trigger Navigation (This will update Firebase in the Fragment)
            if (listener != null) listener.onNotificationClick(n);
        });
    }

    private void loadSenderNameAndFormatMessage(String senderId, String actionType, TextView tvMsg, String originalMsg) {
        userDbRef.child("student_profiles").child(senderId).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    setDynamicMessage(snapshot.getValue(String.class), actionType, tvMsg, originalMsg);
                } else {
                    userDbRef.child("tutor_profiles").child(senderId).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot tutorSnap) {
                            if (tutorSnap.exists()) {
                                setDynamicMessage(tutorSnap.getValue(String.class), actionType, tvMsg, originalMsg);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setDynamicMessage(String realName, String actionType, TextView tvMsg, String fallbackMsg) {
        if (realName == null) return;
        if ("OPEN_COMMENT".equals(actionType)) tvMsg.setText(realName + " commented on your solution.");
        else if ("NEW_SOLUTION".equals(actionType)) tvMsg.setText(realName + " posted a solution.");
        else if ("LIKE".equals(actionType) || "KUDOS".equals(actionType)) tvMsg.setText(realName + " liked your post.");
        else tvMsg.setText(realName + " sent a notification.");
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