package com.example.mad_edumatch.recycleAdapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.StudentRequest;
import com.example.mad_edumatch.helper.LocalizationHelper; // Import Helper
import com.example.mad_edumatch.student.EditStudentRequestDialog;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class StudentRequestAdapter extends RecyclerView.Adapter<StudentRequestAdapter.ViewHolder> {

    private Context context;
    private List<StudentRequest> requestList;
    private FragmentManager fragmentManager;

    public StudentRequestAdapter(Context context, List<StudentRequest> requestList, FragmentManager fragmentManager) {
        this.context = context;
        this.requestList = requestList;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card_student_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentRequest req = requestList.get(position);

        holder.tvSubject.setText(req.getSubject());
        holder.tvArea.setText(req.getArea());
        holder.tvDesc.setText(req.getDescription());

        // --- 1. Translate Level ---
        int levelResId = LocalizationHelper.getLevelStringId(req.getLevel());
        if (levelResId != 0) {
            holder.tvLevel.setText(context.getString(levelResId));
        } else {
            holder.tvLevel.setText(req.getLevel());
        }

        // --- 2. Format Budget (RM %.2f/hr) ---
        // Assuming budget is stored as Double, use format string.
        // If stored as String in your model, parse it or use simple concatenation if prefered.
        try {
            double budgetVal = Double.parseDouble(String.valueOf(req.getBudget()));
            holder.tvBudget.setText(context.getString(R.string.budget_per_hour, budgetVal));
        } catch (NumberFormatException e) {
            holder.tvBudget.setText("RM " + req.getBudget()); // Fallback
        }

        // --- 3. Translate Modes ---
        String deliveryRaw = req.getDeliveryMode();
        String learningRaw = req.getLearningMode();
        String deliveryDisplay = deliveryRaw;
        String learningDisplay = learningRaw;

        int deliveryId = LocalizationHelper.getDeliveryModeStringId(deliveryRaw);
        if (deliveryId != 0) deliveryDisplay = context.getString(deliveryId);

        int learningId = LocalizationHelper.getLearningModeStringId(learningRaw);
        if (learningId != 0) learningDisplay = context.getString(learningId);

        // Format: "%1$s (%2$s)" -> e.g., "Online (One-to-One)"
        holder.tvMode.setText(context.getString(R.string.mode_format_brackets, deliveryDisplay, learningDisplay));


        // --- 4. Time Ago Logic ---
        if (req.getTimestamp() != 0) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    req.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            // "Last updated: %s"
            holder.tvTimestamp.setText(context.getString(R.string.last_updated_format, timeAgo));
            holder.tvTimestamp.setVisibility(View.VISIBLE);
        } else {
            holder.tvTimestamp.setVisibility(View.GONE);
        }

        // DELETE BUTTON LOGIC
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.delete_request_title)
                    .setMessage(R.string.delete_request_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                                .getReference("student_requests")
                                .child(req.getRequestId())
                                .removeValue();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });

        // EDIT BUTTON LOGIC
        holder.btnEdit.setOnClickListener(v -> {
            EditStudentRequestDialog dialog = new EditStudentRequestDialog(req);
            dialog.show(fragmentManager, "EditRequest");
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvLevel, tvArea, tvBudget, tvMode, tvDesc, tvTimestamp;
        Button btnDelete, btnEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvBudget = itemView.findViewById(R.id.tvBudget);
            tvMode = itemView.findViewById(R.id.tvModes);
            tvDesc = itemView.findViewById(R.id.tvDescription);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnDelete = itemView.findViewById(R.id.btnDeleteRequest);
            btnEdit = itemView.findViewById(R.id.btnEditRequest);
        }
    }
}