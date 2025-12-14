package com.example.mad_edumatch.tutor;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.upload.UploadMaterialBottom; // Import upload dialog
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

// Implement UploadListener
public class TutorEditFreeLessonFragment extends Fragment implements UploadMaterialBottom.UploadListener {

    private EditText etTitle, etDesc, etVideo;
    private Button btnUploadMaterial; // New button
    private TextView tvFileStatus; // To show current/new file
    private MaterialButton btnSaveChanges;

    private String lessonId;

    // File Tracking
    private String currentMatUrl, currentMatName; // Existing data
    private String newMatUrl = null, newMatName = null; // New data if uploaded

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tutor_fragment_edit_free_lesson, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Bind Views
        etTitle = view.findViewById(R.id.etEditTitle);
        etDesc = view.findViewById(R.id.etEditDesc);
        etVideo = view.findViewById(R.id.etEditVideo);
        btnUploadMaterial = view.findViewById(R.id.btnEditUploadMaterial);
        tvFileStatus = view.findViewById(R.id.tvEditFileStatus);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);

        if (getArguments() != null) {
            lessonId = getArguments().getString("lessonId");
            etTitle.setText(getArguments().getString("currentTitle"));
            etDesc.setText(getArguments().getString("currentDesc"));
            etVideo.setText(getArguments().getString("currentVideo"));

            // Store old file data
            currentMatUrl = getArguments().getString("currentMatUrl");
            currentMatName = getArguments().getString("currentMatName");

            if (!TextUtils.isEmpty(currentMatName)) {
                tvFileStatus.setText("Current File: " + currentMatName);
            } else {
                tvFileStatus.setText("No file attached previously.");
            }
        }

        // Open Upload Dialog
        btnUploadMaterial.setOnClickListener(v -> {
            UploadMaterialBottom bottomSheet = new UploadMaterialBottom();
            bottomSheet.show(getChildFragmentManager(), "EditUpload");
        });

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    @Override
    public void onUploadSuccess(String fileUrl, String fileName) {
        // Callback when new file is uploaded
        this.newMatUrl = fileUrl;
        this.newMatName = fileName;
        tvFileStatus.setText("New File Attached: " + fileName);
        tvFileStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    }

    private void saveChanges() {
        String newTitle = etTitle.getText().toString().trim();
        String newDesc = etDesc.getText().toString().trim();
        String newVideo = etVideo.getText().toString().trim();

        if (TextUtils.isEmpty(newTitle) || TextUtils.isEmpty(newDesc)) {
            Toast.makeText(getContext(), "Title and Description required", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("free_lessons").child(lessonId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", newTitle);
        updates.put("description", newDesc);
        updates.put("videoLink", newVideo);

        // Logic: Use new file if uploaded, otherwise keep old file
        if (newMatUrl != null) {
            updates.put("materialUrl", newMatUrl);
            updates.put("materialName", newMatName);
        } else {
            // Keep existing (no change needed in DB unless we want to explicitly set it)
        }

        ref.updateChildren(updates).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Lesson Updated", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            getParentFragmentManager().popBackStack(); // Go back twice to refresh list
        });
    }
}