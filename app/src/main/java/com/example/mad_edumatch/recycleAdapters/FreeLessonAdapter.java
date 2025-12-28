package com.example.mad_edumatch.recycleAdapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.FreeLesson;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class FreeLessonAdapter extends RecyclerView.Adapter<FreeLessonAdapter.ViewHolder> {

    private List<FreeLesson> list;
    private OnLessonClickListener listener;
    private int itemLimit = 0; // 0 means "Show All"

    // Database URL
    private static final String FIREBASE_URL = "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app";

    public interface OnLessonClickListener { void onLessonClick(FreeLesson lesson); }

    public FreeLessonAdapter(List<FreeLesson> list, OnLessonClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_free_lesson, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FreeLesson lesson = list.get(position);

        // 1. Basic Info
        holder.tvTitle.setText(lesson.getTitle());
        holder.tvDesc.setText(lesson.getDescription());

        // 2. Set Placeholder (Old Name) immediately so it's not empty
        String oldName = lesson.getTutorName() != null ? lesson.getTutorName() : "Tutor";
        holder.tvTutor.setText("By " + oldName + " • " + lesson.getDurationMinutes() + " mins");

        // 3. Kudos Count
        int actualLikes = (lesson.getLikes() != null) ? lesson.getLikes().size() : 0;
        holder.tvLikes.setText("❤️ " + actualLikes + " Kudos");

        // 4. Button Click
        holder.btnWatch.setOnClickListener(v -> listener.onLessonClick(lesson));

        // 5. DYNAMIC FETCH: Get fresh username from Tutor Profiles
        String tutorId = lesson.getTutorId();
        if (tutorId != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL)
                    .getReference("tutor_profiles").child(tutorId);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Safety check: ensure view is still bound to this position
                    if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;

                    String freshName = null;
                    if (snapshot.exists()) {
                        if (snapshot.hasChild("username")) {
                            freshName = snapshot.child("username").getValue(String.class);
                        } else if (snapshot.hasChild("tutorName")) {
                            freshName = snapshot.child("tutorName").getValue(String.class);
                        }
                    }

                    // Update UI if we found a new name
                    if (freshName != null) {
                        holder.tvTutor.setText("By " + freshName + " • " + lesson.getDurationMinutes() + " mins");
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    @Override
    public int getItemCount() {
        if (list == null) return 0;
        if (itemLimit == 0) return list.size();
        return Math.min(list.size(), itemLimit);
    }

    public void setLimit(int limit) {
        this.itemLimit = limit;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTutor, tvDesc, tvLikes;
        Button btnWatch;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvLessonTitle);
            tvTutor = itemView.findViewById(R.id.tvTutorName);
            tvDesc = itemView.findViewById(R.id.tvDescription);
            tvLikes = itemView.findViewById(R.id.tvLikeCount);
            btnWatch = itemView.findViewById(R.id.btnWatchLesson);
        }
    }
}