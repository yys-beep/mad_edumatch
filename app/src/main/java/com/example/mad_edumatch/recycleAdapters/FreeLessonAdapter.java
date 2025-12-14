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
import com.example.mad_edumatch.helper.TimeHelper;
import java.util.List;

public class FreeLessonAdapter extends RecyclerView.Adapter<FreeLessonAdapter.ViewHolder> {

    private List<FreeLesson> list;
    private OnLessonClickListener listener;
    // FIX 1: Default itemLimit remains 0 (indicating "show all")
    private int itemLimit = 0;

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
        holder.tvTitle.setText(lesson.getTitle());
        holder.tvDesc.setText(lesson.getDescription());
        holder.tvTutor.setText("By " + lesson.getTutorName() + " • " + lesson.getDurationMinutes() + " mins");

        int actualLikes = (lesson.getLikes() != null) ? lesson.getLikes().size() : 0;
        holder.tvLikes.setText("❤️ " + actualLikes + " Kudos");

        holder.btnWatch.setOnClickListener(v -> listener.onLessonClick(lesson));
    }

    // FIX 2: Correct logic for getItemCount
    @Override
    public int getItemCount() {
        if (list == null) return 0;

        // If limit is 0, it means "Show All".
        // Previously, Math.min(size, 0) was returning 0, hiding everything.
        if (itemLimit == 0) {
            return list.size();
        }

        // Otherwise, show the limit (or size if smaller)
        return Math.min(list.size(), itemLimit);
    }

    public void setLimit(int limit) {
        this.itemLimit = limit;
        // Don't notify here, let the Fragment notify to avoid conflicts
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