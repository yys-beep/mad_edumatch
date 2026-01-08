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
            answerId = args.getString("answerId");
            if (answerId == null) {
                answerId = args.getString("sourceId");
            }

            if (!args.containsKey("answerContent") && answerId != null) {
                fetchAnswerDetailsFromFirebase(answerId);
            } else {
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
            commentRef.child(commentId).removeValue().addOnSuccessListener(unused -> {
                if(isAdded()) Toast.makeText(getContext(), getString(R.string.comment_deleted_success), Toast.LENGTH_SHORT).show();
            });
        });
        rvComments.setAdapter(adapter);
        loadComments();

        etCommentInput = view.findViewById(R.id.etCommentInput);
        btnPostComment = view.findViewById(R.id.btnPostComment);
        btnPostComment.setOnClickListener(v -> postComment());
    }

    private void fetchAnswerDetailsFromFirebase(String aId) {
        answerRef.child(aId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), getString(R.string.solution_deleted_error), Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    return;
                }

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

    private void setupAnswerUI(String link, String url, String name) {
        answerLink = link;
        if (!TextUtils.isEmpty(answerLink)) {
            tvLink.setVisibility(View.VISIBLE);
            tvLink.setText(getString(R.string.refer_link, answerLink));
        } else {
            tvLink.setVisibility(View.GONE);
        }

        attUrl = url;
        attName = name;
        if (attUrl != null && !attUrl.isEmpty()) {
            btnDownloadAttachment.setVisibility(View.VISIBLE);
            btnDownloadAttachment.setText(getString(R.string.download_file_label, (attName != null ? attName : "File")));
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
                                updateUI(getString(R.string.unknown_user), null);
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
                if(isAdded()) {
                    Toast.makeText(getContext(), getString(R.string.solution_deleted_success), Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                }
            });
        }
    }

    private void downloadFile(String fileId) {
        String fullUrl = ENDPOINT + "/storage/buckets/" + BUCKET_ID + "/files/" + fileId + "/view?project=" + PROJECT_ID;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)));
        } catch (Exception e) {
            if(isAdded()) Toast.makeText(getContext(), getString(R.string.file_open_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void postComment() {
        String content = etCommentInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();

        db.child("student_profiles").child(uid).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = (snapshot.exists() && snapshot.getValue() != null) ? snapshot.getValue(String.class) : null;
                if (name != null) {
                    saveComment(uid, name, content);
                } else {
                    db.child("tutor_profiles").child(uid).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot tutorSnap) {
                            String tutorName = (tutorSnap.exists() && tutorSnap.getValue() != null) ? tutorSnap.getValue(String.class) : getString(R.string.unknown_user);
                            saveComment(uid, tutorName, content);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveComment(String uid, String name, String content) {
        String key = commentRef.push().getKey();
        Comment comment = new Comment(key, answerId, uid, name, content, System.currentTimeMillis());
        if (key != null) {
            commentRef.child(key).setValue(comment).addOnSuccessListener(unused -> {
                if (answerUserId != null && !answerUserId.equals(uid)) {
                    sendCommentNotification(answerUserId, name);
                }
                etCommentInput.setText("");
            });
        }
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
                if(isAdded()) {
                    adapter.notifyDataSetChanged();
                    tvNoComments.setVisibility(commentList.isEmpty() ? View.VISIBLE : View.GONE);
                    tvNoComments.setText(getString(R.string.no_comments_yet));
                    rvComments.setVisibility(commentList.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Inside QnaAnswerCommentFragment.java
    private void sendCommentNotification(String recipientId, String commenterName) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("notifications").child(recipientId);

        HashMap<String, Object> notifData = new HashMap<>();
        notifData.put("title", getString(R.string.notif_new_comment_title));

        // Standard Q&A Comment Action Type
        notifData.put("action_type", "OPEN_COMMENT");
        notifData.put("sourceId", answerId); // Points to the Answer node
        notifData.put("senderId", CurrentUser.getInstance().getUid());
        notifData.put("timestamp", System.currentTimeMillis());
        notifData.put("isRead", false);

        notifRef.push().setValue(notifData);
    }
}