package com.example.mad_edumatch.qna;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.Question;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.recycleAdapters.QuestionAdapter;
import com.example.mad_edumatch.upload.UploadMaterialBottom;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QnaForumFragment extends Fragment implements UploadMaterialBottom.UploadListener {

    private RecyclerView recyclerView;
    private QuestionAdapter adapter;
    private List<Question> fullList, displayList;
    private DatabaseReference dbRef;

    private EditText etSearch;
    private Button btnSearch, btnPostQuestion, btnMyQuestions;
    private boolean isShowingMyQuestions = false;

    // Upload variables
    private String tempFileUrl = null, tempFileName = null;
    private TextView tvDialogFileName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.qna_forum_fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.etSearchQuestion);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnPostQuestion = view.findViewById(R.id.btnPostQuestion);
        btnMyQuestions = view.findViewById(R.id.btnMyQuestions);
        recyclerView = view.findViewById(R.id.rvQuestions);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fullList = new ArrayList<>();
        displayList = new ArrayList<>();

        // Pass Context and Listener
        adapter = new QuestionAdapter(getContext(), displayList, this::openQuestionDetail);
        recyclerView.setAdapter(adapter);

        dbRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("forum_questions");

        loadQuestions();

        btnSearch.setOnClickListener(v -> performSearch());
        btnPostQuestion.setOnClickListener(v -> showPostDialog());
        btnMyQuestions.setOnClickListener(v -> toggleMyQuestions());
    }

    // === CRITICAL: Sending Data Correctly ===
    private void openQuestionDetail(Question q) {
        QnaDetailFragment detailFragment = new QnaDetailFragment();
        Bundle args = new Bundle();

        // Using standard keys
        args.putString("questionId", q.getQuestionId());
        args.putString("title", q.getTitle());
        args.putString("content", q.getContent());
        args.putString("username", q.getUserName());
        args.putString("userId", q.getUserId());
        args.putLong("timestamp", q.getTimestamp());
        args.putBoolean("solved", q.isSolved());

        args.putString("fileUrl", q.getFileUrl());
        args.putString("fileName", q.getFileName());

        detailFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    // Standard Loading Logic
    private void loadQuestions() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullList.clear();
                displayList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Question q = data.getValue(Question.class);
                    if (q != null) fullList.add(q);
                }

                // --- UPDATED SORTING LOGIC ---
                Collections.sort(fullList, (q1, q2) -> {
                    // Priority 1: Status (Unsolved comes FIRST)
                    // If q1 is solved and q2 is not, q1 goes to the bottom (return 1)
                    if (q1.isSolved() && !q2.isSolved()) return 1;
                    // If q1 is not solved and q2 is solved, q1 goes to the top (return -1)
                    if (!q1.isSolved() && q2.isSolved()) return -1;

                    // Priority 2: Timestamp (Latest comes FIRST)
                    // Compare q2 vs q1 for Descending order
                    return Long.compare(q2.getTimestamp(), q1.getTimestamp());
                });
                // -----------------------------

                displayList.addAll(fullList);
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Upload & Post Logic
    @Override
    public void onUploadSuccess(String fileUrl, String fileName) {
        this.tempFileUrl = fileUrl;
        this.tempFileName = fileName;
        if (tvDialogFileName != null) tvDialogFileName.setText(fileName);
    }

    private void showPostDialog() {
        tempFileUrl = null;
        tempFileName = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_post_question, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.etQuestionTitleInput);
        EditText etContent = dialogView.findViewById(R.id.etQuestionContentInput);
        Button btnAttach = dialogView.findViewById(R.id.btnAttachFile);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitQuestion);
        tvDialogFileName = dialogView.findViewById(R.id.tvSelectedFileName);

        AlertDialog dialog = builder.create();

        btnAttach.setOnClickListener(v -> {
            UploadMaterialBottom uploadDialog = new UploadMaterialBottom();
            uploadDialog.show(getChildFragmentManager(), "UploadDialog");
        });

        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(content)) {
                saveQuestion(title, content, dialog);
            }
        });
        dialog.show();
    }

    private void saveQuestion(String title, String content, AlertDialog dialog) {
        String uid = CurrentUser.getInstance().getUid();
        if (uid == null) return;

        // Generate Key
        String key = dbRef.push().getKey();

        // Create Question with placeholder name (e.g., empty string)
        // We only care about 'uid' now.
        Question q = new Question(key, uid, "", title, content, System.currentTimeMillis(), false, tempFileUrl, tempFileName);

        dbRef.child(key).setValue(q).addOnSuccessListener(v -> {
            dialog.dismiss();
        });
    }

    // Helper method to actually save the data to keep code clean
    private void pushQuestionToDatabase(String uid, String name, String title, String content, AlertDialog dialog) {
        String key = dbRef.push().getKey();
        // Create the Question object with the correct Name
        Question q = new Question(key, uid, name, title, content, System.currentTimeMillis(), false, tempFileUrl, tempFileName);

        dbRef.child(key).setValue(q).addOnSuccessListener(v -> {
            dialog.dismiss();
            // Optional: Show success message
            // Toast.makeText(getContext(), "Question posted!", Toast.LENGTH_SHORT).show();
        });
    }

    // Search & Filter
    private void performSearch() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        displayList.clear();
        if (TextUtils.isEmpty(query)) displayList.addAll(fullList);
        else {
            for (Question q : fullList) {
                if ((q.getTitle() != null && q.getTitle().toLowerCase().contains(query)) ||
                        (q.getContent() != null && q.getContent().toLowerCase().contains(query))) {
                    displayList.add(q);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void toggleMyQuestions() {
        isShowingMyQuestions = !isShowingMyQuestions;
        displayList.clear();
        if (isShowingMyQuestions) {
            btnMyQuestions.setText("Show All");
            String uid = CurrentUser.getInstance().getUid();
            for (Question q : fullList) if (uid != null && uid.equals(q.getUserId())) displayList.add(q);
        } else {
            btnMyQuestions.setText("My Questions");
            displayList.addAll(fullList);
        }
        adapter.notifyDataSetChanged();
    }
}