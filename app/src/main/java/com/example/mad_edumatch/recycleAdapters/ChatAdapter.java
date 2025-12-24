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
import com.example.mad_edumatch.helper.TimeHelper;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private Context context;
    private List<ChatMessage> chatList;
    private String currentUserId;
    private String myProfileImageName = "";
    private String targetProfileImageName = "";

    public ChatAdapter(Context context, List<ChatMessage> chatList, String myProfileImageName, String targetProfileImageName) {
        this.context = context;
        this.chatList = chatList;
        this.myProfileImageName = myProfileImageName;
        this.targetProfileImageName = targetProfileImageName;
        this.currentUserId = CurrentUser.getInstance().getUid();
    }

    public void updateProfileUrls(String myImageName, String targetImageName) {
        this.myProfileImageName = myImageName;
        this.targetProfileImageName = targetImageName;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == MSG_TYPE_RIGHT) {
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_right, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_left, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage chat = chatList.get(position);

        // Safety check for null message
        holder.tvMessage.setText(chat.getMessage() != null ? chat.getMessage() : "");

        // --- FIXED TIME LOGIC ---
        if (holder.tvTime != null) {
            holder.tvTime.setText(TimeHelper.getMalaysiaTime(chat.getTimestamp()));
        }

        // --- FIXED IMAGE LOGIC ---
        String imageName = (getItemViewType(position) == MSG_TYPE_RIGHT) ? myProfileImageName : targetProfileImageName;

        if (holder.imgProfile != null) {
            if (imageName != null && !imageName.isEmpty()) {
                int resId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
                holder.imgProfile.setImageResource(resId != 0 ? resId : R.drawable.outline_background_replace_24);
            } else {
                holder.imgProfile.setImageResource(R.drawable.outline_background_replace_24);
            }
        }
    }

    @Override
    public int getItemCount() { return chatList.size(); }

    @Override
    public int getItemViewType(int position) {
        ChatMessage chat = chatList.get(position);
        String myUid = CurrentUser.getInstance().getUid();

        if (chat.getSenderId() != null && myUid != null && chat.getSenderId().equals(myUid)) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvMessage;
        public TextView tvTime;
        public ImageView imgProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);

            // Use the correct ID from XML
            tvTime = itemView.findViewById(R.id.tvTime);

            // Fallback for image IDs to cover both left and right XMLs
            imgProfile = itemView.findViewById(R.id.imgProfileRight);
            if (imgProfile == null) {
                imgProfile = itemView.findViewById(R.id.imgProfileLeft);
            }
        }
    }
}