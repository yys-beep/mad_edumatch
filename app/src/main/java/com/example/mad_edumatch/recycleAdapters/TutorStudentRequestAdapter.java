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
import com.example.mad_edumatch.helper.TimeHelper; // IMPORT TimeHelper
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
        // Make sure this matches your XML file name exactly
        View view = LayoutInflater.from(context).inflate(R.layout.item_card_tutor_view_student_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentRequest request = requestList.get(position);

        holder.tvSubject.setText(request.getSubject());
        holder.tvLevel.setText(request.getLevel());
        holder.tvArea.setText(request.getArea());
        holder.tvBudget.setText(String.format(Locale.getDefault(), "RM %.2f/hr", request.getBudget()));
        holder.tvMode.setText(request.getDeliveryMode() + " (" + request.getLearningMode() + ")");
        holder.tvDesc.setText(request.getDescription());

        // --- NEW: BIND TIMESTAMP ---
        if (request.getTimestamp() > 0) {
            String formattedTime = TimeHelper.getMalaysiaTime(request.getTimestamp());
            holder.tvTimestamp.setText("Posted: " + formattedTime);
        } else {
            holder.tvTimestamp.setText("Posted: Just now");
        }
        // ---------------------------

        // --- CONTACT BUTTON LOGIC ---
        holder.btnContact.setOnClickListener(v -> {
            String currentUid = CurrentUser.getInstance().getUid();

            if (currentUid == null) {
                Toast.makeText(context, "Please login to contact", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUid.equals(request.getStudentId())) {
                Toast.makeText(context, "You cannot contact yourself", Toast.LENGTH_SHORT).show();
                return;
            }

            fetchStudentNameAndOpenChat(request.getStudentId(), request.getSubject(), request.getRequestId());
        });
    }

    private void fetchStudentNameAndOpenChat(String studentId, String listingTitle, String listingId) {
        // Note: Assuming student names are stored under "student_profiles" or "users"
        // Adjust the path "student_profiles" if your database structure is different
        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("student_profiles").child(studentId) // changed from "users" to be safer, adjust if needed
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String studentName = "Student";
                        if (snapshot.exists()) {
                            // Check for "username" or "name" depending on your DB
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
                        openChatFragment(studentId, "Student", listingTitle, listingId);
                    }
                });
    }

    private void openChatFragment(String targetUserId, String targetUserName, String listingTitle, String listingId) {
        ChatDetailFragment chatFragment = new ChatDetailFragment();
        Bundle args = new Bundle();
        args.putString("targetUserId", targetUserId);
        args.putString("targetUserName", targetUserName);
        args.putString("listingId", listingId);
        args.putString("listingTitle", "Student Request: " + listingTitle);
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
        TextView tvSubject, tvLevel, tvArea, tvBudget, tvMode, tvDesc, tvTimestamp; // Added tvTimestamp
        Button btnContact;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            // --- NEW: Bind the ID from XML ---
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            // ---------------------------------
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvBudget = itemView.findViewById(R.id.tvBudget);
            tvMode = itemView.findViewById(R.id.tvModes);
            tvDesc = itemView.findViewById(R.id.tvDescription);
            btnContact = itemView.findViewById(R.id.btnContactStudent);
        }
    }
}