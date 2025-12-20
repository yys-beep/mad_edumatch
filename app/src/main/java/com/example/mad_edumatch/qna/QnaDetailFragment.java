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
        answerRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("forum_answers");
        questionRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("forum_questions");

        // Load Question Data
        if (getArguments() != null) {
            questionId = getArguments().getString("questionId");
            questionUserId = getArguments().getString("userId");

            tvTitle.setText(getArguments().getString("title"));
            tvContent.setText(getArguments().getString("content"));
            long time = getArguments().getLong("timestamp");
            loadQuestionUserProfile(questionUserId, time);

            qFileUrl = getArguments().getString("fileUrl");
            qFileName = getArguments().getString("fileName");

            // Default to false if not found
            isSolved = getArguments().getBoolean("solved", false);
            updateSolvedUI(); // Update button appearance immediately
            btnMarkSolved.setOnClickListener(v -> toggleSolvedStatus());

            loadAvatar(questionUserId, ivAvatar);
            checkOwnership();

            updateAttachmentButtonUI(); // Refactored to a method so we can call it after edit
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

        // Attach for ANSWER
        btnAttachFileToAnswer.setOnClickListener(v -> {
            isEditMode = false; // Important: Set flag to false
            UploadMaterialBottom uploadDialog = new UploadMaterialBottom();
            uploadDialog.show(getChildFragmentManager(), "UploadAnswer");
        });
    }

    private void loadQuestionUserProfile(String uid, long timestamp) {
        if (uid == null) return;

        DatabaseReference db = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        // Check Student First
        db.child("student_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("username").getValue(String.class);
                    String imgUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    updateHeaderUI(name, imgUrl, timestamp);
                } else {
                    // Check Tutor Second
                    db.child("tutor_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot tutorSnap) {
                            if (tutorSnap.exists()) {
                                String name = tutorSnap.child("username").getValue(String.class);
                                String imgUrl = tutorSnap.child("profileImageUrl").getValue(String.class);
                                updateHeaderUI(name, imgUrl, timestamp);
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

        // Update Text
        tvInfo.setText(name + " • " + TimeHelper.getMalaysiaTime(timestamp));

        // Update Avatar
        if (imgUrl != null) {
            int resId = getResources().getIdentifier(imgUrl, "drawable", requireContext().getPackageName());
            if (resId != 0) ivAvatar.setImageResource(resId);
            else ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
        } else {
            ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    // --- UPLOAD CALLBACK ---
    @Override
    public void onUploadSuccess(String fileId, String fileName) {
        if (isEditMode) {
            // Logic for EDIT QUESTION
            this.tempEditFileId = fileId;
            this.tempEditFileName = fileName;
            if (tvEditFileStatus != null) {
                tvEditFileStatus.setText("Selected: " + fileName);
                tvEditFileStatus.setVisibility(View.VISIBLE);
            }
        } else {
            // Logic for POST ANSWER
            this.tempAnswerFileId = fileId;
            this.tempAnswerFileName = fileName;
            if (tvAnswerFileName != null) {
                tvAnswerFileName.setText("Attached: " + fileName);
                tvAnswerFileName.setVisibility(View.VISIBLE);
            }
        }
    }

    // --- EDIT DIALOG LOGIC ---
    private void showEditQuestionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        // Use the same layout as posting a question
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_post_question, null);
        builder.setView(view);

        EditText etEditTitle = view.findViewById(R.id.etQuestionTitleInput);
        EditText etEditContent = view.findViewById(R.id.etQuestionContentInput);
        Button btnAttach = view.findViewById(R.id.btnAttachFile);
        Button btnSubmit = view.findViewById(R.id.btnSubmitQuestion);

        // This is the TextView inside the dialog
        tvEditFileStatus = view.findViewById(R.id.tvSelectedFileName);

        // 1. Initialize data
        etEditTitle.setText(tvTitle.getText().toString());
        etEditContent.setText(tvContent.getText().toString());

        // Initialize temp edit variables with current data
        tempEditFileId = qFileUrl;
        tempEditFileName = qFileName;

        if (tempEditFileName != null && !TextUtils.isEmpty(tempEditFileName)) {
            tvEditFileStatus.setText("Current: " + tempEditFileName);
        } else {
            tvEditFileStatus.setText("No file attached");
        }

        btnSubmit.setText("Update Question");

        AlertDialog dialog = builder.create();

        // 2. Attach File in Edit Mode
        btnAttach.setOnClickListener(v -> {
            isEditMode = true; // Important: Set flag to true
            UploadMaterialBottom uploadDialog = new UploadMaterialBottom();
            uploadDialog.show(getChildFragmentManager(), "UploadEdit");
        });

        // 3. Submit Update
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

        // Add file info (whether it's the old one or a newly uploaded one)
        // Note: Use keys "fileUrl" and "fileName" as per your database model
        updates.put("fileUrl", tempEditFileId);
        updates.put("fileName", tempEditFileName);

        questionRef.child(questionId).updateChildren(updates).addOnSuccessListener(aVoid -> {
            // Update UI instantly
            tvTitle.setText(title);
            tvContent.setText(content);

            // Update local variables so download button works
            qFileUrl = tempEditFileId;
            qFileName = tempEditFileName;
            updateAttachmentButtonUI();

            Toast.makeText(getContext(), "Question Updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Update Failed", Toast.LENGTH_SHORT).show();
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

    // --- EXISTING HELPER METHODS ---

    private void postAnswer() {
        String content = etAnswerContent.getText().toString().trim();
        String link = etSolutionLink.getText().toString().trim();

        if (TextUtils.isEmpty(content)) {
            etAnswerContent.setError("Content required");
            return;
        }

        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        userRef.child("student_profiles").child(uid).child("username").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                saveToFirebase(uid, task.getResult().getValue(String.class), content, link);
            } else {
                userRef.child("tutor_profiles").child(uid).child("username").get().addOnCompleteListener(task2 -> {
                    String name = "User";
                    if (task2.isSuccessful() && task2.getResult().getValue() != null) name = task2.getResult().getValue(String.class);
                    saveToFirebase(uid, name, content, link);
                });
            }
        });
    }

    private void saveToFirebase(String uid, String username, String content, String link) {
        String key = answerRef.push().getKey();
        Answer answer = new Answer(key, questionId, uid, username, content, link, System.currentTimeMillis(), tempAnswerFileId, tempAnswerFileName);

        answerRef.child(key).setValue(answer).addOnSuccessListener(unused -> {
            etAnswerContent.setText("");
            etSolutionLink.setText("");
            tempAnswerFileId = null;
            tempAnswerFileName = null;
            if(tvAnswerFileName != null) tvAnswerFileName.setText("");
            Toast.makeText(getContext(), "Solution Posted!", Toast.LENGTH_SHORT).show();
        });
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
                if (answerList.isEmpty()) {
                    tvNoAnswers.setVisibility(View.VISIBLE);
                    rvAnswers.setVisibility(View.GONE); // Optional: Hide list if empty
                } else {
                    tvNoAnswers.setVisibility(View.GONE);
                    rvAnswers.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
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

    private void checkOwnership() {
        String currentUid = CurrentUser.getInstance().getUid();
        boolean isOwner = (currentUid != null && questionUserId != null && currentUid.equals(questionUserId));
        btnEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnMarkSolved.setVisibility(isOwner ? View.VISIBLE : View.GONE);
    }

    private void deleteQuestion() {
        if (questionId != null) {
            questionRef.child(questionId).removeValue();
            getParentFragmentManager().popBackStack();
            Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadFile(String fileId) {
        String fullUrl = ENDPOINT + "/storage/buckets/" + BUCKET_ID + "/files/" + fileId + "/view?project=" + PROJECT_ID;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Cannot open file", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSolvedStatus() {
        if (questionId == null) return;

        // Toggle local state
        isSolved = !isSolved;

        // Update Firebase
        questionRef.child(questionId).child("solved").setValue(isSolved)
                .addOnSuccessListener(aVoid -> {
                    updateSolvedUI(); // Update UI on success
                    String msg = isSolved ? "Marked as Solved" : "Marked as Unsolved";
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Revert on failure
                    isSolved = !isSolved;
                    updateSolvedUI();
                    Toast.makeText(getContext(), "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSolvedUI() {
        if (btnMarkSolved == null) return;

        if (isSolved) {
            // STATE: SOLVED
            btnMarkSolved.setText("Mark as Unsolved");
            btnMarkSolved.setIconResource(R.drawable.outline_check_circle_24); // Use your outline Icon
            // Change color to Pastel Green (Success)
            btnMarkSolved.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.pastelGreen, null)
                    )
            );
        } else {
            // STATE: UNSOLVED
            btnMarkSolved.setText("Mark as Solved");
            btnMarkSolved.setIconResource(R.drawable.baseline_check_circle_24); // Use your filled Icon
            // Change color to Light Sky Blue (Actionable) or Grey
            btnMarkSolved.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.lightSkyBlue, null)
                    )
            );
        }
    }
}