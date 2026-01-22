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
import com.example.mad_edumatch.helper.ListingDataHelper; // Import Helper
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

        // --- 1. Translate Level (Corrected) ---
        // Converts Key (e.g. "PRIMARY") to Display (e.g. "Rendah")
        String displayLevel = ListingDataHelper.getLevelDisplayName(context, req.getLevel());
        holder.tvLevel.setText(displayLevel);

        // --- 2. Format Budget ---
        try {
            double budgetVal = Double.parseDouble(String.valueOf(req.getBudget()));
            holder.tvBudget.setText(context.getString(R.string.budget_per_hour, budgetVal));
        } catch (NumberFormatException e) {
            holder.tvBudget.setText("RM " + req.getBudget());
        }

        // --- 3. Translate Modes (Corrected) ---
        String deliveryDisplay = ListingDataHelper.getDeliveryModeDisplayName(context, req.getDeliveryMode());
        String learningDisplay = ListingDataHelper.getLearningModeDisplayName(context, req.getLearningMode());

        // Format: "%1$s (%2$s)" -> e.g., "Online (One-to-One)"
        holder.tvMode.setText(context.getString(R.string.mode_format_brackets, deliveryDisplay, learningDisplay));

        // --- 4. Time Ago ---
        if (req.getTimestamp() != 0) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    req.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            holder.tvTimestamp.setText(context.getString(R.string.last_updated_format, timeAgo));
            holder.tvTimestamp.setVisibility(View.VISIBLE);
        } else {
            holder.tvTimestamp.setVisibility(View.GONE);
        }

        // Buttons
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