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

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.ChatMessage;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.recycleAdapters.ChatAdapter;
import com.example.mad_edumatch.student.StudentProfileFragment; // Import Student Fragment
import com.example.mad_edumatch.tutor.TutorProfileFragment;   // Import Tutor Fragment
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
    private TextView tvUserName, tvUserRole;
    private ImageView imgTopAvatar;

    private String targetUserId;
    private String targetUserName;
    private String currentUserId;
    private String targetUserRoleStr = "Student"; // Default role

    private String targetProfileImageName = "";
    private String myProfileImageName = "";

    private DatabaseReference rootRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chat_fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            targetUserId = getArguments().getString("targetUserId");
            targetUserName = getArguments().getString("targetUserName");
        }

        if (targetUserId == null) {
            // If we are coming from Bottom Nav, there is no "target".
            // We should show a "Select a chat" message or go back to the Chat List.
            Toast.makeText(getContext(), "Please select a conversation from the list.", Toast.LENGTH_SHORT).show();

            // Safety check to ensure we don't crash the activity
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
            return;
        }

        currentUserId = CurrentUser.getInstance().getUid();
        rootRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        // Init Views
        tvUserName = view.findViewById(R.id.tvChatUserName);
        tvUserRole = view.findViewById(R.id.tvChatUserRole);
        imgTopAvatar = view.findViewById(R.id.imgTopAvatar);

        recyclerView = view.findViewById(R.id.rvChatMessages);
        etMessage = view.findViewById(R.id.etChatMessage);
        btnSend = view.findViewById(R.id.btnSendMessage);

        tvUserName.setText(targetUserName != null ? targetUserName : "User");

        // Setup Recycler
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(getContext(), messageList, "", "");
        recyclerView.setAdapter(chatAdapter);

        // Fetch Data
        fetchTargetUserDetails(targetUserId);
        fetchMyDetails();
        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage());
        seenMessage();

        // --- CLICK LISTENER FOR PROFILE PIC ---
        imgTopAvatar.setOnClickListener(v -> openUserProfile());
    }

    private void openUserProfile() {
        Fragment profileFragment;
        Bundle args = new Bundle();
        args.putString("targetUserId", targetUserId); // Pass the ID to the fragment

        // Check the role string we fetched earlier
        if ("Tutor".equalsIgnoreCase(targetUserRoleStr)) {
            profileFragment = new TutorProfileFragment();
        } else {
            profileFragment = new StudentProfileFragment();
        }

        profileFragment.setArguments(args);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit();
    }

    private void fetchTargetUserDetails(String uid) {
        if (uid == null) return;
        rootRef.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                if (!snapshot.exists()) return;

                String name = snapshot.child("name").getValue(String.class);
                if (name != null) tvUserName.setText(name);

                // --- CAPTURE ROLE ---
                if (snapshot.hasChild("role")) {
                    targetUserRoleStr = snapshot.child("role").getValue(String.class);
                    tvUserRole.setText(targetUserRoleStr);
                }

                // --- IMAGE ---
                if (snapshot.hasChild("profileImageUrl")) {
                    targetProfileImageName = snapshot.child("profileImageUrl").getValue(String.class);
                    int resId = getResources().getIdentifier(targetProfileImageName, "drawable", requireContext().getPackageName());
                    imgTopAvatar.setImageResource(resId != 0 ? resId : R.drawable.outline_background_replace_24);

                    if (chatAdapter != null) chatAdapter.updateProfileUrls(myProfileImageName, targetProfileImageName);
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
                    myProfileImageName = snapshot.child("profileImageUrl").getValue(String.class);
                    if (chatAdapter != null) chatAdapter.updateProfileUrls(myProfileImageName, targetProfileImageName);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String msg = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) return;

        // --- ADDED: ID Safety check to prevent crashes ---
        if (currentUserId == null || targetUserId == null) return;

        // --- ADDED: Fetch listing details from arguments with fallbacks ---
        String tempListingId = "general";
        String tempListingTitle = "Chat";

        if (getArguments() != null) {
            tempListingId = getArguments().getString("listingId", "general");
            tempListingTitle = getArguments().getString("listingTitle", "Chat");
        }

        final String finalListingId = tempListingId;
        final String finalListingTitle = tempListingTitle;

        String messagePushId = rootRef.child("chats").child(currentUserId).child(targetUserId).push().getKey();
        if (messagePushId == null) return;

        long timestamp = System.currentTimeMillis();

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", messagePushId);
        messageMap.put("senderId", currentUserId);
        messageMap.put("receiverId", targetUserId);
        messageMap.put("message", msg);
        messageMap.put("timestamp", timestamp);
        // --- ADDED: Store listingId in the message for tracking ---
        messageMap.put("listingId", finalListingId);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("chats/" + currentUserId + "/" + targetUserId + "/" + messagePushId, messageMap);
        updateMap.put("chats/" + targetUserId + "/" + currentUserId + "/" + messagePushId, messageMap);

        // Update Chat Lists
        Map<String, Object> listMap = new HashMap<>();
        listMap.put("id", targetUserId);
        listMap.put("lastMessage", msg);
        listMap.put("timestamp", timestamp);
        listMap.put("isSeen", true);
        updateMap.put("chatlist/" + currentUserId + "/" + targetUserId, listMap);

        Map<String, Object> receiverListMap = new HashMap<>();
        receiverListMap.put("id", currentUserId);
        receiverListMap.put("lastMessage", msg);
        receiverListMap.put("timestamp", timestamp);
        receiverListMap.put("isSeen", false);
        updateMap.put("chatlist/" + targetUserId + "/" + currentUserId, receiverListMap);

        rootRef.updateChildren(updateMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                etMessage.setText("");

                // --- ADDED: SMART NOTIFICATION LOGIC ---
                // Triggers for every different subject listing inquired about
                if (!"general".equals(finalListingId) && messageList != null) {
                    boolean alreadyNotifiedForThisListing = false;

                    for (ChatMessage m : messageList) {
                        if (m != null && m.getMessage() != null && m.getMessage().contains(finalListingTitle)) {
                            alreadyNotifiedForThisListing = true;
                            break;
                        }
                    }

                    if (!alreadyNotifiedForThisListing) {
                        // Safe name check to avoid Firebase null value crash
                        String safeSenderName = CurrentUser.getInstance().getName();
                        if (safeSenderName == null) safeSenderName = "User";

                        sendChatNotification(targetUserId, safeSenderName, finalListingTitle, finalListingId);
                    }
                }
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
                            messageList.add(data.getValue(ChatMessage.class));
                        }
                        chatAdapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void seenMessage() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("isSeen", true);
        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("chatlist").child(currentUserId).child(targetUserId).updateChildren(hashMap);
    }

    private void sendChatNotification(String recipientId, String senderName, String listingTitle, String listingId) {
        DatabaseReference notifRef = rootRef.child("notifications").child(recipientId);

        HashMap<String, Object> data = new HashMap<>();
        data.put("title", "Listing Inquiry");
        data.put("message", senderName + " inquired about " + listingTitle);
        data.put("action_type", "OPEN_CHAT");
        data.put("sourceId", listingId); // This is the listing ID
        data.put("senderId", currentUserId); // Your ID so they can reply
        data.put("timestamp", System.currentTimeMillis());
        data.put("isRead", false);

        notifRef.push().setValue(data);
    }
}