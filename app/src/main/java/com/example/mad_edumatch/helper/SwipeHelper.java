package com.example.mad_edumatch.helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mad_edumatch.R;

public class SwipeHelper {
    private final Context context;

    public SwipeHelper(Context context) {
        this.context = context;
    }

    public void paint(Canvas c, RecyclerView.ViewHolder viewHolder, float dX) {
        View itemView = viewHolder.itemView;
        Paint p = new Paint();

        // 1. Draw Red Background
        p.setColor(Color.parseColor("#D32F2F")); // Material Red
        RectF background;

        if (dX > 0) { // Swiping Right
            background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
        } else { // Swiping Left
            background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
        }
        c.drawRect(background, p);

        // 2. Draw Trash Icon
        Drawable icon = ContextCompat.getDrawable(context, R.drawable.baseline_delete_24);
        if (icon != null) {
            int itemHeight = itemView.getBottom() - itemView.getTop();
            int intrinsicWidth = icon.getIntrinsicWidth();
            int intrinsicHeight = icon.getIntrinsicWidth();

            int iconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int iconMargin = (itemHeight - intrinsicHeight) / 2;
            int iconLeft, iconRight;

            if (dX > 0) { // Right Swipe Icon Position
                iconLeft = itemView.getLeft() + iconMargin;
                iconRight = itemView.getLeft() + iconMargin + intrinsicWidth;
            } else { // Left Swipe Icon Position
                iconLeft = itemView.getRight() - iconMargin - intrinsicWidth;
                iconRight = itemView.getRight() - iconMargin;
            }

            icon.setBounds(iconLeft, iconTop, iconRight, iconTop + intrinsicHeight);
            icon.draw(c);
        }
    }
}