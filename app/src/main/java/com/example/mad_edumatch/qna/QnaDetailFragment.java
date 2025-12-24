package com.example.mad_edumatch.qna;

import android.app.AlertDialog;
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
import com.example.mad_edumatch.firebaseModels.Answer;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.helper.TimeHelper;
import com.example.mad_edumatch.recycleAdapters.AnswerAdapter;
import com.example.mad_edumatch.upload.UploadMaterialBottom;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QnaDetailFragment extends Fragment implements UploadMaterialBottom.UploadListener {

    private String questionId, questionUserId, qFileUrl, qFileName;
    private DatabaseReference answerRef, questionRef;

    // --- VARIABLES FOR ANSWER UPLOAD ---
    private String tempAnswerFileId = null;
    private String tempAnswerFileName = null;

    // --- NEW: VARIABLES FOR EDIT QUESTION UPLOAD ---
    private boolean isEditMode = false; // Flag to distinguish Answer vs Edit upload
    private String tempEditFileId = null;
    private String tempEditFileName = null;
    private TextView tvEditFileStatus; // Reference to the TextView inside the Dialog

    // UI
    private ImageView ivAvatar;
    private TextView tvTitle, tvInfo, tvContent, tvAnswerFileName, tvNoAnswers;
    private Button btnEdit, btnDelete, btnViewAttachment, btnAttachFileToAnswer, btnPostAnswer;
    private MaterialButton btnMarkSolved;
    private EditText etAnswerContent, etSolutionLink;
    private RecyclerView rvAnswers;

    private AnswerAdapter adapter;
    private List<Answer> answerList;

    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String PROJECT_ID = "693c16f700198f0a2ed3";
    private static final String BUCKET_ID = "693c1807002ab38e1751";
    private static final String ENDPOINT = "https://sgp.cloud.appwrite.io/v1";
    private boolean isSolved = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.qna_fragment_question_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI Binding
        ivAvatar = view.findViewById(R.id.ivDetailAvatar);
        tvTitle = view.findViewById(R.id.tvDetailTitle);
        tvInfo = view.findViewById(R.id.tvDetailInfo);
        tvContent = view.findViewById(R.id.tvDetailContent);
        tvNoAnswers = view.findViewById(R.id.tvNoAnswers);

        btnEdit = view.findViewById(R.id.btnEditQuestion);
        btnDelete = view.findViewById(R.id.btnDeleteQuestion);
        btnMarkSolved = view.findViewById(R.id.btnMarkSolved);
        btnViewAttachment = view.findViewById(R.id.btnViewAttachment);

        rvAnswers = view.findViewById(R.id.rvAnswers);
        etAnswerContent = view.findViewById(R.id.etAnswerContent);
        etSolutionLink = view.findViewById(R.id.etSolutionLink);
        btnPostAnswer = view.findViewById(R.id.btnPostAnswer);
        btnAttachFileToAnswer = view.findViewById(R.id.btnAttachFileToAnswer);
        tvAnswerFileName = view.findViewById(R.id.tvAnswerFileName);

        // References
        answerRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("forum_answers");
        questionRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("forum_questions");

        // --- ENHANCED DATA LOADING (Supports Deep Links) ---
        if (getArguments() != null) {
            // 1. Resolve the ID from either 'sourceId' (notification) or 'questionId' (internal)
            questionId = getArguments().containsKey("sourceId") ?
                    getArguments().getString("sourceId") : getArguments().getString("questionId");

            // 2. Logic to determine if we fetch from Firebase or use passed data
            if (getArguments().containsKey("title") && !getArguments().containsKey("sourceId")) {
                // Normal Path: Data passed from list
                questionUserId = getArguments().getString("userId");
                tvTitle.setText(getArguments().getString("title"));
                tvContent.setText(getArguments().getString("content"));
                long time = getArguments().getLong("timestamp");
                qFileUrl = getArguments().getString("fileUrl");
                qFileName = getArguments().getString("fileName");
                isSolved = getArguments().getBoolean("solved", false);

                loadQuestionUserProfile(questionUserId, time);
                loadAvatar(questionUserId, ivAvatar);
                checkOwnership();
                updateSolvedUI();
                updateAttachmentButtonUI();
                loadAnswers(); // Ensure answers load for normal path
            } else if (questionId != null) {
                // Notification Path: ONLY the ID is known, so fetch everything
                fetchQuestionDetailsFromFirebase(questionId);
            } else {
                Toast.makeText(getContext(), "Error: Invalid Question ID", Toast.LENGTH_SHORT).show();
            }
        }

        // Setup Answer List
        rvAnswers.setLayoutManager(new LinearLayoutManager(getContext()));
        answerList = new ArrayList<>();
        adapter = new AnswerAdapter(getContext(), answerList, answer -> {
            QnaAnswerCommentFragment fragment = new QnaAnswerCommentFragment();
            Bundle args = new Bundle();
            args.putString("answerId", answer.getAnswerId());
            args.putString("answerUser", answer.getUserName());
            args.putString("answerUserId", answer.getUserId());
            args.putString("answerContent", answer.getContent());
            args.putString("answerLink", answer.getSolutionLink());
            args.putLong("answerTime", answer.getTimestamp());
            args.putString("attachmentUrl", answer.getAttachmentUrl());
            args.putString("attachmentName", answer.getAttachmentName());
            fragment.setArguments(args);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        rvAnswers.setAdapter(adapter);
        loadAnswers();

        // --- LISTENERS ---
        btnPostAnswer.setOnClickListener(v -> postAnswer());
        btnDelete.setOnClickListener(v -> deleteQuestion());
        btnEdit.setOnClickListener(v -> showEditQuestionDialog());
        btnMarkSolved.setOnClickListener(v -> toggleSolvedStatus());

        btnAttachFileToAnswer.setOnClickListener(v -> {
            isEditMode = false;
            UploadMaterialBottom uploadDialog = new UploadMaterialBottom();
            uploadDialog.show(getChildFragmentManager(), "UploadAnswer");
        });
    }

    // --- NEW: FETCH FOR NOTIFICATION DEEP LINK ---
    private void fetchQuestionDetailsFromFirebase(String qId) {
        this.questionId = qId;
        questionRef.child(qId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;

                if (!snapshot.exists()) {
                    // The question was deleted!
                    Toast.makeText(getContext(), "This question has been deleted.", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) getActivity().onBackPressed();
                    return;
                }

                tvTitle.setText(snapshot.child("title").getValue(String.class));
                tvContent.setText(snapshot.child("content").getValue(String.class));
                questionUserId = snapshot.child("userId").getValue(String.class);
                long time = snapshot.child("timestamp").getValue(Long.class);
                qFileUrl = snapshot.child("fileUrl").getValue(String.class);
                qFileName = snapshot.child("fileName").getValue(String.class);
                isSolved = snapshot.child("solved").getValue(Boolean.class);

                loadQuestionUserProfile(questionUserId, time);
                loadAvatar(questionUserId, ivAvatar);
                checkOwnership();
                updateSolvedUI();
                updateAttachmentButtonUI();
                loadAnswers(); // Refresh answers once ID is set
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadQuestionUserProfile(String uid, long timestamp) {
        if (uid == null) return;
        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();
        db.child("student_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    updateHeaderUI(snapshot.child("username").getValue(String.class), snapshot.child("profileImageUrl").getValue(String.class), timestamp);
                } else {
                    db.child("tutor_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot tutorSnap) {
                            if (tutorSnap.exists()) {
                                updateHeaderUI(tutorSnap.child("username").getValue(String.class), tutorSnap.child("profileImageUrl").getValue(String.class), timestamp);
                            } else {
                                updateHeaderUI("Unknown User", null, timestamp);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateHeaderUI(String name, String imgUrl, long timestamp) {
        if (!isAdded()) return;
        tvInfo.setText(name + " • " + TimeHelper.getMalaysiaTime(timestamp));
        if (imgUrl != null) {
            int resId = getResources().getIdentifier(imgUrl, "drawable", requireContext().getPackageName());
            if (resId != 0) ivAvatar.setImageResource(resId);
            else ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
        } else {
            ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    @Override
    public void onUploadSuccess(String fileId, String fileName) {
        if (isEditMode) {
            this.tempEditFileId = fileId;
            this.tempEditFileName = fileName;
            if (tvEditFileStatus != null) {
                tvEditFileStatus.setText("Selected: " + fileName);
                tvEditFileStatus.setVisibility(View.VISIBLE);
            }
        } else {
            this.tempAnswerFileId = fileId;
            this.tempAnswerFileName = fileName;
            if (tvAnswerFileName != null) {
                tvAnswerFileName.setText("Attached: " + fileName);
                tvAnswerFileName.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showEditQuestionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_post_question, null);
        builder.setView(view);

        EditText etEditTitle = view.findViewById(R.id.etQuestionTitleInput);
        EditText etEditContent = view.findViewById(R.id.etQuestionContentInput);
        Button btnAttach = view.findViewById(R.id.btnAttachFile);
        Button btnSubmit = view.findViewById(R.id.btnSubmitQuestion);
        tvEditFileStatus = view.findViewById(R.id.tvSelectedFileName);

        etEditTitle.setText(tvTitle.getText().toString());
        etEditContent.setText(tvContent.getText().toString());
        tempEditFileId = qFileUrl;
        tempEditFileName = qFileName;

        if (tempEditFileName != null && !TextUtils.isEmpty(tempEditFileName)) {
            tvEditFileStatus.setText("Current: " + tempEditFileName);
        } else {
            tvEditFileStatus.setText("No file attached");
        }

        btnSubmit.setText("Update Question");
        AlertDialog dialog = builder.create();

        btnAttach.setOnClickListener(v -> {
            isEditMode = true;
            UploadMaterialBottom uploadDialog = new UploadMaterialBottom();
            uploadDialog.show(getChildFragmentManager(), "UploadEdit");
        });

        btnSubmit.setOnClickListener(v -> {
            String newTitle = etEditTitle.getText().toString().trim();
            String newContent = etEditContent.getText().toString().trim();
            if (TextUtils.isEmpty(newTitle) || TextUtils.isEmpty(newContent)) {
                Toast.makeText(getContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            updateQuestion(newTitle, newContent, dialog);
        });
        dialog.show();
    }

    private void updateQuestion(String title, String content, AlertDialog dialog) {
        if (questionId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("content", content);
        updates.put("fileUrl", tempEditFileId);
        updates.put("fileName", tempEditFileName);

        questionRef.child(questionId).updateChildren(updates).addOnSuccessListener(aVoid -> {
            tvTitle.setText(title);
            tvContent.setText(content);
            qFileUrl = tempEditFileId;
            qFileName = tempEditFileName;
            updateAttachmentButtonUI();
            Toast.makeText(getContext(), "Question Updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void updateAttachmentButtonUI() {
        if (qFileUrl != null && !qFileUrl.isEmpty()) {
            btnViewAttachment.setVisibility(View.VISIBLE);
            btnViewAttachment.setText("Download: " + (qFileName != null ? qFileName : "File"));
            btnViewAttachment.setOnClickListener(v -> downloadFile(qFileUrl));
        } else {
            btnViewAttachment.setVisibility(View.GONE);
        }
    }

    private void postAnswer() {
        String content = etAnswerContent.getText().toString().trim();
        String link = etSolutionLink.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            etAnswerContent.setError("Content required");
            return;
        }

        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();
        // 1. Try fetching from student_profiles first
        userRef.child("student_profiles").child(uid).child("username").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                String name = task.getResult().getValue(String.class);
                saveToFirebase(uid, name, content, link); // Pass real name
            } else {
                // 2. If not a student, try tutor_profiles
                userRef.child("tutor_profiles").child(uid).child("username").get().addOnCompleteListener(tutorTask -> {
                    String name = (tutorTask.isSuccessful() && tutorTask.getResult().getValue() != null)
                            ? tutorTask.getResult().getValue(String.class) : "A user";
                    saveToFirebase(uid, name, content, link);
                });
            }
        });
    }

    private void saveToFirebase(String uid, String username, String content, String link) {
        String key = answerRef.push().getKey();
        Answer answer = new Answer(key, questionId, uid, username, content, link, System.currentTimeMillis(), tempAnswerFileId, tempAnswerFileName);

        answerRef.child(key).setValue(answer).addOnSuccessListener(unused -> {
            // UI Cleanup
            if (tvAnswerFileName != null) {
                tvAnswerFileName.setVisibility(View.GONE);
                tvAnswerFileName.setText("");
            }
            tempAnswerFileId = null;
            tempAnswerFileName = null;
            etAnswerContent.setText("");
            etSolutionLink.setText("");

            // NOTIFICATION LOGIC
            if (questionUserId != null && !questionUserId.equals(uid)) {
                sendAnswerNotification(questionUserId, username, tvTitle.getText().toString());
            }
            Toast.makeText(getContext(), "Solution Posted!", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendAnswerNotification(String recipientId, String answererName, String questionTitle) {
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("notifications").child(recipientId);

        // Snippet Logic: 20 chars
        String snippet = (questionTitle != null && questionTitle.length() > 20)
                ? questionTitle.substring(0, 20) + "..." : questionTitle;

        String dynamicMessage = answererName + " answered your question: " + snippet;

        HashMap<String, Object> data = new HashMap<>();
        data.put("title", "New Solution!");
        data.put("message", dynamicMessage);
        data.put("action_type", "OPEN_QUESTION");
        data.put("sourceId", questionId);
        data.put("timestamp", System.currentTimeMillis());
        data.put("isRead", false);

        ref.push().setValue(data);
    }

    private void loadAnswers() {
        if (questionId == null) return;
        answerRef.orderByChild("questionId").equalTo(questionId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                answerList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Answer a = ds.getValue(Answer.class);
                    if (a != null) answerList.add(a);
                }
                adapter.notifyDataSetChanged();
                tvNoAnswers.setVisibility(answerList.isEmpty() ? View.VISIBLE : View.GONE);
                rvAnswers.setVisibility(answerList.isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAvatar(String uid, ImageView iv) {
        if(uid == null) return;
        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();
        db.child("student_profiles").child(uid).child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if(s.exists()) setDrawableAvatar(s.getValue(String.class), iv);
                else db.child("tutor_profiles").child(uid).child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot s2) { if(s2.exists()) setDrawableAvatar(s2.getValue(String.class), iv); }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void setDrawableAvatar(String name, ImageView iv) {
        if (name != null && isAdded()) {
            int resId = getResources().getIdentifier(name, "drawable", requireContext().getPackageName());
            iv.setImageResource(resId != 0 ? resId : R.drawable.ic_launcher_foreground);
        }
    }

    private void checkOwnership() {
        String currentUid = CurrentUser.getInstance().getUid();
        boolean isOwner = (currentUid != null && questionUserId != null && currentUid.equals(questionUserId));
        btnEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnMarkSolved.setVisibility(isOwner ? View.VISIBLE : View.GONE);
    }

    private void deleteQuestion() {
        if (questionId == null) return;

        // Use a listener to confirm deletion was successful in Firebase
        questionRef.child(questionId).removeValue().addOnSuccessListener(aVoid -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Question Deleted", Toast.LENGTH_SHORT).show();

                // Go back to the forum list
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadFile(String fileId) {
        String fullUrl = ENDPOINT + "/storage/buckets/" + BUCKET_ID + "/files/" + fileId + "/view?project=" + PROJECT_ID;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Cannot open file", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSolvedStatus() {
        if (questionId == null) return;
        isSolved = !isSolved;
        questionRef.child(questionId).child("solved").setValue(isSolved).addOnSuccessListener(aVoid -> {
            updateSolvedUI();
            String msg = isSolved ? "Marked as Solved" : "Marked as Unsolved";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSolvedUI() {
        if (btnMarkSolved == null) return;
        btnMarkSolved.setText(isSolved ? "Mark as Unsolved" : "Mark as Solved");
        btnMarkSolved.setIconResource(isSolved ? R.drawable.outline_check_circle_24 : R.drawable.baseline_check_circle_24);
        btnMarkSolved.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(isSolved ? R.color.pastelGreen : R.color.lightSkyBlue, null)));
    }
}