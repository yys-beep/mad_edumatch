package com.example.mad_edumatch.chat;

import android.graphics.Canvas;
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
import com.example.mad_edumatch.firebaseModels.ChatList;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.helper.SwipeHelper;
import com.example.mad_edumatch.recycleAdapters.ChatListAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<ChatList> chatLists = new ArrayList<>();
    private DatabaseReference chatListRef;
    private TextView tvNoChats;

    // 1. DEFINE LISTENER AS GLOBAL VARIABLE
    private ValueEventListener chatListListener;

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
        adapter = new ChatListAdapter(getContext(), chatLists);
        recyclerView.setAdapter(adapter);

        String currentUid = CurrentUser.getInstance().getUid();

        if (currentUid != null) {
            chatListRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("chatlist")
                    .child(currentUid);

            loadChatList();
        } else {
            tvNoChats.setVisibility(View.VISIBLE);
        }

        setupSwipeToDelete();
    }

    private void loadChatList() {
        // 2. PREVENT DUPLICATE LISTENERS
        if (chatListListener != null) {
            chatListRef.removeEventListener(chatListListener);
        }

        chatListListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                // 3. USE MAP TO ENFORCE UNIQUENESS (Fixes duplicates)
                Map<String, ChatList> uniqueMap = new HashMap<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatList chat = data.getValue(ChatList.class);
                    if (chat != null) {
                        // Use key as ID (from your JSON: "kpahon..." or "qBlO6K...")
                        chat.setId(data.getKey());
                        uniqueMap.put(chat.getId(), chat);
                    }
                }

                List<ChatList> newList = new ArrayList<>(uniqueMap.values());

                // 4. SORT BY TIME
                if (newList.size() > 1) {
                    Collections.sort(newList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                }

                // 5. UPDATE UI
                chatLists.clear();
                chatLists.addAll(newList);
                adapter.notifyDataSetChanged();

                // Visibility
                if (chatLists.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvNoChats.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvNoChats.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        // Attach the new listener
        chatListRef.addValueEventListener(chatListListener);
    }

    // 6. CRITICAL: CLEAN UP LISTENER WHEN LEAVING SCREEN
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatListRef != null && chatListListener != null) {
            chatListRef.removeEventListener(chatListListener);
        }
    }

    private void setupSwipeToDelete() {
        SwipeHelper customSwipeHelper = new SwipeHelper(getContext());
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position >= 0 && position < chatLists.size()) {
                    ChatList deletedChat = chatLists.get(position);
                    new android.app.AlertDialog.Builder(getContext())
                            .setTitle("Delete Chat")
                            .setMessage("Are you sure?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                if (deletedChat.getId() != null) {
                                    chatListRef.child(deletedChat.getId()).removeValue();
                                }
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> adapter.notifyItemChanged(position))
                            .show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, float dX, float dY, int actionState, boolean isActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    customSwipeHelper.paint(c, vh, dX);
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive);
            }
        };
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
    }
}