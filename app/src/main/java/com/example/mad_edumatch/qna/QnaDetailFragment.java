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

    private String tempAnswerFileId = null;
    private String tempAnswerFileName = null;

    private boolean isEditMode = false;
    private String tempEditFileId = null;
    private String tempEditFileName = null;
    private TextView tvEditFileStatus;

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

        answerRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("forum_answers");
        questionRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("forum_questions");

        if (getArguments() != null) {
            questionId = getArguments().containsKey("sourceId") ?
                    getArguments().getString("sourceId") : getArguments().getString("questionId");

            if (getArguments().containsKey("title") && !getArguments().containsKey("sourceId")) {
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
                loadAnswers();
            } else if (questionId != null) {
                fetchQuestionDetailsFromFirebase(questionId);
            } else {
                Toast.makeText(getContext(), getString(R.string.error_invalid_question_id), Toast.LENGTH_SHORT).show();
            }
        }

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

    private void fetchQuestionDetailsFromFirebase(String qId) {
        this.questionId = qId;
        questionRef.child(qId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;

                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), getString(R.string.question_deleted), Toast.LENGTH_SHORT).show();
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
                loadAnswers();
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
                                updateHeaderUI(getString(R.string.unknown_user_default), null, timestamp);
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
                tvEditFileStatus.setText(getString(R.string.selected_file, fileName));
                tvEditFileStatus.setVisibility(View.VISIBLE);
            }
        } else {
            this.tempAnswerFileId = fileId;
            this.tempAnswerFileName = fileName;
            if (tvAnswerFileName != null) {
                tvAnswerFileName.setText(getString(R.string.attached_file, fileName));
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
            tvEditFileStatus.setText(getString(R.string.current_file, tempEditFileName));
        } else {
            tvEditFileStatus.setText(getString(R.string.no_file_attached));
        }

        btnSubmit.setText(getString(R.string.update_question_btn));
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
                Toast.makeText(getContext(), getString(R.string.fields_empty_error), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), getString(R.string.question_updated), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void updateAttachmentButtonUI() {
        if (qFileUrl != null && !qFileUrl.isEmpty()) {
            btnViewAttachment.setVisibility(View.VISIBLE);
            btnViewAttachment.setText(getString(R.string.download_file_btn, (qFileName != null ? qFileName : "File")));
            btnViewAttachment.setOnClickListener(v -> downloadFile(qFileUrl));
        } else {
            btnViewAttachment.setVisibility(View.GONE);
        }
    }

    private void postAnswer() {
        String content = etAnswerContent.getText().toString().trim();
        String link = etSolutionLink.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            etAnswerContent.setError(getString(R.string.content_required_error));
            return;
        }

        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();
        userRef.child("student_profiles").child(uid).child("username").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                String name = task.getResult().getValue(String.class);
                saveToFirebase(uid, name, content, link);
            } else {
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
            if (tvAnswerFileName != null) {
                tvAnswerFileName.setVisibility(View.GONE);
                tvAnswerFileName.setText("");
            }
            tempAnswerFileId = null;
            tempAnswerFileName = null;
            etAnswerContent.setText("");
            etSolutionLink.setText("");

            if (questionUserId != null && !questionUserId.equals(uid)) {
                sendAnswerNotification(questionUserId, uid, username, tvTitle.getText().toString());
            }
            Toast.makeText(getContext(), getString(R.string.solution_posted), Toast.LENGTH_SHORT).show();
        });
    }

    // Inside QnaDetailFragment.java
    private void sendAnswerNotification(String recipientId, String senderId, String answererName, String questionTitle) {
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("notifications").child(recipientId);

        HashMap<String, Object> data = new HashMap<>();
        data.put("title", getString(R.string.new_solution_notif_title));

        // Standard Q&A Action Type
        data.put("action_type", "NEW_SOLUTION");
        data.put("senderId", senderId);
        data.put("sourceId", questionId); // Points to the Question node
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

        questionRef.child(questionId).removeValue().addOnSuccessListener(aVoid -> {
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.delete_question_success), Toast.LENGTH_SHORT).show();
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.delete_failed_msg, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadFile(String fileId) {
        String fullUrl = ENDPOINT + "/storage/buckets/" + BUCKET_ID + "/files/" + fileId + "/view?project=" + PROJECT_ID;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)));
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.cannot_open_file), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSolvedStatus() {
        if (questionId == null) return;
        isSolved = !isSolved;
        questionRef.child(questionId).child("solved").setValue(isSolved).addOnSuccessListener(aVoid -> {
            updateSolvedUI();
            String msg = isSolved ? getString(R.string.marked_as_solved) : getString(R.string.marked_as_unsolved);
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSolvedUI() {
        if (btnMarkSolved == null) return;
        btnMarkSolved.setText(isSolved ? getString(R.string.btn_mark_unsolved) : getString(R.string.btn_mark_solved));
        btnMarkSolved.setIconResource(isSolved ? R.drawable.outline_check_circle_24 : R.drawable.baseline_check_circle_24);
        btnMarkSolved.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(isSolved ? R.color.pastelGreen : R.color.lightSkyBlue, null)));
    }
}