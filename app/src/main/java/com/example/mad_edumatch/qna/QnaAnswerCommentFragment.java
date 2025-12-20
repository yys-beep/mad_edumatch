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
        commentRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("forum_comments");
        answerRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("forum_answers");

        Bundle args = getArguments();
        if (args != null) {
            answerId = args.getString("answerId");
            answerUserId = args.getString("answerUserId");

            tvContent.setText(args.getString("answerContent"));
            tvDate.setText(TimeHelper.getMalaysiaTime(args.getLong("answerTime")));

            // --- NEW: Fetch Name & Avatar Live ---
            loadAnswerUserProfile(answerUserId);

            // Handle Link
            answerLink = args.getString("answerLink");
            if (!TextUtils.isEmpty(answerLink)) {
                tvLink.setVisibility(View.VISIBLE);
                tvLink.setText("Refer Link: " + answerLink);
            } else {
                tvLink.setVisibility(View.GONE);
            }

            // Handle Attachments
            attUrl = args.getString("attachmentUrl");
            attName = args.getString("attachmentName");
            if (attUrl != null && !attUrl.isEmpty()) {
                btnDownloadAttachment.setVisibility(View.VISIBLE);
                btnDownloadAttachment.setText("Download: " + (attName != null ? attName : "File"));
                btnDownloadAttachment.setOnClickListener(v -> downloadFile(attUrl));
            } else {
                btnDownloadAttachment.setVisibility(View.GONE);
            }

            // Delete Logic
            String currentUid = CurrentUser.getInstance().getUid();
            if (currentUid != null && currentUid.equals(answerUserId)) {
                btnDeleteSolution.setVisibility(View.VISIBLE);
                btnDeleteSolution.setOnClickListener(v -> deleteSolution());
            } else {
                btnDeleteSolution.setVisibility(View.GONE);
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

    private void loadAnswerUserProfile(String uid) {
        if (uid == null) return;

        DatabaseReference db = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        // 1. Check Student Profile
        db.child("student_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("username").getValue(String.class);
                    String imgUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    updateUI(name, imgUrl);
                } else {
                    // 2. Check Tutor Profile
                    db.child("tutor_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot tutorSnap) {
                            if (tutorSnap.exists()) {
                                String name = tutorSnap.child("username").getValue(String.class);
                                String imgUrl = tutorSnap.child("profileImageUrl").getValue(String.class);
                                updateUI(name, imgUrl);
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

        // Set Name
        tvAuthor.setText(name);

        // Set Avatar
        if (imgUrl != null) {
            int resId = getResources().getIdentifier(imgUrl, "drawable", requireContext().getPackageName());
            if (resId != 0) ivAnswerAvatar.setImageResource(resId);
            else ivAnswerAvatar.setImageResource(R.drawable.ic_launcher_foreground);
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

    // ... (keep downloadFile, loadAvatar, setDrawableAvatar, postComment, saveComment, loadComments same as before) ...
    // To save space, I'm assuming you use the same helper methods as provided in the previous turn.
    // If you need them repeated, just ask!

    private void downloadFile(String fileId) {
        String fullUrl = ENDPOINT + "/storage/buckets/" + BUCKET_ID + "/files/" + fileId + "/view?project=" + PROJECT_ID;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening file", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAvatar(String uid, ImageView iv) {
        if(uid == null) return;
        DatabaseReference db = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();
        db.child("student_profiles").child(uid).child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { if(s.exists()) setDrawableAvatar(s.getValue(String.class), iv);
            else db.child("tutor_profiles").child(uid).child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot s2) { if(s2.exists()) setDrawableAvatar(s2.getValue(String.class), iv); }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void setDrawableAvatar(String name, ImageView iv) {
        if (name != null && isAdded()) {
            int resId = getResources().getIdentifier(name, "drawable", requireContext().getPackageName());
            if (resId != 0) iv.setImageResource(resId);
            else iv.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    private void postComment() {
        String content = etCommentInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;
        String uid = CurrentUser.getInstance().getUid();

        DatabaseReference db = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        db.child("student_profiles").child(uid).child("username").get().addOnCompleteListener(task -> {
            String name = "User";
            if (task.isSuccessful() && task.getResult().getValue() != null) name = task.getResult().getValue(String.class);
            saveComment(uid, name, content);
        });
    }

    private void saveComment(String uid, String name, String content) {
        String key = commentRef.push().getKey();
        Comment comment = new Comment(key, answerId, uid, name, content, System.currentTimeMillis());
        commentRef.child(key).setValue(comment);
        etCommentInput.setText("");
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
                if (commentList.isEmpty()) {
                    tvNoComments.setVisibility(View.VISIBLE);
                    rvComments.setVisibility(View.GONE);
                } else {
                    tvNoComments.setVisibility(View.GONE);
                    rvComments.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}