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
import com.example.mad_edumatch.firebaseModels.Comment;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.helper.TimeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private List<Comment> list;
    private OnDeleteListener deleteListener;
    private Context context;

    public interface OnDeleteListener {
        void onDeleteClick(String commentId);
    }

    public CommentAdapter(List<Comment> list, OnDeleteListener deleteListener) {
        this.list = list;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        // Links to the new XML above
        View v = LayoutInflater.from(context).inflate(R.layout.qna_item_comment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment c = list.get(position);

        // 1. Content
        holder.tvContent.setText(c.getContent());

        // 2. Date (Standardized)
        holder.tvDate.setText(TimeHelper.getMalaysiaTime(c.getTimestamp()));

        // 3. Username
        loadUsername(c.getUserId(), holder.tvAuthor);

        // 4. Avatar
        loadUserAvatar(c.getUserId(), holder.ivAvatar);

        // 5. Delete Logic
        String currentUid = CurrentUser.getInstance().getUid();
        if (currentUid != null && currentUid.equals(c.getUserId())) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> deleteListener.onDeleteClick(c.getCommentId()));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    private void loadUsername(String uid, TextView tvName) {
        if (uid == null) return;
        DatabaseReference db = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        db.child("student_profiles").child(uid).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvName.setText(snapshot.getValue(String.class));
                } else {
                    db.child("tutor_profiles").child(uid).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot tutorSnap) {
                            if (tutorSnap.exists()) {
                                tvName.setText(tutorSnap.getValue(String.class));
                            } else {
                                tvName.setText("Unknown User");
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadUserAvatar(String uid, ImageView imageView) {
        if (uid == null) { imageView.setImageResource(R.drawable.ic_launcher_foreground); return; }

        DatabaseReference db = FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        // Check Student first
        db.child("student_profiles").child(uid).child("profileImageUrl")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            setLocalImage(snapshot.getValue(String.class), imageView);
                        } else {
                            // Check Tutor second
                            db.child("tutor_profiles").child(uid).child("profileImageUrl")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot tutorSnap) {
                                            if (tutorSnap.exists()) {
                                                setLocalImage(tutorSnap.getValue(String.class), imageView);
                                            } else {
                                                imageView.setImageResource(R.drawable.ic_launcher_foreground);
                                            }
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setLocalImage(String imageName, ImageView iv) {
        if (!TextUtils.isEmpty(imageName)) {
            int resId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
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
        MaterialButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvDate = itemView.findViewById(R.id.tvCommentDate);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            ivAvatar = itemView.findViewById(R.id.ivCommentAvatar);
            btnDelete = itemView.findViewById(R.id.btnDeleteComment);
        }
    }
}