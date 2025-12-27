package com.example.mad_edumatch.recycleAdapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.chat.ChatDetailFragment;
import com.example.mad_edumatch.firebaseModels.TutorListing;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.helper.TimeHelper; // IMPORT THIS
import com.google.android.material.button.MaterialButton;

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
        View view = LayoutInflater.from(context).inflate(R.layout.item_student_view_tutor_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TutorListing tutor = tutorList.get(position);

        // 1. Bind Data
        holder.tvName.setText(tutor.getName());
        holder.tvSubjects.setText(tutor.getSubject());

        // Helper method from your model for List<String>
        holder.tvLevels.setText(tutor.getLevelsAsString());

        // Format Fee
        holder.tvFee.setText(String.format(Locale.getDefault(), "RM %.2f/hr", tutor.getFee()));

        holder.tvArea.setText(tutor.getArea());
        holder.tvQualification.setText(tutor.getQualification());

        // 2. Format Timestamp using TimeHelper (FIXED HERE)
        if (tutor.getTimestamp() > 0) {
            String formattedTime = TimeHelper.getMalaysiaTime(tutor.getTimestamp());
            holder.tvTimestamp.setText("Posted: " + formattedTime);
        } else {
            holder.tvTimestamp.setText("Posted: Just now");
        }

        // 3. Contact Button Logic
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

            openChatFragment(tutor.getTutorId(), tutor.getName(), tutor.getSubject(), tutor.getKey());
        });
    }

    private void openChatFragment(String targetUserId, String targetUserName, String subject, String listingId) {
        ChatDetailFragment chatFragment = new ChatDetailFragment();

        Bundle args = new Bundle();
        args.putString("targetUserId", targetUserId);
        args.putString("targetUserName", targetUserName);
        args.putString("listingId", listingId);
        args.putString("listingTitle", "Tutor Listing: " + subject);

        chatFragment.setArguments(args);

        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction()
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
        MaterialButton btnContact;

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