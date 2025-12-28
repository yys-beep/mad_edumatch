package com.example.mad_edumatch.tutor;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_edumatch.R;
import com.example.mad_edumatch.firebaseModels.TutorViewListing;
import com.example.mad_edumatch.helper.CurrentUser;
import com.example.mad_edumatch.recycleAdapters.TutorViewListingAdapter; // Make sure this matches your adapter name
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class TutorViewListingFragment extends Fragment {

    private RecyclerView recyclerView;
    private TutorViewListingAdapter adapter; // Updated class name
    private ArrayList<TutorViewListing> listingList;
    private ArrayList<TutorViewListing> filteredList;
    private DatabaseReference listingRef;
    private String currentUserId;
    private EditText etSearchSubject;
    private ValueEventListener dbListener; // Store listener to remove it later

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tutor_listing_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvTutorListings);
        etSearchSubject = view.findViewById(R.id.etSearchSubject);
        TextView tvTitle = view.findViewById(R.id.tvTutorListingTitle);
        tvTitle.setText("My Tutor Listings");

        listingList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Pass FragmentManager for the Edit Dialog
        adapter = new TutorViewListingAdapter(getContext(), filteredList, getChildFragmentManager());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (!CurrentUser.getInstance().isLoggedIn()) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUserId = CurrentUser.getInstance().getUid();
        listingRef = FirebaseDatabase.getInstance(
                "https://edumatch-74070-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference("tutor_listings");

        setupSearchFilter();

        // Start Live Loading
        loadTutorListings();
    }

    private void loadTutorListings() {
        // USE addValueEventListener INSTEAD OF addListenerForSingleValueEvent
        dbListener = listingRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return; // Check if fragment is attached

                listingList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    TutorViewListing model = ds.getValue(TutorViewListing.class);
                    if (model == null) continue;

                    // Filter: Only show my own listings
                    if (currentUserId.equals(model.getTutorId())) {
                        model.setKey(ds.getKey());
                        listingList.add(model);
                    }
                }

                // Sort: Newest First
                Collections.sort(listingList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                // Refresh UI
                filterList(etSearchSubject.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) Toast.makeText(getContext(), "Failed loading listings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchFilter() {
        etSearchSubject.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterList(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(listingList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (TutorViewListing item : listingList) {
                // Option A: If Subject is just free text, keep it simple:
                if (item.getSubject() != null && item.getSubject().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(item);
                }

                // Option B: If you translate subjects, convert DB value first:
            /* String rawSubject = item.getSubject();
            String localizedSubject = rawSubject;
            // int resId = LocalizationHelper.getSubjectStringId(rawSubject); // You need to create this method
            // if (resId != 0) localizedSubject = getString(resId);

            if (localizedSubject.toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(item);
            }
            */
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listener to prevent crashes or memory leaks when leaving the tab
        if (listingRef != null && dbListener != null) {
            listingRef.removeEventListener(dbListener);
        }
    }
}