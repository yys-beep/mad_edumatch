package com.example.mad_edumatch.freeLesson;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
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
import com.example.mad_edumatch.firebaseModels.LessonComment; // Corrected Model
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.recycleAdapters.LessonCommentAdapter; // Corrected Adapter
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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

    // --- VIEWS ---
    private TextView tvTitle, tvDesc, tvTimerStatus, tvKudosCount;
    private ProgressBar progressBar;
    private MaterialButton btnMarkComplete, btnWatchVideo, btnDownloadMaterial, btnGiveKudos;
    private LinearLayout layoutOwnerActions;
    private LinearLayout layoutParticipation; // Added for Owner hide/show
    private RecyclerView rvComments;
    private EditText etCommentInput;
    private ImageButton btnSendComment, btnAttachFile;
    private TextView tvAttachmentPreview;
    private TextView tvNoComments;

    // --- DATA ---
    private String lessonId, videoUrl, materialUrl, materialName, tutorId; // Added materialName
    private boolean isCompleted = false;
    private boolean isLiked = false;
    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    // --- ATTACHMENT DATA ---
    private Uri selectedFileUri;
    private String selectedFileName;

    // --- APPWRITE CONFIG ---
    private static final String PROJECT_ID = "693c16f700198f0a2ed3";
    private static final String BUCKET_ID = "693c1807002ab38e1751";
    private static final String APPWRITE_ENDPOINT_FILE = "https://sgp.cloud.appwrite.io/v1/storage/buckets/" + BUCKET_ID + "/files";
    private static final String APPWRITE_VIEW_ENDPOINT = "https://sgp.cloud.appwrite.io/v1/storage/buckets/" + BUCKET_ID + "/files/%s/view?project=" + PROJECT_ID + "&mode=admin";

    // --- ADAPTERS ---
    private List<LessonComment> commentList;
    private LessonCommentAdapter commentAdapter;

    // --- TIMER ---
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
            if (elapsed >= requiredDurationMs) {
                unlockCompletion();
            } else {
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    // --- FILE PICKER ---
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    selectedFileName = getFileName(selectedFileUri);
                    if (selectedFileUri != null) {
                        tvAttachmentPreview.setText("📎 " + selectedFileName);
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

        // 1. Bind Views
        tvTitle = view.findViewById(R.id.tvDetailTitle);
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
        layoutParticipation = view.findViewById(R.id.layoutParticipation); // Bind new view
        btnAttachFile = view.findViewById(R.id.btnAttachFile);
        tvAttachmentPreview = view.findViewById(R.id.tvAttachmentPreview);

        // 2. Get Arguments
        if (getArguments() != null) {
            lessonId = getArguments().getString("lessonId");
            tvTitle.setText(getArguments().getString("title"));
            videoUrl = getArguments().getString("videoUrl");
            materialUrl = getArguments().getString("materialUrl");
            materialName = getArguments().getString("materialName", "Download Material"); // Get material name from args

            long durationMins = getArguments().getLong("duration", 0);
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

        // Initial Material Name update
        if (materialName != null) {
            btnDownloadMaterial.setText(materialName);
        }

        // 3. Listeners
        btnWatchVideo.setOnClickListener(v -> openLink(videoUrl));
        btnDownloadMaterial.setOnClickListener(v -> openLink(materialUrl));
        btnMarkComplete.setOnClickListener(v -> saveParticipationToFirebase());
        btnGiveKudos.setOnClickListener(v -> toggleKudos());

        btnAttachFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });

        btnSendComment.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                uploadFileWithOkHttpAndPost();
            } else {
                prepareAndPostComment(null, null);
            }
        });

        // 4. Load Data
        loadLessonDetails(); // This calls checkOwnerActions()
        checkPreviousParticipation();
        loadKudosStatus();
        loadComments();
        countParticipation(); // Load completion count
    }

    // =========================================================
    // SECTION 0: LOAD DETAILS & OWNER ACTIONS
    // =========================================================
    public void loadLessonDetails() {
        if (lessonId == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("free_lessons").child(lessonId);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Update main details
                    String dbTitle = snapshot.child("title").getValue(String.class);
                    if (dbTitle != null) tvTitle.setText(dbTitle);

                    videoUrl = snapshot.child("videoLink").getValue(String.class);
                    materialUrl = snapshot.child("materialUrl").getValue(String.class);
                    tutorId = snapshot.child("tutorId").getValue(String.class);

                    String dbDesc = snapshot.child("description").getValue(String.class);
                    tvDesc.setText((dbDesc != null && !dbDesc.isEmpty()) ? dbDesc : "No description provided.");

                    // Update Material Name in button text
                    materialName = snapshot.child("materialName").getValue(String.class);
                    if (materialName != null && !materialName.isEmpty()) {
                        btnDownloadMaterial.setText("Download: " + materialName);
                    } else {
                        btnDownloadMaterial.setText("Download Material");
                    }

                    checkOwnerActions(); // Update visibility based on tutorId
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Inside FreeLessonDetailFragment.java

    private void checkOwnerActions() {
        String currentUid = CurrentUser.getInstance().getUid();

        if (currentUid != null && currentUid.equals(tutorId)) {
            // ... (Hide/Show participation layout) ...
            layoutParticipation.setVisibility(View.GONE);
            layoutOwnerActions.setVisibility(View.VISIBLE);
            layoutOwnerActions.removeAllViews();

            // --- EDIT BUTTON: CALL DIALOG ---
            MaterialButton btnEdit = new MaterialButton(getContext());
            btnEdit.setText("Edit Lesson");
            btnEdit.setBackgroundColor(getResources().getColor(R.color.pastelBlue));

            btnEdit.setOnClickListener(v -> {
                // Launch the DialogFragment
                EditLessonDialogFragment dialog = EditLessonDialogFragment.newInstance(
                        lessonId,
                        tvTitle.getText().toString(),
                        tvDesc.getText().toString(),
                        videoUrl,
                        materialUrl,
                        materialName // Pass the material name/display text
                );
                // Use getChildFragmentManager() for DialogFragment to prevent leaks
                dialog.show(getChildFragmentManager(), "EditLessonDialog");
            });

            // ... (Delete Button setup) ...
            MaterialButton btnDelete = new MaterialButton(getContext());
            btnDelete.setText("Delete Lesson");
            btnDelete.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            btnDelete.setOnClickListener(v -> confirmDeleteLesson());

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);

            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(params);
            deleteParams.setMarginStart(16);

            btnEdit.setLayoutParams(params);
            btnDelete.setLayoutParams(deleteParams);


            layoutOwnerActions.addView(btnEdit);
            layoutOwnerActions.addView(btnDelete);

            // 3. Display Completion Count for Tutor
            countParticipation();
        } else {
            layoutParticipation.setVisibility(View.VISIBLE);
            layoutOwnerActions.setVisibility(View.GONE);
        }
    }

    private void countParticipation() {
        if (lessonId == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("lesson_participation").child(lessonId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long completedCount = 0;

                // Tutor's UID (used to exclude the tutor's own record)
                String currentTutorId = CurrentUser.getInstance().getUid();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String studentId = ds.child("studentId").getValue(String.class);
                    Boolean isCompleted = ds.child("isCompleted").getValue(Boolean.class);

                    // Count only if completed AND the UID is NOT the tutor's ID
                    if (Boolean.TRUE.equals(isCompleted) && !studentId.equals(currentTutorId)) {
                        completedCount++;
                    }
                }

                // Display count
                String status = completedCount + " students have completed this lesson.";

                // We reuse tvTimerStatus to show the count when it's not showing the timer
                if (layoutOwnerActions.getVisibility() == View.VISIBLE) {
                    tvTimerStatus.setText(status);
                    tvTimerStatus.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void confirmDeleteLesson() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Lesson")
                .setMessage("Are you sure? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseDatabase.getInstance(FIREBASE_URL)
                            .getReference("free_lessons").child(lessonId).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Lesson Deleted", Toast.LENGTH_SHORT).show();
                                if (getActivity() != null) getActivity().onBackPressed();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================
    // SECTION 1: KUDOS
    // =========================================================
    private void loadKudosStatus() {
        if (lessonId == null) return;
        // Get UID safely
        String uid = CurrentUser.getInstance().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("free_lessons").child(lessonId).child("likes");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // CRITICAL: Check if Fragment is still attached before accessing resources (like getResources())
                if (!isAdded()) {
                    return;
                }

                // 1. Count: Gets the total number of children under the 'likes' node.
                long count = snapshot.getChildrenCount();
                tvKudosCount.setText(count + " Kudos");

                // 2. User Like Status: Checks if the current user's UID exists
                if (uid != null && snapshot.hasChild(uid)) {
                    isLiked = true;
                    btnGiveKudos.setIconResource(R.drawable.baseline_thumb_up_24);
                    btnGiveKudos.setText("Liked");
                    // Using non-deprecated getColor
                    btnGiveKudos.setBackgroundColor(getResources().getColor(R.color.pastelGreen, null));
                } else {
                    isLiked = false;
                    btnGiveKudos.setIconResource(R.drawable.outline_thumb_up_24);
                    btnGiveKudos.setText("Like");
                    btnGiveKudos.setBackgroundColor(getResources().getColor(R.color.white, null));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load kudos: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load likes.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void toggleKudos() {
        if (lessonId == null) return;
        String uid = CurrentUser.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(getContext(), "Please log in to like this lesson.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference likesRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("free_lessons").child(lessonId).child("likes");

        // Read the current state of the user's like key
        likesRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Snapshot here represents the user's specific like key (uid)
                if (snapshot.exists()) {
                    // If it exists, remove it (Unlike)
                    snapshot.getRef().removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Like removed.", Toast.LENGTH_SHORT).show());
                } else {
                    // If it doesn't exist, set it (Like)
                    snapshot.getRef().setValue(true)
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Lesson liked!", Toast.LENGTH_SHORT).show());
                }
                // Note: The main loadKudosStatus() listener handles the UI update
                // after this change is committed to Firebase.
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ToggleKudos", "Failed to read like state: " + error.getMessage());
            }
        });
    }

    // =========================================================
    // SECTION 2: COMMENTS
    // =========================================================
    private void loadComments() {
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        commentList = new ArrayList<>();

        // Initialize LessonCommentAdapter
        commentAdapter = new LessonCommentAdapter(commentList, new LessonCommentAdapter.OnCommentActionListener() {
            @Override
            public void onDeleteClick(String commentId) {
                deleteCommentFromFirebase(commentId);
            }

            @Override
            public void onReplyClick(String userName) {
                String replyText = "@" + userName + " ";
                etCommentInput.setText(replyText);
                etCommentInput.setSelection(etCommentInput.getText().length());
                etCommentInput.requestFocus();
            }

            @Override
            public void onAttachmentClick(String url) {
                openLink(url);
            }
        });

        rvComments.setAdapter(commentAdapter);

        if (lessonId == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("free_lesson_comments");

        Query query = ref.orderByChild("lessonId").equalTo(lessonId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    LessonComment comment = ds.getValue(LessonComment.class);
                    if (comment != null) {
                        comment.setCommentId(ds.getKey());
                        commentList.add(comment);
                    }
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    commentAdapter.notifyDataSetChanged();
                    // Optional: Scroll to bottom on new message
                    if (commentList.isEmpty()) {
                        tvNoComments.setVisibility(View.VISIBLE);
                        rvComments.setVisibility(View.GONE);
                    } else {
                        tvNoComments.setVisibility(View.GONE);
                        rvComments.setVisibility(View.VISIBLE);
                        rvComments.smoothScrollToPosition(commentList.size() - 1);
                    }
                });
                commentAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Comments", "Failed: " + error.getMessage());
            }
        });
    }

    private void deleteCommentFromFirebase(String commentId) {
        if (commentId == null) return;
        FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("free_lesson_comments").child(commentId).removeValue();
    }

    // =========================================================
    // SECTION 3: UPLOAD & POST
    // =========================================================
    private void uploadFileWithOkHttpAndPost() {
        // ... (Existing uploadFileWithOkHttpAndPost logic remains the same) ...
        if (getContext() == null) return;

        Toast.makeText(getContext(), "Uploading attachment...", Toast.LENGTH_SHORT).show();
        btnSendComment.setEnabled(false);

        File file = getFileFromUri(selectedFileUri);
        if (file == null) {
            Toast.makeText(getContext(), "Error reading file", Toast.LENGTH_SHORT).show();
            btnSendComment.setEnabled(true);
            return;
        }

        OkHttpClient client = new OkHttpClient();
        String mimeType = "application/octet-stream";
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) mimeType = "application/pdf";
        else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) mimeType = "image/jpeg";
        else if (name.endsWith(".png")) mimeType = "image/png";

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fileId", "unique()")
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse(mimeType)))
                .build();

        Request request = new Request.Builder()
                .url(APPWRITE_ENDPOINT_FILE)
                .addHeader("X-Appwrite-Project", PROJECT_ID)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    btnSendComment.setEnabled(true);
                    Toast.makeText(getContext(), "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                new Handler(Looper.getMainLooper()).post(() -> {
                    btnSendComment.setEnabled(true);
                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String fileId = json.getString("$id");
                            prepareAndPostComment(selectedFileName, fileId);
                        } catch (JSONException e) {
                            Toast.makeText(getContext(), "Parsing Error", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Server Error: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private File getFileFromUri(Uri uri) {
        // ... (Existing getFileFromUri logic remains the same) ...
        if (getContext() == null || uri == null) return null;
        try {
            String fileName = getFileName(uri);
            File tempFile = new File(getContext().getCacheDir(), fileName);
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.close();
            is.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileName(Uri uri) {
        // ... (Existing getFileName logic remains the same) ...
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if(index >= 0) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void prepareAndPostComment(String attachmentName, String attachmentUrl) {
        // ... (Existing prepareAndPostComment logic remains the same) ...
        String content = etCommentInput.getText().toString().trim();
        if (TextUtils.isEmpty(content) && attachmentUrl == null) return;

        String uid = CurrentUser.getInstance().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("student_profiles").child(uid).child("username");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String userName = snapshot.exists() ? snapshot.getValue(String.class) : "Anonymous";
                postCommentToFirebase(uid, userName, content, attachmentName, attachmentUrl);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                postCommentToFirebase(uid, "Anonymous", content, attachmentName, attachmentUrl);
            }
        });
    }

    private void postCommentToFirebase(String uid, String userName, String content, String attName, String attUrl) {
        // ... (Existing postCommentToFirebase logic remains the same) ...
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("free_lesson_comments");

        String commentId = ref.push().getKey();

        LessonComment newComment = new LessonComment(
                commentId,
                lessonId,
                uid,
                userName,
                content,
                System.currentTimeMillis(),
                attName,
                attUrl
        );

        if (commentId != null) {
            ref.child(commentId).setValue(newComment).addOnSuccessListener(unused -> {
                etCommentInput.setText("");
                selectedFileUri = null;
                selectedFileName = null;
                tvAttachmentPreview.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Comment Posted!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    // =========================================================
    // SECTION 4: UTILS
    // =========================================================
    @Override
    public void onResume() {
        super.onResume();
        String currentUid = CurrentUser.getInstance().getUid();
        boolean isTutor = currentUid != null && currentUid.equals(tutorId);

        if (requiredDurationMs > 0 && !isCompleted && !isTutor) {
            if (sessionStartTime == 0) {
                sessionStartTime = System.currentTimeMillis();
            }
            timerHandler.post(timerRunnable);
        }
    }

    @Override
    public void onPause() {
        // ... (Existing onPause logic remains the same) ...
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void updateTimerUI(long elapsedMillis) {
        String currentUid = CurrentUser.getInstance().getUid();
        // NEW CHECK: If the current user is the tutor, do nothing.
        if (currentUid != null && currentUid.equals(tutorId)) {
            return;
        }

        // ... (rest of the timer calculation logic) ...
        if (elapsedMillis > requiredDurationMs) elapsedMillis = requiredDurationMs;
        int progress = (int) ((elapsedMillis * 100) / requiredDurationMs);
        progressBar.setProgress(progress);
        long remaining = requiredDurationMs - elapsedMillis;
        long min = (remaining / 1000) / 60;
        long sec = (remaining / 1000) % 60;
        if (elapsedMillis < requiredDurationMs) {
            tvTimerStatus.setText(String.format(Locale.getDefault(), "Study time remaining: %02d:%02d", min, sec));
        }
    }

    private void unlockCompletion() {
        String currentUid = CurrentUser.getInstance().getUid();
        // NEW CHECK: If the current user is the tutor, do nothing.
        if (currentUid != null && currentUid.equals(tutorId)) {
            return;
        }

        // ... (rest of the completion logic) ...
        timerHandler.removeCallbacks(timerRunnable);
        progressBar.setProgress(100);
        tvTimerStatus.setText("Lesson Completed!");
        tvTimerStatus.setTextColor(getResources().getColor(R.color.pastelGreen));
        btnMarkComplete.setEnabled(true);
        btnMarkComplete.setAlpha(1.0f);
    }

    private void checkPreviousParticipation() {
        // ... (Existing checkPreviousParticipation logic remains the same) ...
        String uid = CurrentUser.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("lesson_participation").child(lessonId).child(uid);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && Boolean.TRUE.equals(snapshot.child("isCompleted").getValue(Boolean.class))) {
                    isCompleted = true;
                    unlockCompletion();
                    btnMarkComplete.setText("Completed");
                    btnMarkComplete.setEnabled(false);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveParticipationToFirebase() {
        // ... (Existing saveParticipationToFirebase logic remains the same) ...
        String uid = CurrentUser.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("lesson_participation").child(lessonId).child(uid);

        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        map.put("studentId", uid);
        map.put("completedAt", System.currentTimeMillis());
        map.put("isCompleted", true);

        ref.setValue(map).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Marked Complete!", Toast.LENGTH_SHORT).show();
            isCompleted = true;
            btnMarkComplete.setText("Completed ✅");
            btnMarkComplete.setEnabled(false);
        });
    }

    private void openLink(String urlOrId) {
        // ... (Existing openLink logic remains the same) ...
        if (urlOrId == null || urlOrId.isEmpty()) {
            Toast.makeText(getContext(), "No link provided", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalUrl = urlOrId;
        if (!urlOrId.startsWith("http")) {
            finalUrl = String.format(APPWRITE_VIEW_ENDPOINT, urlOrId);
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }

}