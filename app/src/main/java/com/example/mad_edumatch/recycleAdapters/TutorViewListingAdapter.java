package com.example.mad_edumatch.recycleAdapters;

import android.content.Context;
import android.text.format.DateUtils; // Import for timestamp
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.TutorViewListing;
import com.example.mad_edumatch.tutor.EditTutorListingDialog;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class TutorViewListingAdapter extends RecyclerView.Adapter<TutorViewListingAdapter.ViewHolder> {

    private Context context;
    private List<TutorViewListing> list;
    private FragmentManager fragmentManager; // Needed for the Dialog

    // Updated Constructor to accept FragmentManager
    public TutorViewListingAdapter(Context context, List<TutorViewListing> list, FragmentManager fragmentManager) {
        this.context = context;
        this.list = list;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_tutor_view_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        TutorViewListing item = list.get(position);

        // --- Data Binding ---
        holder.tvName.setText(item.getName());
        holder.tvSubjects.setText(item.getSubject());
        holder.tvLevels.setText(item.getLevelsAsString());
        holder.tvFee.setText(item.getFeeString());
        holder.tvArea.setText(item.getArea());
        String modeCombined = item.getDeliveryMode() + " / " + item.getLearningMode();
        holder.tvMode.setText(modeCombined);
        holder.tvQualification.setText(item.getQualification());
        holder.tvContact.setText(item.getContact());

        // --- Fix Timestamp ---
        // Convert the long timestamp to "5 minutes ago", "Yesterday", etc.
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                item.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);
        holder.tvTimestamp.setText("Last updated: " + timeAgo);

        // --- Handle Buttons (Edit & Delete) ---
        // Since this is the "My Listings" page, we MUST show these buttons
        holder.btnEdit.setVisibility(View.VISIBLE);
        holder.btnDelete.setVisibility(View.VISIBLE);

        // 1. EDIT Logic
        holder.btnEdit.setOnClickListener(v -> {
            EditTutorListingDialog dialog = new EditTutorListingDialog(item);
            dialog.show(fragmentManager, "EditListing");
        });

        // 2. DELETE Logic
        holder.btnDelete.setOnClickListener(v -> {
            // Note: In a real app, you should show an "Are you sure?" alert dialog here first.
            deleteListing(item.getKey());
        });
    }

    private void deleteListing(String key) {
        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_listings")
                .child(key)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(context, "Listing Deleted Successfully", Toast.LENGTH_SHORT).show();
                    // NOTE: We do NOT need to manually remove from 'list' here
                    // because the Fragment's addValueEventListener will detect the deletion
                    // and refresh the list automatically.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Delete Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvSubjects, tvLevels, tvFee, tvArea, tvMode, tvQualification, tvContact, tvTimestamp;
        Button btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvSubjects = itemView.findViewById(R.id.tvSubjects);
            tvLevels = itemView.findViewById(R.id.tvLevels);
            tvFee = itemView.findViewById(R.id.tvFee);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvQualification = itemView.findViewById(R.id.tvQualification);
            tvContact = itemView.findViewById(R.id.tvContact);
            tvMode = itemView.findViewById(R.id.tvMode);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnEdit = itemView.findViewById(R.id.btnEditListing);
            btnDelete = itemView.findViewById(R.id.btnDeleteListing);
        }
    }
}