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
import com.example.mad_edumatch.firebaseModels.FreeLesson;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.upload.UploadMaterialBottom;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TutorUploadFreeLessonFragment extends Fragment implements UploadMaterialBottom.UploadListener {

    private EditText etTitle, etDesc, etVideoLink, etDuration; // Added etDuration
    private Button btnUploadMaterial, btnPost;
    private TextView tvStatus;

    private String tempFileUrl = null;
    private String tempFileName = null;

    private DatabaseReference lessonRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tutor_fragment_upload_free_lesson, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTitle = view.findViewById(R.id.etLessonTitle);
        etDesc = view.findViewById(R.id.etLessonDesc);
        etVideoLink = view.findViewById(R.id.etVideoLink);
        etDuration = view.findViewById(R.id.etDuration); // Bind view

        btnUploadMaterial = view.findViewById(R.id.btnUploadMaterial);
        btnPost = view.findViewById(R.id.btnPostLesson);
        tvStatus = view.findViewById(R.id.tvMaterialStatus);

        lessonRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("free_lessons");

        btnUploadMaterial.setOnClickListener(v -> {
            UploadMaterialBottom bottomSheet = new UploadMaterialBottom();
            bottomSheet.show(getChildFragmentManager(), "UploadLessonMaterial");
        });

        btnPost.setOnClickListener(v -> postLesson());
    }

    @Override
    public void onUploadSuccess(String fileUrl, String fileName) {
        this.tempFileUrl = fileUrl;
        this.tempFileName = fileName;
        tvStatus.setText("Material Attached: " + fileName);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    }

    private void postLesson() {
        String title = etTitle.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String video = etVideoLink.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(desc)) {
            Toast.makeText(getContext(), "Title and Description required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse Duration (Default to 5 mins if empty)
        long durationMinutes = 0;
        if (!TextUtils.isEmpty(durationStr)) {
            try {
                durationMinutes = Long.parseLong(durationStr);
            } catch (NumberFormatException e) {
                durationMinutes = 0;
            }
        }

        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference tutorRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_profiles").child(uid);

        long finalDuration = durationMinutes; // Need final for inner class

        tutorRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String tutorName = "Tutor";
                if (snapshot.exists()) {
                    tutorName = snapshot.getValue(String.class);
                }
                saveToFirebase(uid, tutorName, title, desc, video, finalDuration);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveToFirebase(String uid, String name, String title, String desc, String video, long duration) {
        String key = lessonRef.push().getKey();
        if (key == null) return;

        // Save with new duration field
        FreeLesson lesson = new FreeLesson(key, uid, name, title, desc, video, tempFileUrl, tempFileName, System.currentTimeMillis(), 0, duration);

        lessonRef.child(key).setValue(lesson).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Free Lesson Posted!", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}