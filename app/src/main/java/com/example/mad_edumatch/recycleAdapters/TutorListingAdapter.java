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
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.TutorViewListing;
// Use the new helper we created
import com.example.mad_edumatch.helper.ListingDataHelper;
import com.example.mad_edumatch.tutor.EditTutorListingDialog;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class TutorListingAdapter extends RecyclerView.Adapter<TutorListingAdapter.ViewHolder> {

    private Context context;
    private List<TutorViewListing> listingList;
    private FragmentManager fragmentManager;

    public TutorListingAdapter(Context context, List<TutorViewListing> listingList, FragmentManager fragmentManager) {
        this.context = context;
        this.listingList = listingList;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tutor_view_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TutorViewListing listing = listingList.get(position);

        holder.tvName.setText(listing.getName());
        holder.tvSubjects.setText(listing.getSubject());

        // --- 1. TRANSLATE LEVELS (Updated) ---
        // Instead of trying to translate a combined string, we pass the List<String> of KEYS
        // to the helper, which translates each one and joins them with commas.
        String displayLevels = ListingDataHelper.getLevelsAsString(context, listing.getAcademicLevels());
        holder.tvLevels.setText(displayLevels);

        holder.tvFee.setText(listing.getFeeString());
        holder.tvArea.setText(listing.getArea());

        // --- 2. TRANSLATE MODES (Updated) ---
        // We pass the KEY (e.g., "ONE_TO_ONE") and get back the localized text (e.g., "Satu-ke-satu")
        String displayDelivery = ListingDataHelper.getDeliveryModeDisplayName(context, listing.getDeliveryMode());
        String displayLearning = ListingDataHelper.getLearningModeDisplayName(context, listing.getLearningMode());

        // Format: "Physical / One-to-One"
        // Ensure you have <string name="mode_format">%1$s / %2$s</string> in strings.xml
        // If not, use: displayDelivery + " / " + displayLearning
        holder.tvMode.setText(context.getString(R.string.mode_format, displayDelivery, displayLearning));

        holder.tvQualification.setText(listing.getQualification());
        holder.tvContact.setText(listing.getContact());

        // EDIT BUTTON
        holder.btnEdit.setOnClickListener(v -> {
            if (fragmentManager != null) {
                EditTutorListingDialog dialog = new EditTutorListingDialog(listing);
                dialog.show(fragmentManager, "EditTutorListing");
            } else {
                Toast.makeText(context, R.string.edit_error_manager, Toast.LENGTH_SHORT).show();
            }
        });

        // DELETE BUTTON
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.delete_listing_title)
                    .setMessage(R.string.delete_listing_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        if (listing.getKey() != null) {
                            FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                                    .getReference("tutor_listings")
                                    .child(listing.getKey())
                                    .removeValue()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(context, R.string.listing_deleted, Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(context, R.string.deletion_failed, Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton(R.string.no, null)
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
            tvName = itemView.findViewById(R.id.tvName);
            tvSubjects = itemView.findViewById(R.id.tvSubjects);
            tvLevels = itemView.findViewById(R.id.tvLevels);
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