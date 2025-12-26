package com.example.mad_edumatch.recycleAdapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mad_edumatch.R;
import com.example.mad_edumatch.chat.ChatDetailFragment;
import com.example.mad_edumatch.firebaseModels.ChatList;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone; // Import TimeZone

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private Context context;
    private List<ChatList> chatLists;
    private final String DB_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    public ChatListAdapter(Context context, List<ChatList> chatLists) {
        this.context = context;
        this.chatLists = chatLists;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatList chatList = chatLists.get(position);

        holder.tvLastMessage.setText(chatList.getLastMessage());
        holder.tvName.setText("Loading...");

        // --- UPDATED TIME LOGIC ---
        holder.tvTime.setText(getSmartDate(chatList.getTimestamp()));

        // Bold Logic (Unread)
        if (!chatList.isSeen()) {
            holder.tvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.tvLastMessage.setTextColor(context.getResources().getColor(android.R.color.black));
        } else {
            holder.tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.tvLastMessage.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        }

        loadUserInfo(chatList.getId(), holder);

        holder.itemView.setOnClickListener(v -> {
            String currentName = holder.tvName.getText().toString();
            openChatFragment(chatList.getId(), currentName);
        });
    }

    // --- FIX: USE MALAYSIA TIMEZONE HERE ---
    private String getSmartDate(long timestamp) {
        // 1. Force TimeZone
        TimeZone myTimeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur");

        Calendar now = Calendar.getInstance(myTimeZone);
        Calendar msgTime = Calendar.getInstance(myTimeZone);
        msgTime.setTimeInMillis(timestamp);

        // Check if it's the same day
        if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)) {
            // TODAY: Return Time (e.g., "10:30 PM")
            return DateFormat.format("hh:mm aa", msgTime).toString();
        }

        // Check if it was Yesterday
        now.add(Calendar.DATE, -1);
        if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)) {
            // YESTERDAY
            return "Yesterday";
        }

        // OLDER: Return Date (e.g., "13 Dec")
        return DateFormat.format("dd MMM", msgTime).toString();
    }

    // ... (Keep loadUserInfo, openChatFragment, getItemCount same as before) ...
    private void loadUserInfo(String userId, ViewHolder holder) {
        if (userId == null || userId.trim().isEmpty()) {
            holder.tvName.setText("Unknown User");
            holder.imgProfile.setImageResource(R.drawable.outline_background_replace_24);
            return; // Prevents the crash at .child(userId)
        }
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(userId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (context == null || holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;

                if(snapshot.exists()) {
                    String name = "User";
                    if (snapshot.hasChild("name")) name = snapshot.child("name").getValue(String.class);
                    else if (snapshot.hasChild("username")) name = snapshot.child("username").getValue(String.class);
                    holder.tvName.setText(name);

                    if(snapshot.hasChild("profileImageUrl")) {
                        String imgName = snapshot.child("profileImageUrl").getValue(String.class);
                        if (imgName != null && !imgName.isEmpty()) {
                            int resId = context.getResources().getIdentifier(imgName, "drawable", context.getPackageName());
                            if (resId != 0) Glide.with(context).load(resId).into(holder.imgProfile);
                            else holder.imgProfile.setImageResource(R.drawable.outline_background_replace_24);
                        }
                    } else {
                        holder.imgProfile.setImageResource(R.drawable.outline_background_replace_24);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void openChatFragment(String targetId, String targetName) {
        ChatDetailFragment chatFragment = new ChatDetailFragment();
        android.os.Bundle args = new android.os.Bundle();
        args.putString("targetUserId", targetId);
        args.putString("targetUserName", targetName);
        chatFragment.setArguments(args);
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, chatFragment)
                    .addToBackStack(null).commit();
        }
    }

    @Override
    public int getItemCount() { return chatLists.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvLastMessage, tvTime;
        public ImageView imgProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvChatListName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvChatListTime);
            imgProfile = itemView.findViewById(R.id.imgChatListProfile);
        }
    }
}