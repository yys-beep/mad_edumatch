package com.example.mad_edumatch.upload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mad_edumatch.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadMaterialBottom extends BottomSheetDialogFragment {

    // Interface to communicate result back
    public interface UploadListener {
        void onUploadSuccess(String fileId, String fileName);
    }

    private UploadListener listener;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private Button btnChoose, btnCancel;

    // --- CONFIGURATION ---
    // TODO: Paste your Project ID here
    private static final String PROJECT_ID = "693c16f700198f0a2ed3";
    private static final String BUCKET_ID = "693c1807002ab38e1751";
    // NEW (Correct Singapore Endpoint):
    private static final String API_ENDPOINT = "https://sgp.cloud.appwrite.io/v1/storage/buckets/" + BUCKET_ID + "/files";

    private final ActivityResultLauncher<Intent> pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        processAndUpload(uri);
                    }
                }
            }
    );

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof UploadListener) {
            listener = (UploadListener) getParentFragment();
        } else {
            // Log warning but don't crash
            Log.w("Upload", "Parent fragment does not implement listener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_upload_material, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.uploadProgressBar);
        tvStatus = view.findViewById(R.id.tvUploadStatus);
        btnChoose = view.findViewById(R.id.btnChooseFile);
        btnCancel = view.findViewById(R.id.btnCancel);

        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "image/*", "video/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        pickerLauncher.launch(Intent.createChooser(intent, "Select Material"));
    }

    private void processAndUpload(Uri uri) {
        // 1. Get the file from URI
        File file = getFileFromUri(uri);
        if (file == null) {
            Toast.makeText(getContext(), "Error reading file", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Update UI
        progressBar.setVisibility(View.VISIBLE);
        btnChoose.setEnabled(false);
        tvStatus.setText("Uploading " + file.getName() + "...");

        // 3. Upload using OkHttp (Pure Java)
        uploadWithOkHttp(file);
    }

    private void uploadWithOkHttp(File file) {
        OkHttpClient client = new OkHttpClient();

        // Detect MIME type based on extension (fallback to generic stream)
        String mimeType = "application/octet-stream";
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) mimeType = "application/pdf";
        else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) mimeType = "image/jpeg";
        else if (name.endsWith(".png")) mimeType = "image/png";
        else if (name.endsWith(".mp4")) mimeType = "video/mp4";

        // Build the Multipart Request
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fileId", "unique()") // Let Appwrite generate ID
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse(mimeType)))
                .build();

        Request request = new Request.Builder()
                .url(API_ENDPOINT)
                .addHeader("X-Appwrite-Project", PROJECT_ID)
                // .addHeader("X-Appwrite-Key", "YOUR_API_KEY") // Only needed if read/write is restricted
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Return to Main Thread for UI updates
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded()) {
                        progressBar.setVisibility(View.GONE);
                        btnChoose.setEnabled(true);
                        tvStatus.setText("Upload Failed");
                        Toast.makeText(getContext(), "Network Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;

                    progressBar.setVisibility(View.GONE);

                    if (response.isSuccessful()) {
                        try {
                            // Parse JSON to get ID
                            JSONObject json = new JSONObject(responseBody);
                            String fileId = json.getString("$id");
                            String fileName = json.getString("name");

                            if (listener != null) {
                                listener.onUploadSuccess(fileId, fileName);
                            }

                            Toast.makeText(getContext(), "Attached Successfully!", Toast.LENGTH_SHORT).show();
                            dismiss();

                        } catch (JSONException e) {
                            tvStatus.setText("Parsing Error");
                            Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        btnChoose.setEnabled(true);
                        tvStatus.setText("Server Error");
                        Toast.makeText(getContext(), "Upload Failed: " + response.message(), Toast.LENGTH_LONG).show();
                        Log.e("Upload", "Error: " + responseBody);
                    }
                });
            }
        });
    }

    // --- Helper Methods ---

    private File getFileFromUri(Uri uri) {
        try {
            String fileName = getFileName(uri);
            File tempFile = new File(requireContext().getCacheDir(), fileName);
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
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
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = "temp_file";
        }
        return result;
    }
}