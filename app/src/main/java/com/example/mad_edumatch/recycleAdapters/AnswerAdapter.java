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

        holder.tvContent.setText(answer.getContent());

        String timeStr = TimeHelper.getMalaysiaTime(answer.getTimestamp());
        holder.tvDate.setText(timeStr);

        // Attachment Logic
        if (answer.getAttachmentUrl() != null && !answer.getAttachmentUrl().isEmpty()) {
            holder.tvAttachment.setVisibility(View.VISIBLE);
            holder.tvAttachment.setText(context.getString(R.string.attachment_available));
        } else {
            holder.tvAttachment.setVisibility(View.GONE);
        }

        // --- NEW: Set placeholder & Fetch Live Profile ---
        holder.tvUser.setText(context.getString(R.string.loading_dot));
        loadUserProfile(answer.getUserId(), holder.tvUser, holder.ivAvatar);

        holder.itemView.setOnClickListener(v -> listener.onAnswerClick(answer));
    }

    // --- NEW METHOD: Fetches BOTH Name and Avatar ---
    private void loadUserProfile(String uid, TextView tvName, ImageView ivAvatar) {
        if (uid == null) return;
        DatabaseReference db = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        // 1. Check Student Profile
        db.child("student_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("username").getValue(String.class);
                    String imgUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    updateUI(name, imgUrl, tvName, ivAvatar);
                } else {
                    // 2. Check Tutor Profile
                    db.child("tutor_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot s2) {
                            if (s2.exists()) {
                                String name = s2.child("username").getValue(String.class);
                                String imgUrl = s2.child("profileImageUrl").getValue(String.class);
                                updateUI(name, imgUrl, tvName, ivAvatar);
                            } else {
                                updateUI(context.getString(R.string.unknown_user), null, tvName, ivAvatar);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void updateUI(String name, String imgUrl, TextView tvName, ImageView ivAvatar) {
        if (name == null) name = context.getString(R.string.unknown_user);
        tvName.setText(name);

        if (imgUrl != null && !imgUrl.isEmpty()) {
            int resId = context.getResources().getIdentifier(imgUrl, "drawable", context.getPackageName());
            if (resId != 0) {
                ivAvatar.setImageResource(resId);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
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
            // Ensure this ID matches your XML. Your previous code had 'tvAnswerDetailDate'
            tvDate = itemView.findViewById(R.id.tvAnswerDetailDate);
            ivAvatar = itemView.findViewById(R.id.ivAnswerAvatar);
        }
    }
}