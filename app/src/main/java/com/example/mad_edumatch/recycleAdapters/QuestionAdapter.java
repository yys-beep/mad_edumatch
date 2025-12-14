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
    private DatabaseReference userDbRef; // To fetch avatars

    public interface OnQuestionClickListener {
        void onQuestionClick(Question question);
    }

    public QuestionAdapter(Context context, List<Question> list, OnQuestionClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
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
        holder.tvAuthorDate.setText(q.getUserName() + " • " + TimeHelper.getMalaysiaTime(q.getTimestamp()));

        // Status Logic
        if (q.isSolved()) {
            holder.tvStatus.setText("SOLVED");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            holder.tvStatus.setText("UNSOLVED");
            holder.tvStatus.setTextColor(Color.parseColor("#FF6B6B")); // Red
        }

        // --- FIX: FETCH AVATAR MANUALLY ---
        loadAvatar(q.getUserId(), holder.ivAvatar);

        // Click Listeners
        holder.itemView.setOnClickListener(v -> listener.onQuestionClick(q));
        holder.btnView.setOnClickListener(v -> listener.onQuestionClick(q));
    }

    private void loadAvatar(String userId, ImageView imageView) {
        if (userId == null) return;

        // Try finding user in 'student_profiles'
        userDbRef.child("student_profiles").child(userId).child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    setAvatarImage(snapshot.getValue(String.class), imageView);
                } else {
                    // If not found, try 'tutor_profiles'
                    userDbRef.child("tutor_profiles").child(userId).child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot s2) {
                            if (s2.exists() && s2.getValue() != null) {
                                setAvatarImage(s2.getValue(String.class), imageView);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setAvatarImage(String imageName, ImageView iv) {
        if (imageName != null) {
            int resId = AvatarManager.getAvatarResourceId(imageName);
            if (resId != 0) iv.setImageResource(resId);
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
            tvAuthorDate = itemView.findViewById(R.id.tvAuthorDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPreview = itemView.findViewById(R.id.tvQuestionPreview);
            btnView = itemView.findViewById(R.id.btnViewQuestion);
        }
    }
}