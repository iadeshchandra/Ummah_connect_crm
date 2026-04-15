package com.trackiq.ummah.ui.guests;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.R;
import com.trackiq.ummah.databinding.ActivityGuestListBinding;
import com.trackiq.ummah.model.Guest;
import com.trackiq.ummah.utils.GuestAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * GuestListActivity - Display and manage guests/musallees (GST-XXXX)
 */
public class GuestListActivity extends AppCompatActivity {

    private ActivityGuestListBinding binding;
    private DatabaseReference guestsRef;
    private GuestAdapter adapter;
    private List<Guest> guestList;
    private List<Guest> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGuestListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        guestsRef = FirebaseDatabase.getInstance().getReference("guests");

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFab();

        loadGuests();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Guests & Musallees");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        guestList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new GuestAdapter(filteredList, guest -> {
            Intent intent = new Intent(this, AddEditGuestActivity.class);
            intent.putExtra("guest_id", guest.getId());
            intent.putExtra("view_mode", true);
            startActivity(intent);
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGuests(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditGuestActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Load guests from Firebase (works offline via persistence)
     */
    private void loadGuests() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        guestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                guestList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Guest guest = dataSnapshot.getValue(Guest.class);
                    if (guest != null) {
                        guest.setId(dataSnapshot.getKey());
                        guestList.add(guest);
                    }
                }

                // Sort by last visit date (most recent first)
                guestList.sort((g1, g2) -> {
                    if (g1.getLastVisitDate() == null) return 1;
                    if (g2.getLastVisitDate() == null) return -1;
                    return g2.getLastVisitDate().compareTo(g1.getLastVisitDate());
                });

                filteredList.clear();
                filteredList.addAll(guestList);
                adapter.notifyDataSetChanged();

                if (guestList.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(GuestListActivity.this,
                        "Error loading guests: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Filter guests by search query
     */
    private void filterGuests(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(guestList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Guest guest : guestList) {
                if (guest.getName().toLowerCase().contains(lowerQuery) ||
                    guest.getId().toLowerCase().contains(lowerQuery) ||
                    (guest.getPhone() != null && guest.getPhone().contains(lowerQuery)) ||
                    (guest.getType() != null && guest.getType().toLowerCase().contains(lowerQuery))) {
                    filteredList.add(guest);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredList.isEmpty() && !query.isEmpty()) {
            binding.tvEmpty.setText("No guests found");
            binding.tvEmpty.setVisibility(View.VISIBLE);
        } else if (guestList.isEmpty()) {
            binding.tvEmpty.setText("No guests yet");
            binding.tvEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (guestsRef != null) {
            loadGuests();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
