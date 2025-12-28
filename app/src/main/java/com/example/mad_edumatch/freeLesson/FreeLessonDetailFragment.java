package com.example.mad_edumatch.freeLesson;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.LessonComment;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.helper.LinkValidator;
import com.example.mad_edumatch.recycleAdapters.LessonCommentAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FreeLessonDetailFragment extends Fragment {

    private TextView tvTitle, tvDesc, tvTimerStatus, tvKudosCount, tvTutorName;
    private ProgressBar progressBar;
    private MaterialButton btnMarkComplete, btnWatchVideo, btnDownloadMaterial, btnGiveKudos;
    private LinearLayout layoutOwnerActions, layoutParticipation;
    private RecyclerView rvComments;
    private EditText etCommentInput;
    private ImageButton btnSendComment, btnAttachFile;
    private TextView tvAttachmentPreview, tvNoComments;

    private String lessonId, videoLink, materialUrl, materialName, tutorId, tutorNameStr;
    private long timestampPosted = 0;
    private boolean isCompleted = false;
    private boolean isLiked = false;
    private boolean hasCountedView = false;

    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";
    private Uri selectedFileUri;
    private String selectedFileName;

    private static final String PROJECT_ID = "693c16f700198f0a2ed3";
    private static final String BUCKET_ID = "693c1807002ab38e1751";
    private static final String APPWRITE_ENDPOINT_FILE = "https://sgp.cloud.appwrite.io/v1/storage/buckets/" + BUCKET_ID + "/files";
    private static final String APPWRITE_VIEW_ENDPOINT = "https://sgp.cloud.appwrite.io/v1/storage/buckets/" + BUCKET_ID + "/files/%s/view?project=" + PROJECT_ID + "&mode=admin";

    private List<LessonComment> commentList;
    private LessonCommentAdapter commentAdapter;

    private long requiredDurationMs = 300000;
    private long sessionStartTime = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isCompleted) return;
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - sessionStartTime;
            updateTimerUI(elapsed);
            if (elapsed >= requiredDurationMs) unlockCompletion();
            else timerHandler.postDelayed(this, 1000);
        }
    };

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    selectedFileName = getFileName(selectedFileUri);
                    if (selectedFileUri != null) {
                        tvAttachmentPreview.setText(getString(R.string.attachment_preview, selectedFileName));
                        tvAttachmentPreview.setVisibility(View.VISIBLE);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.free_lesson_fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTitle = view.findViewById(R.id.tvDetailTitle);
        tvTutorName = view.findViewById(R.id.tvTutorName);
        tvDesc = view.findViewById(R.id.tvDetailDesc);
        tvKudosCount = view.findViewById(R.id.tvKudosCount);
        btnGiveKudos = view.findViewById(R.id.btnGiveKudos);
        tvTimerStatus = view.findViewById(R.id.tvTimerStatus);
        progressBar = view.findViewById(R.id.progressBarTimer);
        btnMarkComplete = view.findViewById(R.id.btnMarkComplete);
        btnWatchVideo = view.findViewById(R.id.btnWatchVideo);
        btnDownloadMaterial = view.findViewById(R.id.btnDownloadMaterial);
        rvComments = view.findViewById(R.id.rvLessonComments);
        etCommentInput = view.findViewById(R.id.etCommentInput);
        btnSendComment = view.findViewById(R.id.btnSendComment);
        tvNoComments = view.findViewById(R.id.tvNoComments);
        layoutOwnerActions = view.findViewById(R.id.layoutOwnerActions);
        layoutParticipation = view.findViewById(R.id.layoutParticipation);
        btnAttachFile = view.findViewById(R.id.btnAttachFile);
        tvAttachmentPreview = view.findViewById(R.id.tvAttachmentPreview);

        if (getArguments() != null) {
            lessonId = getArguments().containsKey("sourceId") ?
                    getArguments().getString("sourceId") : getArguments().getString("lessonId");

            if (lessonId != null) {
                fetchLessonDetailsFromFirebase(lessonId);
            }
        }

        btnWatchVideo.setOnClickListener(v -> openLink(videoLink));
        btnDownloadMaterial.setOnClickListener(v -> openLink(materialUrl));
        btnMarkComplete.setOnClickListener(v -> saveParticipationToFirebase());
        btnGiveKudos.setOnClickListener(v -> toggleKudos());

        btnAttachFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });

        btnSendComment.setOnClickListener(v -> {
            if (selectedFileUri != null) uploadFileWithOkHttpAndPost();
            else prepareAndPostComment(null, null);
        });
    }

    private void setupTimerData(long durationMins) {
        if (durationMins <= 0) {
            requiredDurationMs = 0;
            unlockCompletion();
            tvTimerStatus.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } else {
            requiredDurationMs = durationMins * 60 * 1000;
            tvTimerStatus.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void fetchLessonDetailsFromFirebase(String id) {
        this.lessonId = id;
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("free_lessons").child(id);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;

                tvTitle.setText(snapshot.child("title").getValue(String.class));
                tutorId = snapshot.child("tutorId").getValue(String.class);

                String descText = snapshot.child("description").getValue(String.class);
                if (descText != null && !descText.isEmpty()) {
                    tvDesc.setText(descText);
                    tvDesc.setVisibility(View.VISIBLE);
                } else {
                    tvDesc.setText(getString(R.string.no_description));
                    tvDesc.setVisibility(View.VISIBLE);
                }

                tutorNameStr = snapshot.child("tutorName").getValue(String.class);
                Long ts = snapshot.child("timestamp").getValue(Long.class);
                String dateString = "";
                if (ts != null) {
                    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                    cal.setTimeInMillis(ts);
                    dateString = DateFormat.format("dd MMM yyyy, hh:mm a", cal).toString();
                }
                fetchLatestTutorName(tutorId, dateString);

                Long duration = snapshot.child("durationMinutes").getValue(Long.class);
                if (duration == null) {
                    duration = 5L;
                }
                setupTimerData(duration);

                videoLink = snapshot.child("videoLink").getValue(String.class);
                if (LinkValidator.isValidUrl(videoLink)) {
                    btnWatchVideo.setVisibility(View.VISIBLE);
                } else {
                    btnWatchVideo.setVisibility(View.GONE);
                }

                materialUrl = snapshot.child("materialUrl").getValue(String.class);
                materialName = snapshot.child("materialName").getValue(String.class);
                if (materialUrl != null && !materialUrl.trim().isEmpty()) {
                    btnDownloadMaterial.setVisibility(View.VISIBLE);
                    if (materialName != null) {
                        btnDownloadMaterial.setText(getString(R.string.download_format, materialName));
                    }
                } else {
                    btnDownloadMaterial.setVisibility(View.GONE);
                }

                checkOwnerActions();
                loadKudosStatus();
                loadComments();
                checkPreviousParticipation();
                countParticipation();
                incrementTutorViewCount(tutorId);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkOwnerActions() {
        String currentUid = CurrentUser.getInstance().getUid();
        if (currentUid != null && currentUid.equals(tutorId)) {
            layoutParticipation.setVisibility(View.GONE);
            layoutOwnerActions.setVisibility(View.VISIBLE);
            layoutOwnerActions.removeAllViews();

            MaterialButton btnEdit = new MaterialButton(getContext());
            btnEdit.setText(getString(R.string.edit_lesson));
            btnEdit.setBackgroundColor(getResources().getColor(R.color.pastelBlue));
            btnEdit.setOnClickListener(v -> {
                EditLessonDialogFragment dialog = EditLessonDialogFragment.newInstance(
                        lessonId, tvTitle.getText().toString(), tvDesc.getText().toString(),
                        videoLink, materialUrl, materialName);
                dialog.show(getChildFragmentManager(), "EditLessonDialog");
            });

            MaterialButton btnDelete = new MaterialButton(getContext());
            btnDelete.setText(getString(R.string.delete_lesson));
            btnDelete.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            btnDelete.setOnClickListener(v -> confirmDeleteLesson());

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            layoutOwnerActions.addView(btnEdit, p);

            LinearLayout.LayoutParams dP = new LinearLayout.LayoutParams(p);
            dP.setMarginStart(16);
            layoutOwnerActions.addView(btnDelete, dP);

            countParticipation();
        } else {
            layoutParticipation.setVisibility(View.VISIBLE);
            layoutOwnerActions.setVisibility(View.GONE);
        }
    }

    private void confirmDeleteLesson() {
        if (lessonId == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.delete_confirm_title))
                .setMessage(getString(R.string.delete_confirm_msg))
                .setPositiveButton(getString(R.string.delete_btn), (dialog, which) -> {
                    FirebaseDatabase.getInstance(FIREBASE_URL).getReference("free_lessons").child(lessonId)
                            .removeValue().addOnSuccessListener(aVoid -> {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), getString(R.string.deleted_toast), Toast.LENGTH_SHORT).show();
                                    if (getActivity() != null) getActivity().onBackPressed();
                                }
                            });
                }).setNegativeButton(getString(R.string.no), null).show();
    }

    private void countParticipation() {
        if (lessonId == null) return;
        FirebaseDatabase.getInstance(FIREBASE_URL).getReference("lesson_participation").child(lessonId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            if (Boolean.TRUE.equals(ds.child("isCompleted").getValue(Boolean.class))) count++;
                        }
                        if (isAdded() && layoutOwnerActions.getVisibility() == View.VISIBLE) {
                            tvTimerStatus.setText(getString(R.string.students_completed, count));
                            tvTimerStatus.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadKudosStatus() {
        if (lessonId == null) return;
        String uid = CurrentUser.getInstance().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("free_lessons").child(lessonId).child("likes");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                long count = snapshot.getChildrenCount();
                tvKudosCount.setText(getString(R.string.kudos_count_format, count));

                if (uid != null && snapshot.hasChild(uid)) {
                    isLiked = true;
                    btnGiveKudos.setText(getString(R.string.liked_btn));
                    btnGiveKudos.setIconResource(R.drawable.baseline_thumb_up_24);
                    btnGiveKudos.setBackgroundColor(getResources().getColor(R.color.pastelGreen, null));
                } else {
                    isLiked = false;
                    btnGiveKudos.setText(getString(R.string.like_btn));
                    btnGiveKudos.setIconResource(R.drawable.outline_thumb_up_24);
                    btnGiveKudos.setBackgroundColor(getResources().getColor(R.color.white, null));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void toggleKudos() {
        if (lessonId == null) return;
        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference likesRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("free_lessons").child(lessonId).child("likes");

        likesRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    snapshot.getRef().removeValue();
                } else {
                    likesRef.child(uid).setValue(true).addOnSuccessListener(aVoid -> fetchStudentNameAndNotify());
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchStudentNameAndNotify() {
        String uid = CurrentUser.getInstance().getUid();
        if (uid == null || tutorId == null) return;

        FirebaseDatabase.getInstance(FIREBASE_URL).getReference("student_profiles").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = "Someone";
                        if (snapshot.exists() && snapshot.hasChild("username")) {
                            name = snapshot.child("username").getValue(String.class);
                        }
                        sendKudosNotification(name);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sendKudosNotification(String name) {
        if (tutorId == null || CurrentUser.getInstance().getUid().equals(tutorId)) return;
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("notifications").child(tutorId);
        String title = tvTitle.getText().toString();
        String snippet = (title.length() > 20) ? title.substring(0, 20) + "..." : title;

        HashMap<String, Object> data = new HashMap<>();
        data.put("title", "New Kudos!");
        data.put("message", name + " liked your lesson: " + snippet);
        data.put("action_type", "OPEN_LESSON");
        data.put("sourceId", lessonId);
        data.put("timestamp", System.currentTimeMillis());
        data.put("isRead", false);
        ref.push().setValue(data);
    }

    private void loadComments() {
        if (lessonId == null) return;
        if (commentList == null) {
            commentList = new ArrayList<>();
            commentAdapter = new LessonCommentAdapter(commentList, new LessonCommentAdapter.OnCommentActionListener() {
                @Override public void onDeleteClick(String id) { deleteCommentFromFirebase(id); }
                @Override public void onReplyClick(String name) { etCommentInput.setText("@" + name + " "); etCommentInput.requestFocus(); }
                @Override public void onAttachmentClick(String url) { openLink(url); }
            });
            rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
            rvComments.setAdapter(commentAdapter);
        }

        FirebaseDatabase.getInstance(FIREBASE_URL).getReference("free_lesson_comments")
                .orderByChild("lessonId").equalTo(lessonId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        commentList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            LessonComment c = ds.getValue(LessonComment.class);
                            if (c != null) { c.setCommentId(ds.getKey()); commentList.add(c); }
                        }
                        if (isAdded()) {
                            commentAdapter.notifyDataSetChanged();
                            if (commentList.isEmpty()) {
                                tvNoComments.setText(getString(R.string.no_comments));
                                tvNoComments.setVisibility(View.VISIBLE);
                                rvComments.setVisibility(View.GONE);
                            } else {
                                tvNoComments.setVisibility(View.GONE);
                                rvComments.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void prepareAndPostComment(String attachmentName, String attachmentUrl) {
        String content = etCommentInput.getText().toString().trim();
        if (TextUtils.isEmpty(content) && attachmentUrl == null) return;
        String uid = CurrentUser.getInstance().getUid();

        FirebaseDatabase.getInstance(FIREBASE_URL).getReference("student_profiles").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = (snapshot.exists() && snapshot.hasChild("username")) ?
                                snapshot.child("username").getValue(String.class) : "Anonymous";
                        postCommentToFirebase(uid, name, content, attachmentName, attachmentUrl);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void postCommentToFirebase(String uid, String realName, String content, String attName, String attUrl) {
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("free_lesson_comments");
        String commentId = ref.push().getKey();
        LessonComment newComment = new LessonComment(commentId, lessonId, uid, realName, content, System.currentTimeMillis(), attName, attUrl);

        if (commentId != null) {
            ref.child(commentId).setValue(newComment).addOnSuccessListener(unused -> {
                etCommentInput.setText("");
                selectedFileUri = null;
                tvAttachmentPreview.setVisibility(View.GONE);
                Toast.makeText(getContext(), getString(R.string.comment_posted), Toast.LENGTH_SHORT).show();

                // --- ADD NOTIFICATION LOGIC HERE ---
                // Only notify if someone else (not the tutor) is commenting
                if (tutorId != null && !uid.equals(tutorId)) {
                    sendCommentNotification(realName);
                }
            });
        }
    }

    // Inside FreeLessonDetailFragment.java
    private void sendCommentNotification(String commenterName) {
        if (tutorId == null) return;

        DatabaseReference notifRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("notifications")
                .child(tutorId);

        HashMap<String, Object> data = new HashMap<>();
        data.put("title", getString(R.string.new_comment_notif_title));

        // We send a simple message as a fallback, but the Adapter will override it
        data.put("message", "New lesson comment");

        data.put("action_type", "LESSON_COMMENT");
        data.put("sourceId", lessonId);
        data.put("senderId", CurrentUser.getInstance().getUid());
        data.put("timestamp", System.currentTimeMillis());
        data.put("isRead", false);

        notifRef.push().setValue(data);
    }

    private void deleteCommentFromFirebase(String id) {
        FirebaseDatabase.getInstance(FIREBASE_URL).getReference("free_lesson_comments").child(id).removeValue();
    }

    private void uploadFileWithOkHttpAndPost() {
        if (getContext() == null) return;
        btnSendComment.setEnabled(false);
        File file = getFileFromUri(selectedFileUri);
        if (file == null) { btnSendComment.setEnabled(true); return; }

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("fileId", "unique()")
                .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse("application/octet-stream")))
                .build();

        Request request = new Request.Builder().url(APPWRITE_ENDPOINT_FILE).addHeader("X-Appwrite-Project", PROJECT_ID).post(requestBody).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { new Handler(Looper.getMainLooper()).post(() -> btnSendComment.setEnabled(true)); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body().string();
                new Handler(Looper.getMainLooper()).post(() -> {
                    btnSendComment.setEnabled(true);
                    if (response.isSuccessful()) {
                        try { prepareAndPostComment(selectedFileName, new JSONObject(body).getString("$id")); } catch (Exception e) {}
                    }
                });
            }
        });
    }

    private File getFileFromUri(Uri uri) {
        if (getContext() == null || uri == null) return null;
        try {
            File temp = new File(getContext().getCacheDir(), getFileName(uri));
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(temp);
            byte[] buf = new byte[1024]; int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            os.close(); is.close();
            return temp;
        } catch (Exception e) { return null; }
    }

    private String getFileName(Uri uri) {
        String res = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) res = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        }
        return res != null ? res : uri.getLastPathSegment();
    }

    @Override public void onResume() {
        super.onResume();
        if (requiredDurationMs > 0 && !isCompleted && !(tutorId != null && CurrentUser.getInstance().getUid().equals(tutorId))) {
            if (sessionStartTime == 0) sessionStartTime = System.currentTimeMillis();
            timerHandler.post(timerRunnable);
        }
    }

    @Override public void onPause() { super.onPause(); timerHandler.removeCallbacks(timerRunnable); }

    private void updateTimerUI(long elapsed) {
        if (CurrentUser.getInstance().getUid().equals(tutorId)) return;
        if (elapsed > requiredDurationMs) elapsed = requiredDurationMs;
        progressBar.setProgress((int) ((elapsed * 100) / requiredDurationMs));
        long rem = requiredDurationMs - elapsed;
        if (elapsed < requiredDurationMs) {
            tvTimerStatus.setText(getString(R.string.study_time_remaining, (rem / 1000) / 60, (rem / 1000) % 60));
        }
    }

    private void unlockCompletion() {
        if (CurrentUser.getInstance().getUid().equals(tutorId)) return;
        timerHandler.removeCallbacks(timerRunnable);
        progressBar.setProgress(100);
        tvTimerStatus.setText(getString(R.string.lesson_completed));
        tvTimerStatus.setTextColor(android.graphics.Color.parseColor("#333333"));
        btnMarkComplete.setEnabled(true); btnMarkComplete.setAlpha(1.0f);
    }

    private void checkPreviousParticipation() {
        String uid = CurrentUser.getInstance().getUid();
        FirebaseDatabase.getInstance(FIREBASE_URL).getReference("lesson_participation").child(lessonId).child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && Boolean.TRUE.equals(snapshot.child("isCompleted").getValue(Boolean.class))) {
                            isCompleted = true;
                            unlockCompletion();
                            btnMarkComplete.setText(getString(R.string.completed_btn));
                            btnMarkComplete.setIconResource(R.drawable.baseline_check_circle_24);
                            btnMarkComplete.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                            btnMarkComplete.setEnabled(false);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void saveParticipationToFirebase() {
        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;
        FirebaseDatabase.getInstance(FIREBASE_URL).getReference("student_profiles").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("username").exists() ? snapshot.child("username").getValue(String.class) : "A Student";
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("studentId", uid);
                        map.put("userName", name);
                        map.put("completedAt", System.currentTimeMillis());
                        map.put("isCompleted", true);
                        FirebaseDatabase.getInstance(FIREBASE_URL).getReference("lesson_participation").child(lessonId).child(uid)
                                .setValue(map).addOnSuccessListener(aVoid -> {
                                    isCompleted = true;
                                    btnMarkComplete.setText(getString(R.string.completed_btn));
                                    btnMarkComplete.setIconResource(R.drawable.baseline_check_circle_24);
                                    btnMarkComplete.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                                    btnMarkComplete.setEnabled(false);
                                    Toast.makeText(getContext(), getString(R.string.progress_saved), Toast.LENGTH_SHORT).show();
                                    incrementStudentsHelpedCount();
                                });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void openLink(String urlInput) {
        if (urlInput == null || urlInput.trim().isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.link_unavailable), Toast.LENGTH_SHORT).show();
            return;
        }

        String finalUrl = urlInput;
        if (LinkValidator.isValidUrl(urlInput)) {
            finalUrl = urlInput;
        } else if (!urlInput.contains(" ") && !urlInput.contains(".") && !urlInput.startsWith("http")) {
            finalUrl = String.format(APPWRITE_VIEW_ENDPOINT, urlInput);
        } else {
            Toast.makeText(getContext(), getString(R.string.invalid_link_format), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.unable_open_link), Toast.LENGTH_SHORT).show();
        }
    }

    private void incrementTutorViewCount(String tId) {
        if (tId == null || hasCountedView) return;
        String currentUid = CurrentUser.getInstance().getUid();
        if (currentUid != null && currentUid.equals(tId)) return;
        hasCountedView = true;

        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("tutor_profiles").child(tId).child("totalViews");

        ref.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer views = currentData.getValue(Integer.class);
                if (views == null) { currentData.setValue(1); } else { currentData.setValue(views + 1); }
                return Transaction.success(currentData);
            }
            @Override public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
        });
    }

    private void incrementStudentsHelpedCount() {
        if (tutorId == null) return;
        String currentUid = CurrentUser.getInstance().getUid();
        if (currentUid != null && currentUid.equals(tutorId)) return;

        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("tutor_profiles").child(tutorId).child("studentsHelped");

        ref.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer currentVal = currentData.getValue(Integer.class);
                if (currentVal == null) { currentData.setValue(1); } else { currentData.setValue(currentVal + 1); }
                return Transaction.success(currentData);
            }
            @Override public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
        });
    }

    public void loadLessonDetails() {
        if (lessonId != null) {
            fetchLessonDetailsFromFirebase(lessonId);
        }
    }

    private void fetchLatestTutorName(String tId, String dateString) {
        DatabaseReference userRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("tutor_profiles").child(tId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String latestName = "Unknown Tutor";
                if (snapshot.exists()) {
                    if (snapshot.hasChild("username")) {
                        latestName = snapshot.child("username").getValue(String.class);
                    } else if (snapshot.hasChild("tutorName")) {
                        latestName = snapshot.child("tutorName").getValue(String.class);
                    } else if (snapshot.hasChild("fullName")) {
                        latestName = snapshot.child("fullName").getValue(String.class);
                    }
                }
                if (!latestName.equals("Unknown Tutor")) {
                    tvTutorName.setText(getString(R.string.by_tutor_date, latestName, dateString));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}