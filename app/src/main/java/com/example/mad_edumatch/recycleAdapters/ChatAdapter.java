package com.example.mad_edumatch.recycleAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.ChatMessage;
import com.example.mad_edumatch.helper.CurrentUser;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;  // Received
    public static final int MSG_TYPE_RIGHT = 1; // Sent

    private Context context;
    private List<ChatMessage> chatList;
    private String currentUserId;

    // Store image names (e.g., "avatar_1")
    private String myProfileImageName = "";
    private String targetProfileImageName = "";

    // Constructor
    public ChatAdapter(Context context, List<ChatMessage> chatList, String myProfileImageName, String targetProfileImageName) {
        this.context = context;
        this.chatList = chatList;
        this.myProfileImageName = myProfileImageName;
        this.targetProfileImageName = targetProfileImageName;
        this.currentUserId = CurrentUser.getInstance().getUid();
    }

    // Method to update images dynamically when they are fetched
    public void updateProfileUrls(String myImageName, String targetImageName) {
        this.myProfileImageName = myImageName;
        this.targetProfileImageName = targetImageName;
        notifyDataSetChanged(); // Refresh the list to show new images
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_right, parent, false);
            return new ViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_left, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage chat = chatList.get(position);
        holder.tvMessage.setText(chat.getMessage());

        // 1. Determine which image name to use
        String imageName;
        if (getItemViewType(position) == MSG_TYPE_RIGHT) {
            imageName = myProfileImageName;     // Use MY avatar
        } else {
            imageName = targetProfileImageName; // Use TARGET avatar
        }

        // 2. Load the Image Resource
        if (holder.imgProfile != null) {
            if (imageName != null && !imageName.isEmpty()) {
                // Convert string name (e.g. "avatar_1") to Resource ID (int)
                int resId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());

                if (resId != 0) {
                    holder.imgProfile.setImageResource(resId);
                } else {
                    // Name exists but drawable not found
                    holder.imgProfile.setImageResource(R.drawable.outline_background_replace_24);
                }
            } else {
                // Name is empty (loading or not set)
                holder.imgProfile.setImageResource(R.drawable.outline_background_replace_24);
            }
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        // Compare senderId with currentUserId to decide Left or Right
        if (chatList.get(position).getSenderId().equals(currentUserId)) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvMessage;
        public ImageView imgProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);

            // Try to find the Right image first
            imgProfile = itemView.findViewById(R.id.imgProfileRight);
            // If not found (meaning we are in Left layout), find Left image
            if (imgProfile == null) {
                imgProfile = itemView.findViewById(R.id.imgProfileLeft);
            }
        }
    }
}