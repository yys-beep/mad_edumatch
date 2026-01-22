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
import com.example.mad_edumatch.helper.ListingDataHelper;
import com.example.mad_edumatch.helper.TimeHelper;
import com.google.android.material.button.MaterialButton;

import java.util.List;

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

        holder.tvName.setText(tutor.getName());
        holder.tvSubjects.setText(tutor.getSubject());

        // Levels
        String displayLevels = ListingDataHelper.getLevelsAsString(context, tutor.getAcademicLevels());
        holder.tvLevels.setText(displayLevels);

        // Fee
        holder.tvFee.setText(context.getString(R.string.budget_per_hour, tutor.getFee()));

        holder.tvArea.setText(tutor.getArea());
        holder.tvQualification.setText(tutor.getQualification());

        // NEW: Display Contact
        holder.tvContact.setText(tutor.getContact());

        // Modes
        String deliveryDisplay = ListingDataHelper.getDeliveryModeDisplayName(context, tutor.getDeliveryMode());
        String learningDisplay = ListingDataHelper.getLearningModeDisplayName(context, tutor.getLearningMode());
        holder.tvMode.setText(context.getString(R.string.mode_format_brackets, deliveryDisplay, learningDisplay));

        // Timestamp
        if (tutor.getTimestamp() > 0) {
            String formattedTime = TimeHelper.getMalaysiaTime(tutor.getTimestamp());
            holder.tvTimestamp.setText(context.getString(R.string.posted_time_format, formattedTime));
        } else {
            holder.tvTimestamp.setText(R.string.posted_just_now);
        }

        // Contact Button
        holder.btnContact.setOnClickListener(v -> {
            String currentUid = CurrentUser.getInstance().getUid();

            if (currentUid == null) {
                Toast.makeText(context, R.string.login_to_contact, Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUid.equals(tutor.getTutorId())) {
                Toast.makeText(context, R.string.contact_self_error, Toast.LENGTH_SHORT).show();
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
        args.putString("listingTitle", context.getString(R.string.tutor_listing_title, subject));

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
        TextView tvName, tvTimestamp, tvSubjects, tvLevels, tvFee, tvArea, tvQualification, tvMode, tvContact;
        MaterialButton btnContact;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvSubjects = itemView.findViewById(R.id.tvSubjects);
            tvLevels = itemView.findViewById(R.id.tvLevels);
            tvFee = itemView.findViewById(R.id.tvFee);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvMode = itemView.findViewById(R.id.tvMode);
            tvQualification = itemView.findViewById(R.id.tvQualification);
            // Added tvContact
            tvContact = itemView.findViewById(R.id.tvContact);
            btnContact = itemView.findViewById(R.id.btnContactTutor);
        }
    }
}