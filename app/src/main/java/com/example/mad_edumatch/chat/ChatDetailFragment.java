package com.example.mad_edumatch.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Ensure Glide dependency is added
import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.ChatMessage;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.recycleAdapters.ChatAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatDetailFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;

    private EditText etMessage;
    private ImageButton btnSend;

    // Top Bar Views
    private TextView tvUserName;
    private TextView tvUserRole;
    private ImageView imgTopAvatar;

    private String targetUserId;
    private String targetUserName;
    private String currentUserId;

    // URLs for Chat Bubbles
    private String targetProfileUrl = "";
    private String myProfileUrl = "";

    private DatabaseReference rootRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chat_fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Get Arguments
        if (getArguments() != null) {
            targetUserId = getArguments().getString("targetUserId");
            targetUserName = getArguments().getString("targetUserName");
        }
        currentUserId = CurrentUser.getInstance().getUid();
        rootRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        // 2. Init Views
        tvUserName = view.findViewById(R.id.tvChatUserName);
        tvUserRole = view.findViewById(R.id.tvChatUserRole); // Make sure ID exists in XML
        imgTopAvatar = view.findViewById(R.id.imgTopAvatar); // Make sure ID exists in XML

        recyclerView = view.findViewById(R.id.rvChatMessages);
        etMessage = view.findViewById(R.id.etChatMessage);
        btnSend = view.findViewById(R.id.btnSendMessage);

        // Set initial name (will update if fetch succeeds)
        tvUserName.setText(targetUserName != null ? targetUserName : "User");

        // 3. Setup RecyclerView
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        messageList = new ArrayList<>();

        // Init Adapter with empty URLs initially
        chatAdapter = new ChatAdapter(getContext(), messageList, myProfileUrl, targetProfileUrl);
        recyclerView.setAdapter(chatAdapter);

        // 4. Fetch Details & Messages
        fetchTargetUserDetails(targetUserId);
        fetchMyDetails();
        loadMessages();

        // 5. Send Button
        btnSend.setOnClickListener(v -> sendMessage());
        seenMessage();
    }

    private void fetchTargetUserDetails(String uid) {
        if (uid == null) return;

        DatabaseReference userRef = rootRef.child("Users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // 1. Name & Role (Same as before)
                String name = snapshot.child("name").getValue(String.class);
                if (name != null) tvUserName.setText(name);

                String role = "Student";
                if (snapshot.hasChild("role")) role = snapshot.child("role").getValue(String.class);
                tvUserRole.setText(role);

                // 2. IMAGE LOGIC (FIXED)
                if (snapshot.hasChild("profileImageUrl")) {
                    targetProfileUrl = snapshot.child("profileImageUrl").getValue(String.class); // e.g. "avatar_1"

                    // Convert String "avatar_1" to R.drawable.avatar_1
                    int resId = getResources().getIdentifier(targetProfileUrl, "drawable", requireContext().getPackageName());

                    if (resId != 0) {
                        // Found the drawable!
                        if (imgTopAvatar != null) imgTopAvatar.setImageResource(resId);
                    } else {
                        // Fallback if drawable not found
                        if (imgTopAvatar != null) imgTopAvatar.setImageResource(R.drawable.outline_background_replace_24);
                    }

                    // Update Adapter
                    if (chatAdapter != null) {
                        chatAdapter.updateProfileUrls(myProfileUrl, targetProfileUrl);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchMyDetails() {
        String myUid = CurrentUser.getInstance().getUid();
        rootRef.child("Users").child(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("profileImageUrl")) {
                    myProfileUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    // Update Adapter
                    if (chatAdapter != null) {
                        chatAdapter.updateProfileUrls(myProfileUrl, targetProfileUrl);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String msg = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) return;

        // 1. Generate Message ID
        String messagePushId = rootRef.child("chats").child(currentUserId).child(targetUserId).push().getKey();
        if (messagePushId == null) return;

        long timestamp = System.currentTimeMillis();

        // 2. Define Paths (Declared only once now)
        String senderPath = "chats/" + currentUserId + "/" + targetUserId + "/" + messagePushId;
        String receiverPath = "chats/" + targetUserId + "/" + currentUserId + "/" + messagePushId;

        // 3. Define ChatList Paths (For Recent Chats screen)
        String senderListPath = "chatlist/" + currentUserId + "/" + targetUserId;
        String receiverListPath = "chatlist/" + targetUserId + "/" + currentUserId;

        // 4. Create Message Data
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", messagePushId);
        messageMap.put("senderId", currentUserId);
        messageMap.put("receiverId", targetUserId);
        messageMap.put("message", msg);
        messageMap.put("timestamp", timestamp);

        // 5. Create ChatList Data (Summary)
        Map<String, Object> listMap = new HashMap<>();
        listMap.put("id", targetUserId);
        listMap.put("lastMessage", msg);
        listMap.put("timestamp", timestamp);
        listMap.put("isSeen", true);

        Map<String, Object> receiverListMap = new HashMap<>();
        receiverListMap.put("id", currentUserId);
        receiverListMap.put("lastMessage", msg);
        receiverListMap.put("timestamp", timestamp);
        receiverListMap.put("isSeen", false);

        // 6. Combine all updates into one map for atomic write
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put(senderPath, messageMap);
        updateMap.put(receiverPath, messageMap);
        updateMap.put(senderListPath, listMap);       // Add to My Recent Chats
        updateMap.put(receiverListPath, receiverListMap); // Add to Their Recent Chats

        // 7. Execute Update
        rootRef.updateChildren(updateMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                etMessage.setText("");
            } else {
                Toast.makeText(getContext(), "Failed to send", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        rootRef.child("chats").child(currentUserId).child(targetUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            ChatMessage message = data.getValue(ChatMessage.class);
                            messageList.add(message);
                        }
                        chatAdapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void seenMessage() {
        DatabaseReference listRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("chatlist")
                .child(currentUserId)
                .child(targetUserId);

        // Set "isSeen" to true immediately
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("isSeen", true);
        listRef.updateChildren(hashMap);
    }
}