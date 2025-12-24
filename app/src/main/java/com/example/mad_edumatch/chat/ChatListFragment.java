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
        // Ensure this layout exists in your res/layout folder
        return inflater.inflate(R.layout.chat_fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvChatList);
        tvNoChats = view.findViewById(R.id.tvNoChats);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the adapter with the empty list first
        adapter = new ChatListAdapter(getContext(), chatLists);
        recyclerView.setAdapter(adapter);

        String currentUid = CurrentUser.getInstance().getUid();

        if (currentUid != null) {
            chatListRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("chatlist")
                    .child(currentUid);

            loadChatList();
        } else {
            // Safety: If no user found, show empty state
            tvNoChats.setVisibility(View.VISIBLE);
            tvNoChats.setText("Please log in to see your chats.");
        }

        // Inside ChatListFragment.java -> onViewCreated
        SwipeHelper customSwipeHelper = new SwipeHelper(getContext());

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                ChatList deletedChat = chatLists.get(position);

                new android.app.AlertDialog.Builder(getContext())
                        .setTitle("Delete Unknown Entry")
                        .setMessage("This entry has no ID mapping. Delete it anyway?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // FIX: If the internal ID is null, try to get the ID from the list item directly
                            String chatIdToDelete = deletedChat.getId();

                            if (chatIdToDelete != null) {
                                chatListRef.child(chatIdToDelete).removeValue();
                            } else {
                                // If it's still null, the data is corrupted.
                                // You may need to delete it manually from the Firebase Console.
                                Toast.makeText(getContext(), "Cannot delete: Missing ID", Toast.LENGTH_SHORT).show();
                                adapter.notifyItemChanged(position);
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                        })
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {

                // This draws your red background from the SwipeHelper class
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    customSwipeHelper.paint(c, viewHolder, dX);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

// ATTACH THE HELPER
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
    }

    private void loadChatList() {
        // REMOVE the loop that was here; 'snapshot' is not available yet!

        chatListRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                chatLists.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatList chat = data.getValue(ChatList.class);
                    if (chat != null) {
                        // FORCE ID: This makes "Unknown User" deletable
                        chat.setId(data.getKey());
                        chatLists.add(chat);
                    }
                }

                if (chatLists.size() > 1) {
                    Collections.sort(chatLists, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                }

                adapter.notifyDataSetChanged();

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
                if (isAdded()) {
                    Toast.makeText(getContext(), "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}