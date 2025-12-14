package com.example.mad_edumatch.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.ChatList;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.recycleAdapters.ChatListAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<ChatList> chatLists = new ArrayList<>();
    private DatabaseReference chatListRef;
    private TextView tvNoChats;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chat_fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvChatList);
        tvNoChats = view.findViewById(R.id.tvNoChats);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // --- MISSING PART START ---
        // You must initialize the adapter before loading data!
        adapter = new ChatListAdapter(getContext(), chatLists);
        recyclerView.setAdapter(adapter);
        // --- MISSING PART END ---

        String currentUid = CurrentUser.getInstance().getUid();
        if (currentUid != null) {
            chatListRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("chatlist")
                    .child(currentUid);

            loadChatList();
        }
    }

    private void loadChatList() {
        chatListRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatLists.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatList chat = data.getValue(ChatList.class);
                    if (chat != null) {
                        chatLists.add(chat);
                    }
                }

                // Sort by Timestamp (Newest first)
                Collections.sort(chatLists, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                adapter.notifyDataSetChanged();

                // --- NEW LOGIC: Check if the list is empty ---
                if (chatLists.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvNoChats.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvNoChats.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // In case of error, show the message too
                recyclerView.setVisibility(View.GONE);
                tvNoChats.setText("Failed to load chats. Please check your connection.");
                tvNoChats.setVisibility(View.VISIBLE);
            }
        });
    }
}