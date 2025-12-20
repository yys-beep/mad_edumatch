package com.example.mad_edumatch.recycleAdapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.Question;
import com.example.mad_edumatch.helper.AvatarManager;
import com.example.mad_edumatch.helper.TimeHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder> {

    private Context context;
    private List<Question> list;
    private OnQuestionClickListener listener;
    private DatabaseReference userDbRef; // To fetch user details

    public interface OnQuestionClickListener {
        void onQuestionClick(Question question);
    }

    public QuestionAdapter(Context context, List<Question> list, OnQuestionClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
        // Use the correct database URL
        this.userDbRef = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.qna_item_question, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question q = list.get(position);

        holder.tvTitle.setText(q.getTitle());
        holder.tvPreview.setText(q.getContent());

        // 1. Set a placeholder text while loading the real name
        holder.tvAuthorDate.setText("Loading... • " + TimeHelper.getMalaysiaTime(q.getTimestamp()));

        // Status Logic
        if (q.isSolved()) {
            holder.tvStatus.setText("SOLVED");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            holder.tvStatus.setText("UNSOLVED");
            holder.tvStatus.setTextColor(Color.parseColor("#FF6B6B")); // Red
        }

        // 2. Fetch Name AND Avatar dynamically using userId
        // This ensures if the user changes their name later, it updates here automatically.
        loadUserProfile(q.getUserId(), holder.tvAuthorDate, holder.ivAvatar, q.getTimestamp());

        // Click Listeners
        holder.itemView.setOnClickListener(v -> listener.onQuestionClick(q));
        holder.btnView.setOnClickListener(v -> listener.onQuestionClick(q));
    }

    /**
     * Searches both 'student_profiles' and 'tutor_profiles' for the given userId.
     */
    private void loadUserProfile(String userId, TextView tvAuthorDate, ImageView ivAvatar, long timestamp) {
        if (userId == null) {
            tvAuthorDate.setText("Unknown User • " + TimeHelper.getMalaysiaTime(timestamp));
            return;
        }

        // A. Check Student Profile First
        userDbRef.child("student_profiles").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // It is a Student
                    String name = snapshot.child("username").getValue(String.class);
                    String imgUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    updateUI(name, imgUrl, tvAuthorDate, ivAvatar, timestamp);
                } else {
                    // B. If not found in Student, Check Tutor Profile
                    userDbRef.child("tutor_profiles").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot tutorSnapshot) {
                            if (tutorSnapshot.exists()) {
                                // It is a Tutor
                                String name = tutorSnapshot.child("username").getValue(String.class);
                                String imgUrl = tutorSnapshot.child("profileImageUrl").getValue(String.class);
                                updateUI(name, imgUrl, tvAuthorDate, ivAvatar, timestamp);
                            } else {
                                // User ID exists in question but not in any profile (Account Deleted?)
                                updateUI("Unknown User", null, tvAuthorDate, ivAvatar, timestamp);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Helper to update the Views on the main thread once data is found
    private void updateUI(String name, String imgUrl, TextView tv, ImageView iv, long timestamp) {
        if (name == null || name.isEmpty()) name = "Unknown User";

        // Update the TextView with the fetched Name + existing Timestamp
        tv.setText(name + " • " + TimeHelper.getMalaysiaTime(timestamp));

        // Update Avatar
        if (imgUrl != null) {
            int resId = AvatarManager.getAvatarResourceId(imgUrl);
            if (resId != 0) iv.setImageResource(resId);
        } else {
            // Optional: Reset to default if no image found
            iv.setImageResource(R.drawable.avatar_1);
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPreview, tvAuthorDate, tvStatus;
        ImageView ivAvatar;
        Button btnView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivQuestionAvatar);
            tvTitle = itemView.findViewById(R.id.tvQuestionTitle);
            tvAuthorDate = itemView.findViewById(R.id.tvAuthorDate); // This now holds Name + Date
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPreview = itemView.findViewById(R.id.tvQuestionPreview);
            btnView = itemView.findViewById(R.id.btnViewQuestion);
        }
    }
}