package com.example.mad_edumatch.recycleAdapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.LessonComment; // Correct Model
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.helper.TimeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class LessonCommentAdapter extends RecyclerView.Adapter<LessonCommentAdapter.ViewHolder> {

    private List<LessonComment> list;
    private OnCommentActionListener actionListener;
    private Context context;

    public interface OnCommentActionListener {
        void onDeleteClick(String commentId);
        void onReplyClick(String userName);
        void onAttachmentClick(String url);
    }

    public LessonCommentAdapter(List<LessonComment> list, OnCommentActionListener listener) {
        this.list = list;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        // --- CORRECTED LAYOUT NAME ---
        View v = LayoutInflater.from(context).inflate(R.layout.item_lesson_comment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LessonComment c = list.get(position);

        // 1. Content
        holder.tvContent.setText(c.getContent());
        holder.tvDate.setText(TimeHelper.getMalaysiaTime(c.getTimestamp()));

        // 2. User Info
        loadUserInfo(c.getUserId(), holder.tvAuthor, holder.ivAvatar);

        // 3. Delete Logic
        String currentUid = CurrentUser.getInstance().getUid();
        if (currentUid != null && currentUid.equals(c.getUserId())) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> actionListener.onDeleteClick(c.getCommentId()));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        // 4. Reply Logic
        holder.btnReply.setOnClickListener(v -> {
            String name = holder.tvAuthor.getText().toString();
            actionListener.onReplyClick(name);
        });

        // 5. Attachment Logic
        if (!TextUtils.isEmpty(c.getAttachmentUrl())) {
            holder.btnAttachment.setVisibility(View.VISIBLE);
            String label = TextUtils.isEmpty(c.getAttachmentName()) ? "View Attachment" : c.getAttachmentName();
            holder.btnAttachment.setText(label);

            holder.btnAttachment.setOnClickListener(v -> actionListener.onAttachmentClick(c.getAttachmentUrl()));
        } else {
            holder.btnAttachment.setVisibility(View.GONE);
        }
    }

    private void loadUserInfo(String uid, TextView tvName, ImageView ivAvatar) {
        if (uid == null) { tvName.setText("Anonymous"); return; }

        DatabaseReference db = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        db.child("student_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    setUserData(snapshot, tvName, ivAvatar);
                } else {
                    db.child("tutor_profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot s2) {
                            if (s2.exists()) setUserData(s2, tvName, ivAvatar);
                            else tvName.setText("Unknown User");
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setUserData(DataSnapshot snapshot, TextView tv, ImageView iv) {
        String name = snapshot.child("username").getValue(String.class);
        if (name != null) tv.setText(name);

        String imgName = snapshot.child("profileImageUrl").getValue(String.class);
        if (imgName != null && !imgName.isEmpty()) {
            int resId = context.getResources().getIdentifier(imgName, "drawable", context.getPackageName());
            if (resId != 0) iv.setImageResource(resId);
            else iv.setImageResource(R.drawable.ic_launcher_foreground);
        } else {
            iv.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvDate, tvContent;
        ImageView ivAvatar;

        // --- ImageButtons based on XML ---
        ImageButton btnDelete, btnReply;

        // --- MaterialButton based on XML ---
        MaterialButton btnAttachment;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // --- View Bindings ---
            tvAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvDate = itemView.findViewById(R.id.tvCommentDate);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            ivAvatar = itemView.findViewById(R.id.ivCommentAvatar);

            // Bindings for ImageButtons (Delete and Reply)
            btnDelete = itemView.findViewById(R.id.btnDeleteComment);
            btnReply = itemView.findViewById(R.id.btnReplyComment);

            // Binding for MaterialButton (Attachment)
            btnAttachment = itemView.findViewById(R.id.btnCommentAttachment);
        }
    }
}