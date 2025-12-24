package com.example.mad_edumatch.recycleAdapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.Notification;
import com.example.mad_edumatch.helper.TimeHelper;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> list;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> list, String role, OnNotificationClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ensure this matches your XML file name
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification n = list.get(position);

        // DEBUG LINE: This will tell us what the Adapter actually sees
        android.util.Log.d("NOTIF_CHECK", "Title: " + n.getTitle() + " | ActionType: [" + n.getAction_type() + "]");

        String actionType = n.getAction_type() != null ? n.getAction_type().toUpperCase() : "";
        String notifTitle = n.getTitle() != null ? n.getTitle().toUpperCase() : "";

        holder.tvTitle.setText(n.getTitle());
        holder.tvMsg.setText(n.getMessage());
        holder.ivIcon.setImageResource(R.drawable.outline_notifications_active_24);

        switch (actionType) {
            case "OPEN_CHAT":
                holder.ivIcon.setImageResource(R.drawable.ic_chat_message);
                break;

            case "KUDOS":
            case "LIKE":
                holder.ivIcon.setImageResource(R.drawable.baseline_thumb_up_24);
                break;

            case "OPEN_QUESTION":
            case "NEW_SOLUTION":
            case "OPEN_COMMENT":
                holder.ivIcon.setImageResource(R.drawable.ic_comment_icon);
                break;

            default:
                // 2. FALLBACK: If actionType didn't match, check if "KUDOS" is in the Title
                if (notifTitle.contains("KUDOS")) {
                    holder.ivIcon.setImageResource(R.drawable.baseline_thumb_up_24);
                } else {
                    holder.ivIcon.setImageResource(R.drawable.outline_notifications_active_24);
                }
                break;
        }

        if (n.getTimestamp() != 0) {
            holder.tvTime.setText(TimeHelper.getMalaysiaTime(n.getTimestamp()));
            holder.tvTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvTime.setVisibility(View.GONE);
        }

        // Read/Unread logic...
        if (n.isRead()) {
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.itemView.setAlpha(0.7f);
        } else {
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.itemView.setAlpha(1.0f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(n);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < list.size()) {
            list.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, list.size());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMsg, tvTime;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMsg = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
        }
    }
}