package com.example.mad_edumatch.recycleAdapters;

import android.app.AlertDialog; // Import AlertDialog
import android.content.Context;
import android.text.format.DateUtils;
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
import com.example.mad_edumatch.helper.ListingDataHelper;
import com.example.mad_edumatch.tutor.EditTutorListingDialog;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class TutorViewListingAdapter extends RecyclerView.Adapter<TutorViewListingAdapter.ViewHolder> {

    private Context context;
    private List<TutorViewListing> list;
    private FragmentManager fragmentManager;

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

        // --- 1. Translate Levels ---
        String displayLevels = ListingDataHelper.getLevelsAsString(context, item.getAcademicLevels());
        holder.tvLevels.setText(displayLevels);

        // --- 2. Translate Modes ---
        String deliveryDisplay = ListingDataHelper.getDeliveryModeDisplayName(context, item.getDeliveryMode());
        String learningDisplay = ListingDataHelper.getLearningModeDisplayName(context, item.getLearningMode());

        holder.tvMode.setText(context.getString(R.string.mode_format_brackets, deliveryDisplay, learningDisplay));

        // --- Normal Data Binding ---
        holder.tvName.setText(item.getName());
        holder.tvSubjects.setText(item.getSubject());
        holder.tvFee.setText(item.getFeeString());
        holder.tvArea.setText(item.getArea());
        holder.tvQualification.setText(item.getQualification());
        holder.tvContact.setText(item.getContact());

        // --- Timestamp ---
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                item.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);

        holder.tvTimestamp.setText(context.getString(R.string.last_updated_format, timeAgo));

        // --- Button Logic ---
        holder.btnEdit.setOnClickListener(v -> {
            EditTutorListingDialog dialog = new EditTutorListingDialog(item);
            dialog.show(fragmentManager, "EditListing");
        });

        // --- DELETE CONFIRMATION DIALOG ---
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.delete_listing_title)      // "Delete Listing"
                    .setMessage(R.string.delete_listing_message)  // "Are you sure...?"
                    .setPositiveButton(R.string.yes, (dialog, which) -> deleteListing(item.getKey()))
                    .setNegativeButton(R.string.no, null)
                    .show();
        });
    }

    private void deleteListing(String key) {
        FirebaseDatabase.getInstance("https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tutor_listings")
                .child(key)
                .removeValue()
                .addOnSuccessListener(unused ->
                        Toast.makeText(context, R.string.listing_deleted_success, Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(context, context.getString(R.string.delete_failed_format, e.getMessage()), Toast.LENGTH_SHORT).show()
                );
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
            tvMode = itemView.findViewById(R.id.tvMode);
            tvQualification = itemView.findViewById(R.id.tvQualification);
            tvContact = itemView.findViewById(R.id.tvContact);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnEdit = itemView.findViewById(R.id.btnEditListing);
            btnDelete = itemView.findViewById(R.id.btnDeleteListing);
        }
    }
}