package com.example.mad_edumatch.recycleAdapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
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

public class TutorListingAdapter extends RecyclerView.Adapter<TutorListingAdapter.ViewHolder> {

    private Context context;
    private List<TutorViewListing> listingList;
    private FragmentManager fragmentManager; // Needed for Dialogs

    // Constructor updated to receive FragmentManager
    public TutorListingAdapter(Context context, List<TutorViewListing> listingList, FragmentManager fragmentManager) {
        this.context = context;
        this.listingList = listingList;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use the XML that has Edit/Delete buttons (assuming it's named similar to student's manage)
        View view = LayoutInflater.from(context).inflate(R.layout.item_tutor_view_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TutorViewListing listing = listingList.get(position);

        // Bind all fields
        holder.tvName.setText(listing.getName());
        holder.tvSubjects.setText(listing.getSubject());

        // Use the helper to display Levels
        holder.tvLevels.setText(listing.getLevelsAsString());

        holder.tvFee.setText(listing.getFeeString());
        holder.tvArea.setText(listing.getArea());
        holder.tvMode.setText(listing.getDeliveryMode() + " / " + listing.getLearningMode());
        holder.tvQualification.setText(listing.getQualification());
        holder.tvContact.setText(listing.getContact());
        // holder.tvTimestamp.setText("Last updated: " + formatTimestamp(listing.getTimestamp())); // Implement formatTimestamp if needed

        // EDIT BUTTON LOGIC
        holder.btnEdit.setOnClickListener(v -> {
            if (fragmentManager != null) {
                // Pass the entire listing object to the dialog
                EditTutorListingDialog dialog = new EditTutorListingDialog(listing);
                dialog.show(fragmentManager, "EditTutorListing");
            } else {
                Toast.makeText(context, "Edit function error: Fragment Manager not available.", Toast.LENGTH_SHORT).show();
            }
        });

        // DELETE BUTTON LOGIC
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Listing")
                    .setMessage("Are you sure you want to permanently delete this listing?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (listing.getKey() != null) {
                            FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                                    .getReference("tutor_listings")
                                    .child(listing.getKey()) // Use the unique key
                                    .removeValue()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Listing Deleted", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(context, "Deletion Failed", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return listingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSubjects, tvLevels, tvFee, tvArea, tvMode, tvQualification, tvContact, tvTimestamp;
        Button btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Assuming your XML element IDs are:
            tvName = itemView.findViewById(R.id.tvName);
            tvSubjects = itemView.findViewById(R.id.tvSubjects);
            tvLevels = itemView.findViewById(R.id.tvLevels); // NEW ID
            tvFee = itemView.findViewById(R.id.tvFee);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvMode = itemView.findViewById(R.id.tvMode);
            tvQualification = itemView.findViewById(R.id.tvQualification);
            tvContact = itemView.findViewById(R.id.tvContact);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);

            btnEdit = itemView.findViewById(R.id.btnEditListing);
            btnDelete = itemView.findViewById(R.id.btnDeleteListing);
        }
    }
}