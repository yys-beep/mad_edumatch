package com.example.mad_edumatch.recycleAdapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager; // Import this
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.StudentRequest;
import com.example.mad_edumatch.student.EditStudentRequestDialog;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class StudentRequestAdapter extends RecyclerView.Adapter<StudentRequestAdapter.ViewHolder> {

    private Context context;
    private List<StudentRequest> requestList;
    private FragmentManager fragmentManager; // Needed for Dialog

    // Constructor needs FragmentManager now
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
        holder.tvLevel.setText(req.getLevel()); // Ensure getLevel() exists in model
        holder.tvArea.setText(req.getArea());
        holder.tvBudget.setText("RM " + req.getBudget() + "/hr");
        holder.tvMode.setText(req.getDeliveryMode() + " (" + req.getLearningMode() + ")");
        holder.tvDesc.setText(req.getDescription());

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

        // EDIT BUTTON LOGIC (Opens the Dialog)
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
        TextView tvSubject, tvLevel, tvArea, tvBudget, tvMode, tvDesc;
        Button btnDelete, btnEdit; // Add btnEdit

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvBudget = itemView.findViewById(R.id.tvBudget);
            tvMode = itemView.findViewById(R.id.tvModes);
            tvDesc = itemView.findViewById(R.id.tvDescription);

            btnDelete = itemView.findViewById(R.id.btnDeleteRequest);
            btnEdit = itemView.findViewById(R.id.btnEditRequest); // Bind View
        }
    }
}