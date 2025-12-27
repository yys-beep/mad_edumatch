package com.example.mad_edumatch.recycleAdapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.format.DateUtils; // Import this for time formatting
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
        holder.tvLevel.setText(req.getLevel());
        holder.tvArea.setText(req.getArea());
        holder.tvBudget.setText("RM " + req.getBudget() + "/hr");
        holder.tvMode.setText(req.getDeliveryMode() + " (" + req.getLearningMode() + ")");
        holder.tvDesc.setText(req.getDescription());

        // --- NEW: TIME AGO LOGIC ---
        if (req.getTimestamp() != 0) {
            // This helper method converts the timestamp into "42 mins ago", "Yesterday", etc.
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    req.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            holder.tvTimestamp.setText("Last updated: " + timeAgo);
            holder.tvTimestamp.setVisibility(View.VISIBLE);
        } else {
            // For old items with no timestamp, hide or show default
            holder.tvTimestamp.setVisibility(View.GONE);
        }
        // ---------------------------

        // DELETE BUTTON LOGIC
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Request")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                                .getReference("student_requests")
                                .child(req.getRequestId())
                                .removeValue();
                    })
                    .setNegativeButton("No", null)
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
        TextView tvSubject, tvLevel, tvArea, tvBudget, tvMode, tvDesc, tvTimestamp; // Added tvTimestamp
        Button btnDelete, btnEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvBudget = itemView.findViewById(R.id.tvBudget);
            tvMode = itemView.findViewById(R.id.tvModes);
            tvDesc = itemView.findViewById(R.id.tvDescription);

            // --- BIND THE TIMESTAMP VIEW ---
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);

            btnDelete = itemView.findViewById(R.id.btnDeleteRequest);
            btnEdit = itemView.findViewById(R.id.btnEditRequest);
        }
    }
}