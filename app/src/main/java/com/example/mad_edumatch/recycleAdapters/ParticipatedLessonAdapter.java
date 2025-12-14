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

public class ParticipatedLessonAdapter extends RecyclerView.Adapter<ParticipatedLessonAdapter.ViewHolder> {

    private final List<FreeLesson> lessons;
    private int itemLimit = 0;
    private OnLessonClickListener listener;

    public interface OnLessonClickListener {
        void onLessonClick(FreeLesson lesson);
    }

    // Constructor MUST include the listener to enable the Watch button
    public ParticipatedLessonAdapter(List<FreeLesson> lessons, OnLessonClickListener listener) {
        this.lessons = lessons;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use the full lesson card layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_free_lesson, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FreeLesson lesson = lessons.get(position);

        // Bind all fields using the full card layout views
        holder.tvTitle.setText(lesson.getTitle());
        holder.tvDesc.setText(lesson.getDescription());

        // Format and display details
        String details = "By " + lesson.getTutorName() + " • "
                + lesson.getDurationMinutes() + " mins • "
                + TimeHelper.getMalaysiaTime(lesson.getTimestamp());
        holder.tvTutor.setText(details);

        // Calculate and display Kudos (Likes)
        int actualLikes = 0;
        if (lesson.getLikes() != null) {
            actualLikes = lesson.getLikes().size();
        }
        holder.tvLikes.setText("❤️ " + actualLikes + " Kudos");

        // Set click listener and ensure button visibility
        if (listener != null) {
            holder.btnWatch.setVisibility(View.VISIBLE);
            holder.btnWatch.setOnClickListener(v -> listener.onLessonClick(lesson));
        } else {
            holder.btnWatch.setVisibility(View.GONE);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Bind all views from item_card_free_lesson.xml
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

    public void setLimit(int limit) {
        this.itemLimit = limit;
    }

    @Override
    public int getItemCount() {
        if (itemLimit > 0 && itemLimit < lessons.size()) {
            return itemLimit;
        }
        return lessons.size();
    }
}