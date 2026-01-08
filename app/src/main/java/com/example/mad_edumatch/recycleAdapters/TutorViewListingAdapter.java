package com.example.mad_edumatch.recycleAdapters;

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
import com.example.mad_edumatch.helper.LocalizationHelper;
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

        // --- 1. Translate Level ---
        int levelResId = LocalizationHelper.getLevelStringId(item.getLevelsAsString());
        if (levelResId != 0) {
            holder.tvLevel.setText(context.getString(levelResId));
        } else {
            holder.tvLevel.setText(item.getLevelsAsString());
        }

        // --- 2. Translate Modes ---
        String deliveryRaw = item.getDeliveryMode();
        String learningRaw = item.getLearningMode();
        String deliveryDisplay = deliveryRaw;
        String learningDisplay = learningRaw;

        // Translate Delivery Mode
        int deliveryId = LocalizationHelper.getDeliveryModeStringId(deliveryRaw);
        if (deliveryId != 0) deliveryDisplay = context.getString(deliveryId);

        // Translate Learning Mode
        int learningId = LocalizationHelper.getLearningModeStringId(learningRaw);
        if (learningId != 0) learningDisplay = context.getString(learningId);

        // Combine Translated Strings (Using Resource Format "%1$s / %2$s")
        holder.tvMode.setText(context.getString(R.string.mode_format, deliveryDisplay, learningDisplay));

        // --- Normal Data Binding ---
        holder.tvName.setText(item.getName());
        holder.tvSubjects.setText(item.getSubject());
        holder.tvLevels.setText(item.getLevelsAsString());
        holder.tvFee.setText(item.getFeeString());
        holder.tvArea.setText(item.getArea());
        holder.tvQualification.setText(item.getQualification());
        holder.tvContact.setText(item.getContact());

        // --- Timestamp ---
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                item.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);
        // Using Resource Format "Last updated: %s"
        holder.tvTimestamp.setText(context.getString(R.string.last_updated_format, timeAgo));

        // --- Button Logic ---
        holder.btnEdit.setVisibility(View.VISIBLE);
        holder.btnDelete.setVisibility(View.VISIBLE);

        holder.btnEdit.setOnClickListener(v -> {
            EditTutorListingDialog dialog = new EditTutorListingDialog(item);
            dialog.show(fragmentManager, "EditListing"); // Tags are internal IDs, safe to keep hardcoded
        });

        holder.btnDelete.setOnClickListener(v -> deleteListing(item.getKey()));
    }

    private void deleteListing(String key) {
        // Ideally, move URL to a Constants file, but keeping as requested
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
        TextView tvLevel;
        Button btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvLevel = itemView.findViewById(R.id.tvLevels); // Warning: Ensure IDs match your XML
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