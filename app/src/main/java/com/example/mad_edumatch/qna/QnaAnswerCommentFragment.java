package com.example.mad_edumatch.qna;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.Comment;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.helper.TimeHelper;
import com.example.mad_edumatch.recycleAdapters.CommentAdapter;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QnaAnswerCommentFragment extends Fragment {

    private String answerId, answerUserId, attUrl, attName, answerLink;
    private RecyclerView rvComments;
    private CommentAdapter adapter;
    private List<Comment> commentList;
    private DatabaseReference commentRef, answerRef;

    private TextView tvAuthor, tvDate, tvContent, tvLink;
    private ImageView ivAnswerAvatar;
    private Button btnDeleteSolution, btnDownloadAttachment;

    private EditText etCommentInput;
    private Button btnPostComment;
    private TextView tvNoComments;

    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String PROJECT_ID = "693c16f700198f0a2ed3";
    private static final String BUCKET_ID = "693c1807002ab38e1751";
    private static final String ENDPOINT = "https://sgp.cloud.appwrite.io/v1";

    public QnaAnswerCommentFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.qna_fragment_comment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        tvAuthor = view.findViewById(R.id.tvAnswerDetailAuthor);
        tvDate = view.findViewById(R.id.tvAnswerDetailDate);
        tvContent = view.findViewById(R.id.tvAnswerDetailContent);
        tvLink = view.findViewById(R.id.tvAnswerDetailLink);
        ivAnswerAvatar = view.findViewById(R.id.ivAnswerDetailAvatar);
        tvNoComments = view.findViewById(R.id.tvNoComments);
        btnDeleteSolution = view.findViewById(R.id.btnDeleteSolution);
        btnDownloadAttachment = view.findViewById(R.id.btnDownloadMaterial);

        // References
        commentRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("forum_comments");
        answerRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("forum_answers");

        Bundle args = getArguments();
        if (args != null) {
            // Check for sourceId (Notification Path) or answerId (Normal Path)
            answerId = args.getString("answerId");

            // Fallback if you used "sourceId" in the Notification Fragment
            if (answerId == null) {
                answerId = args.getString("sourceId");
            }

            // --- ADDED: NOTIFICATION DEEP LINK LOGIC ---
            if (!args.containsKey("answerContent") && answerId != null) {
                fetchAnswerDetailsFromFirebase(answerId);
            } else {
                // Normal Path: Already has arguments
                answerUserId = args.getString("answerUserId");
                tvContent.setText(args.getString("answerContent"));
                tvDate.setText(TimeHelper.getMalaysiaTime(args.getLong("answerTime")));
                loadAnswerUserProfile(answerUserId);
                setupAnswerUI(args.getString("answerLink"), args.getString("attachmentUrl"), args.getString("attachmentName"));
            }
        }

        // Setup Comments
        rvComments = view.findViewById(R.id.rvComments);
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        commentList = new ArrayList<>();
        adapter = new CommentAdapter(commentList, commentId -> {
            commentRef.child(commentId).removeValue();
            Toast.makeText(getContext(), "Comment Deleted", Toast.LENGTH_SHORT).show();
        });
        rvComments.setAdapter(adapter);
        loadComments();

        // Post Comment
        etCommentInput = view.findViewById(R.id.etCommentInput);
        btnPostComment = view.findViewById(R.id.btnPostComment);
        btnPostComment.setOnClickListener(v -> postComment());
    }

    // --- ADDED: HELPER TO FETCH SOLUTION DETAILS ---
    private void fetchAnswerDetailsFromFirebase(String aId) {
        answerRef.child(aId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) {
                    if (snapshot.exists() == false) {
                        Toast.makeText(getContext(), "This solution was deleted.", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    }
                    return;
                }

                // Extract and Set Data
                answerUserId = snapshot.child("userId").getValue(String.class);
                tvContent.setText(snapshot.child("content").getValue(String.class));
                long time = snapshot.child("timestamp").getValue(Long.class);
                String link = snapshot.child("solutionLink").getValue(String.class);

                tvDate.setText(TimeHelper.getMalaysiaTime(time));

                loadAnswerUserProfile(answerUserId);

                String url = snapshot.child("attachmentUrl").getValue(String.class);
                String name = snapshot.child("attachmentName").getValue(String.class);
                setupAnswerUI(link, url, name);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- ADDED: HELPER TO REUSE UI LOGIC ---
    private void setupAnswerUI(String link, String url, String name) {
        answerLink = link;
        if (!TextUtils.isEmpty(answerLink)) {
            tvLink.setVisibility(View.VISIBLE);
            tvLink.setText("Refer Link: " + answerLink);
        } else {
            tvLink.setVisibility(View.GONE);
        }

        attUrl = url;
        attName = name;
        if (attUrl != null && !attUrl.isEmpty()) {
            btnDownloadAttachment.setVisibility(View.VISIBLE);
            btnDownloadAttachment.setText("Download: " + (attName != null ? attName : "File"));
            btnDownloadAttachment.setOnClickListener(v -> downloadFile(attUrl));
        } else {
            btnDownloadAttachment.setVisibility(View.GONE);
        }

        String currentUid = CurrentUser.getInstance().getUid();
        if (currentUid != null && currentUid.equals(answerUserId)) {
            btnDeleteSolution.setVisibility(View.VISIBLE);
            btnDeleteSolution.setOnClickListener(v -> deleteSolution());
        } else {
            btnDeleteSolution.setVisibility(View.GONE);
        }
    }

    private void loadAnswerUserProfile(String uid) {
        if (uid == null) return;
        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();
        db.child("student_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    updateUI(snapshot.child("username").getValue(String.class), snapshot.child("profileImageUrl").getValue(String.class));
                } else {
                    db.child("tutor_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot tutorSnap) {
                            if (tutorSnap.exists()) {
                                updateUI(tutorSnap.child("username").getValue(String.class), tutorSnap.child("profileImageUrl").getValue(String.class));
                            } else {
                                updateUI("Unknown User", null);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUI(String name, String imgUrl) {
        if (!isAdded()) return;
        tvAuthor.setText(name);
        if (imgUrl != null) {
            int resId = getResources().getIdentifier(imgUrl, "drawable", requireContext().getPackageName());
            ivAnswerAvatar.setImageResource(resId != 0 ? resId : R.drawable.ic_launcher_foreground);
        } else {
            ivAnswerAvatar.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    private void deleteSolution() {
        if (answerId != null) {
            answerRef.child(answerId).removeValue().addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Solution Deleted", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            });
        }
    }

    private void downloadFile(String fileId) {
        String fullUrl = ENDPOINT + "/storage/buckets/" + BUCKET_ID + "/files/" + fileId + "/view?project=" + PROJECT_ID;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening file", Toast.LENGTH_SHORT).show();
        }
    }

    private void postComment() {
        String content = etCommentInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();

        // 1. Try fetching from Student Profile
        db.child("student_profiles").child(uid).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    // It is a Student -> Post Comment with their name
                    String name = snapshot.getValue(String.class);
                    saveComment(uid, name, content);
                } else {
                    // 2. Not a Student? Check Tutor Profile
                    db.child("tutor_profiles").child(uid).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot tutorSnap) {
                            String name = "User"; // Fallback default
                            if (tutorSnap.exists() && tutorSnap.getValue() != null) {
                                // It is a Tutor -> Post Comment with their name
                                name = tutorSnap.getValue(String.class);
                            }
                            // Save with whatever name we found
                            saveComment(uid, name, content);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // On error, post as "User" so the app doesn't crash
                            saveComment(uid, "User", content);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // On error, post as "User"
                saveComment(uid, "User", content);
            }
        });
    }

    private void saveComment(String uid, String name, String content) {
        String key = commentRef.push().getKey();
        Comment comment = new Comment(key, answerId, uid, name, content, System.currentTimeMillis());
        commentRef.child(key).setValue(comment).addOnSuccessListener(unused -> {
            if (answerUserId != null && !answerUserId.equals(uid)) {
                sendCommentNotification(answerUserId, name);
            }
            etCommentInput.setText("");
        });
    }

    private void loadComments() {
        if (answerId == null) return;
        commentRef.orderByChild("answerId").equalTo(answerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Comment c = ds.getValue(Comment.class);
                    if (c != null) commentList.add(c);
                }
                adapter.notifyDataSetChanged();
                tvNoComments.setVisibility(commentList.isEmpty() ? View.VISIBLE : View.GONE);
                rvComments.setVisibility(commentList.isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendCommentNotification(String recipientId, String commenterName) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("notifications").child(recipientId);
        String notifId = notifRef.push().getKey();

        HashMap<String, Object> notifData = new HashMap<>();
        notifData.put("title", "New Comment!");

        // This message is now just a fallback/placeholder
        notifData.put("message", commenterName + " commented on your solution.");

        notifData.put("action_type", "OPEN_COMMENT");
        notifData.put("sourceId", answerId);

        // --- NEW: SAVE THE SENDER ID ---
        notifData.put("senderId", CurrentUser.getInstance().getUid());
        // -------------------------------

        notifData.put("timestamp", System.currentTimeMillis());
        notifData.put("isRead", false);

        notifRef.child(notifId).setValue(notifData);
    }
}