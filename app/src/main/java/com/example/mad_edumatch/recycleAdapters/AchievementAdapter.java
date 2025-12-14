package com.example.mad_edumatch.recycleAdapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private final List<String> achievements;
    private int itemLimit = 0;

    public AchievementAdapter(List<String> achievements) {
        this.achievements = achievements;
    }

    public void setLimit(int limit) {
        this.itemLimit = limit;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String achievement = achievements.get(position);
        holder.tvTitle.setText(achievement);
        // If you want a description, set it here. For now, hide it
        holder.tvDescription.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        if (achievements == null) return 0;
        if (itemLimit == 0) return achievements.size();
        return Math.min(achievements.size(), itemLimit);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAchievementTitle);
            tvDescription = itemView.findViewById(R.id.tvAchievementDescription);
        }
    }
}
