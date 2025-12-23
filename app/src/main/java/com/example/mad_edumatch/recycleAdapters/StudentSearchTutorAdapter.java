package com.example.mad_edumatch.recycleAdapters;

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
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
import com.example.mad_edumatch.firebaseModels.TutorListing;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.student.StudentViewTutorProfileFragment; // Ensure this matches your package

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StudentSearchTutorAdapter extends RecyclerView.Adapter<StudentSearchTutorAdapter.ViewHolder> {

    private List<TutorListing> tutorList;
    private Context context;

    public StudentSearchTutorAdapter(List<TutorListing> tutorList) {
        this.tutorList = tutorList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        // Load the card layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_student_view_tutor_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TutorListing tutor = tutorList.get(position);

        // 1. Bind Basic Data
        holder.tvName.setText(tutor.getName());
        holder.tvSubjects.setText(tutor.getSubject());
        holder.tvLevels.setText(tutor.getLevelsAsString());
        holder.tvFee.setText(String.format(Locale.getDefault(), "RM %.2f/hr", tutor.getFee()));
        holder.tvArea.setText(tutor.getArea());
        holder.tvQualification.setText(tutor.getQualification());

        // 2. CLICK LISTENER - NAVIGATE TO PROFILE (The Fix)
        holder.itemView.setOnClickListener(null);
        holder.itemView.setClickable(false);
        holder.itemView.setFocusable(false);

        // 3. Timestamp Logic
        if (tutor.getTimestamp() > 0) {
            Calendar cal = Calendar.getInstance(Locale.getDefault());
            cal.setTimeInMillis(tutor.getTimestamp());
            String date = DateFormat.format("dd MMM, hh:mm a", cal).toString();
            holder.tvTimestamp.setText("Posted: " + date);
        } else {
            holder.tvTimestamp.setText("Posted: Just now");
        }

        // 4. Contact Button Logic
        holder.btnContact.setOnClickListener(v -> {
            String currentUid = CurrentUser.getInstance().getUid();

            if (currentUid == null) {
                Toast.makeText(context, "Please login to contact tutors", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentUid.equals(tutor.getTutorId())) {
                Toast.makeText(context, "You cannot chat with yourself!", Toast.LENGTH_SHORT).show();
                return;
            }
            openChatFragment(tutor.getTutorId(), tutor.getName());
        });
    }

    private void openChatFragment(String targetUserId, String targetUserName) {
        ChatDetailFragment chatFragment = new ChatDetailFragment();
        Bundle args = new Bundle();
        args.putString("targetUserId", targetUserId);
        args.putString("targetUserName", targetUserName);
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
        return tutorList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTimestamp, tvSubjects, tvLevels, tvFee, tvArea, tvQualification;
        Button btnContact; // Changed to Button to match standard Android button if Material isn't working

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvSubjects = itemView.findViewById(R.id.tvSubjects);
            tvLevels = itemView.findViewById(R.id.tvLevels);
            tvFee = itemView.findViewById(R.id.tvFee);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvQualification = itemView.findViewById(R.id.tvQualification);
            btnContact = itemView.findViewById(R.id.btnContactTutor);
        }
    }
}