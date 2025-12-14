package com.example.mad_edumatch.recycleAdapters;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mad_edumatch.R;
import com.google.android.material.card.MaterialCardView;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {
    private final int[] avatarDrawables;
    private final AvatarClickListener listener;
    private int selectedPosition = 0; // Default selection

    public interface AvatarClickListener {
        void onAvatarSelected(int index);
    }

    public AvatarAdapter(int[] drawables, AvatarClickListener listener) {
        this.avatarDrawables = drawables;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        holder.bind(avatarDrawables[position], position == selectedPosition);
        holder.itemView.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);
            listener.onAvatarSelected(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return avatarDrawables.length;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    public static class AvatarViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        MaterialCardView cardAvatar;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            cardAvatar = itemView.findViewById(R.id.cardAvatar);
        }

        public void bind(int drawableId, boolean isSelected) {
            imgAvatar.setImageResource(drawableId);

            if (isSelected) {
                // 1. THICKER BORDER: Increased from 3 to 4dp
                int strokeWidthPx = dpToPx(4, itemView.getContext());
                cardAvatar.setStrokeWidth(strokeWidthPx);

                // 2. DARKER COLOR: Using a darker blue Hex code (e.g., #1565C0)
                // You can also use R.color.your_dark_blue_color
                cardAvatar.setStrokeColor(android.graphics.Color.parseColor("#1565C0"));
            } else {
                cardAvatar.setStrokeWidth(0);
            }
        }

        // Helper to convert dp to pixels
        private int dpToPx(int dp, Context context) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp,
                    context.getResources().getDisplayMetrics()
            );
        }
    }
}