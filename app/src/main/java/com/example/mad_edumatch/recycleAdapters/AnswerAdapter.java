package com.example.mad_edumatch.recycleAdapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.Answer;
import com.example.mad_edumatch.helper.TimeHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class AnswerAdapter extends RecyclerView.Adapter<AnswerAdapter.ViewHolder> {

    private Context context;
    private List<Answer> list;
    private OnAnswerClickListener listener;

    // Interface for click events
    public interface OnAnswerClickListener {
        void onAnswerClick(Answer answer);
    }

    public AnswerAdapter(Context context, List<Answer> list, OnAnswerClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.qna_item_answer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Answer answer = list.get(position);

        // --- FIX: Use getUserName() to match your new Model/Database ---
        holder.tvUser.setText(answer.getUserName());

        holder.tvContent.setText(answer.getContent());

        String timeStr = TimeHelper.getMalaysiaTime(answer.getTimestamp());
        holder.tvDate.setText(timeStr);

        // Show attachment indicator if exists
        if (answer.getAttachmentUrl() != null && !answer.getAttachmentUrl().isEmpty()) {
            holder.tvAttachment.setVisibility(View.VISIBLE);
            holder.tvAttachment.setText("📎 Attachment Available");
        } else {
            holder.tvAttachment.setVisibility(View.GONE);
        }

        // Load Avatar (By Resource Name)
        loadAvatar(answer.getUserId(), holder.ivAvatar);

        // CLICK LISTENER: Go to Solution Detail
        holder.itemView.setOnClickListener(v -> listener.onAnswerClick(answer));
    }

    private void loadAvatar(String uid, ImageView iv) {
        if (uid == null) return;
        DatabaseReference db = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        // Check Student Profile first
        db.child("student_profiles").child(uid).child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    setDrawableAvatar(snapshot.getValue(String.class), iv);
                } else {
                    // Check Tutor Profile
                    db.child("tutor_profiles").child(uid).child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot s2) {
                            if (s2.exists()) setDrawableAvatar(s2.getValue(String.class), iv);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void setDrawableAvatar(String imageName, ImageView iv) {
        if (imageName != null && !imageName.isEmpty()) {
            int resId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
            if (resId != 0) {
                iv.setImageResource(resId);
            } else {
                iv.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvContent, tvAttachment, tvDate;
        ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvAnswerUser);
            tvContent = itemView.findViewById(R.id.tvAnswerContent);
            tvAttachment = itemView.findViewById(R.id.tvAnswerAttachment);

            // Make sure this ID matches what is in your qna_item_answer.xml
            tvDate = itemView.findViewById(R.id.tvAnswerDetailDate);

            ivAvatar = itemView.findViewById(R.id.ivAnswerAvatar);
        }
    }
}