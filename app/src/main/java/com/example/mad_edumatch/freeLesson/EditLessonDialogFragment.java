package com.example.mad_edumatch.freeLesson;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.helper.LinkValidator;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditLessonDialogFragment extends DialogFragment {

    private EditText etTitle, etDescription, etVideoLink;
    private MaterialButton btnChooseMaterial, btnSaveChanges;
    private ProgressBar progressBar;
    private TextView tvMaterialStatus;
    private TextView tvCurrentMaterialName;

    private String lessonId;
    private String initialMaterialUrl;
    private String initialMaterialName;

    private String currentMaterialUrl;
    private String currentMaterialName;
    private Uri newFileUri;
    private String newFileName;
    private boolean isUploading = false;

    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String PROJECT_ID = "693c16f700198f0a2ed3";
    private static final String BUCKET_ID = "693c1807002ab38e1751";
    private static final String APPWRITE_ENDPOINT_FILE = "https://sgp.cloud.appwrite.io/v1/storage/buckets/" + BUCKET_ID + "/files";

    public static EditLessonDialogFragment newInstance(String lessonId, String title, String desc, String videoLink, String materialUrl, String materialName) {
        EditLessonDialogFragment fragment = new EditLessonDialogFragment();
        Bundle args = new Bundle();
        args.putString("lessonId", lessonId);
        args.putString("title", title);
        args.putString("description", desc);
        args.putString("videoLink", videoLink);
        args.putString("materialUrl", materialUrl);
        args.putString("materialName", materialName);
        fragment.setArguments(args);
        return fragment;
    }

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    newFileUri = result.getData().getData();
                    newFileName = getFileName(newFileUri);
                    if (newFileUri != null) {
                        tvMaterialStatus.setText(getString(R.string.new_file_selected, newFileName));
                        tvMaterialStatus.setVisibility(View.VISIBLE);
                        currentMaterialUrl = null;
                        currentMaterialName = null;
                        tvCurrentMaterialName.setVisibility(View.GONE);
                    }
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_free_lesson, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTitle = view.findViewById(R.id.etEditLessonTitle);
        etDescription = view.findViewById(R.id.etEditLessonDescription);
        etVideoLink = view.findViewById(R.id.etEditVideoUrl);

        btnChooseMaterial = view.findViewById(R.id.btnEditChooseMaterial);
        btnSaveChanges = view.findViewById(R.id.btnEditSaveChanges);
        progressBar = view.findViewById(R.id.progressBarEditUpload);
        tvMaterialStatus = view.findViewById(R.id.tvEditMaterialStatus);
        tvCurrentMaterialName = view.findViewById(R.id.tvEditCurrentMaterialName);

        loadDataFromArguments();

        btnChooseMaterial.setOnClickListener(v -> openFilePicker());
        btnSaveChanges.setOnClickListener(v -> handleSaveAttempt());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setLayout(width, height);
        }
    }

    private void loadDataFromArguments() {
        Bundle args = getArguments();
        if (args != null) {
            lessonId = args.getString("lessonId");
            etTitle.setText(args.getString("title"));
            etDescription.setText(args.getString("description"));
            etVideoLink.setText(args.getString("videoLink"));

            initialMaterialUrl = args.getString("materialUrl");
            initialMaterialName = args.getString("materialName");

            currentMaterialUrl = initialMaterialUrl;
            currentMaterialName = initialMaterialName;

            if (!TextUtils.isEmpty(currentMaterialUrl)) {
                String displayName = TextUtils.isEmpty(currentMaterialName) ? getString(R.string.file_attached) : currentMaterialName;
                tvCurrentMaterialName.setText(getString(R.string.current_material_label, displayName));
                tvCurrentMaterialName.setVisibility(View.VISIBLE);
            } else {
                tvCurrentMaterialName.setVisibility(View.GONE);
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "image/*", "video/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_new_material)));
    }

    private void handleSaveAttempt() {
        if (isUploading) {
            Toast.makeText(getContext(), getString(R.string.upload_progress), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(etTitle.getText())) {
            etTitle.setError(getString(R.string.title_required));
            return;
        }

        String linkInput = etVideoLink.getText().toString().trim();
        if (!TextUtils.isEmpty(linkInput)) {
            if (!LinkValidator.isValidUrl(linkInput)) {
                etVideoLink.setError(getString(R.string.invalid_link_error));
                etVideoLink.requestFocus();
                return;
            }
        }

        if (newFileUri != null) {
            uploadNewMaterialAndSave();
        } else {
            saveLessonChanges(currentMaterialUrl, currentMaterialName);
        }
    }

    private void uploadNewMaterialAndSave() {
        isUploading = true;
        progressBar.setVisibility(View.VISIBLE);
        btnSaveChanges.setEnabled(false);
        tvMaterialStatus.setText(getString(R.string.uploading_material));

        File file = getFileFromUri(newFileUri);
        if (file == null) {
            isUploading = false;
            progressBar.setVisibility(View.GONE);
            btnSaveChanges.setEnabled(true);
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
                    isUploading = false;
                    progressBar.setVisibility(View.GONE);
                    btnSaveChanges.setEnabled(true);
                    // Use getActivity/getContext check for safety inside async callback
                    if (getContext() != null) {
                        tvMaterialStatus.setText(getString(R.string.upload_failed_msg, e.getMessage()));
                        Toast.makeText(getContext(), getString(R.string.upload_failed_msg, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();
                new Handler(Looper.getMainLooper()).post(() -> {
                    isUploading = false;
                    progressBar.setVisibility(View.GONE);
                    btnSaveChanges.setEnabled(true);

                    if (getContext() == null) return;

                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String fileId = json.getString("$id");
                            saveLessonChanges(fileId, newFileName);

                        } catch (JSONException e) {
                            tvMaterialStatus.setText(getString(R.string.parsing_error));
                            Toast.makeText(getContext(), getString(R.string.error_parsing_response), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        tvMaterialStatus.setText(getString(R.string.server_error));
                        Toast.makeText(getContext(), getString(R.string.upload_failed_msg, response.message()), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void saveLessonChanges(String materialFileId, String materialFileName) {
        if (lessonId == null) return;

        DatabaseReference lessonRef = FirebaseDatabase.getInstance(FIREBASE_URL)
                .getReference("free_lessons").child(lessonId);

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("title", etTitle.getText().toString());
        updates.put("description", etDescription.getText().toString());
        updates.put("videoLink", etVideoLink.getText().toString());
        updates.put("materialUrl", materialFileId);
        updates.put("materialName", materialFileName);

        lessonRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), getString(R.string.lesson_updated_success), Toast.LENGTH_SHORT).show();
            dismiss();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), getString(R.string.save_failed_msg, e.getMessage()), Toast.LENGTH_LONG).show();
        });
    }

    private File getFileFromUri(Uri uri) {
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
            Toast.makeText(getContext(), getString(R.string.error_reading_file, e.getMessage()), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private String getFileName(Uri uri) {
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

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getParentFragment() instanceof FreeLessonDetailFragment) {
            ((FreeLessonDetailFragment) getParentFragment()).loadLessonDetails();
        }
    }
}