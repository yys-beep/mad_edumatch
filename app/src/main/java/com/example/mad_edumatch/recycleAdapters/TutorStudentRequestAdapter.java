package com.example.mad_edumatch.recycleAdapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.chat.ChatDetailFragment;
import com.example.mad_edumatch.firebaseModels.StudentRequest;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.helper.LocalizationHelper;
import com.example.mad_edumatch.helper.TimeHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;

public class TutorStudentRequestAdapter extends RecyclerView.Adapter<TutorStudentRequestAdapter.ViewHolder> {

    private List<StudentRequest> requestList;
    private Context context;

    public TutorStudentRequestAdapter(List<StudentRequest> requestList) {
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_card_tutor_view_student_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentRequest request = requestList.get(position);

        // --- 1. Translate Level ---
        int levelResId = LocalizationHelper.getLevelStringId(request.getLevel());
        if (levelResId != 0) {
            holder.tvLevel.setText(context.getString(levelResId));
        } else {
            holder.tvLevel.setText(request.getLevel());
        }

        // --- 2. Translate Modes ---
        String deliveryRaw = request.getDeliveryMode();
        String learningRaw = request.getLearningMode();
        String deliveryDisplay = deliveryRaw;
        String learningDisplay = learningRaw;

        int deliveryId = LocalizationHelper.getDeliveryModeStringId(deliveryRaw);
        if (deliveryId != 0) deliveryDisplay = context.getString(deliveryId);

        int learningId = LocalizationHelper.getLearningModeStringId(learningRaw);
        if (learningId != 0) learningDisplay = context.getString(learningId);

        // Uses format: "%1$s (%2$s)"
        holder.tvMode.setText(context.getString(R.string.mode_format_brackets, deliveryDisplay, learningDisplay));


        // --- Normal Binding ---
        holder.tvSubject.setText(request.getSubject());
        holder.tvArea.setText(request.getArea());

        // Uses format: "RM %.2f/hr"
        holder.tvBudget.setText(context.getString(R.string.budget_per_hour, request.getBudget()));

        holder.tvDesc.setText(request.getDescription());

        // --- Timestamp ---
        if (request.getTimestamp() > 0) {
            String formattedTime = TimeHelper.getMalaysiaTime(request.getTimestamp());
            // Uses format: "Posted: %s"
            holder.tvTimestamp.setText(context.getString(R.string.posted_time_format, formattedTime));
        } else {
            holder.tvTimestamp.setText(R.string.posted_just_now);
        }

        // --- Contact Button ---
        holder.btnContact.setOnClickListener(v -> {
            String currentUid = CurrentUser.getInstance().getUid();
            if (currentUid == null) {
                Toast.makeText(context, R.string.login_to_contact, Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentUid.equals(request.getStudentId())) {
                Toast.makeText(context, R.string.contact_self_error, Toast.LENGTH_SHORT).show();
                return;
            }
            fetchStudentNameAndOpenChat(request.getStudentId(), request.getSubject(), request.getRequestId());
        });
    }

    private void fetchStudentNameAndOpenChat(String studentId, String listingTitle, String listingId) {
        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_profiles").child(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String studentName = context.getString(R.string.student_default_name); // Fallback: "Student" or "Pelajar"
                        if (snapshot.exists()) {
                            if (snapshot.hasChild("username")) {
                                studentName = snapshot.child("username").getValue(String.class);
                            } else if (snapshot.hasChild("name")) {
                                studentName = snapshot.child("name").getValue(String.class);
                            }
                        }
                        openChatFragment(studentId, studentName, listingTitle, listingId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        openChatFragment(studentId, context.getString(R.string.student_default_name), listingTitle, listingId);
                    }
                });
    }

    private void openChatFragment(String targetUserId, String targetUserName, String listingTitle, String listingId) {
        ChatDetailFragment chatFragment = new ChatDetailFragment();
        Bundle args = new Bundle();
        args.putString("targetUserId", targetUserId);
        args.putString("targetUserName", targetUserName);
        args.putString("listingId", listingId);

        // Uses format: "Student Request: %s"
        args.putString("listingTitle", context.getString(R.string.student_request_title, listingTitle));

        chatFragment.setArguments(args);

        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, chatFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvLevel, tvArea, tvBudget, tvMode, tvDesc, tvTimestamp;
        Button btnContact;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvBudget = itemView.findViewById(R.id.tvBudget);
            tvMode = itemView.findViewById(R.id.tvModes);
            tvDesc = itemView.findViewById(R.id.tvDescription);
            btnContact = itemView.findViewById(R.id.btnContactStudent);
        }
    }
}