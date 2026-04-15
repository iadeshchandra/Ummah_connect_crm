package com.trackiq.ummah.ui.guests;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.trackiq.ummah.R;
import com.trackiq.ummah.databinding.ActivityAddEditGuestBinding;
import com.trackiq.ummah.model.Guest;
import com.trackiq.ummah.utils.AuditLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * AddEditGuestActivity - Create or edit guest/musallee (GST-XXXX)
 * 
 * Auto-generates GST- prefix IDs
 * Tracks visit history
 */
public class AddEditGuestActivity extends AppCompatActivity {

    private ActivityAddEditGuestBinding binding;
    private DatabaseReference guestsRef;
    private String existingGuestId = null;
    private boolean isEditMode = false;
    private boolean isViewMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditGuestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        guestsRef = FirebaseDatabase.getInstance().getReference("guests");

        setupToolbar();
        setupTypeDropdown();

        // Check mode
        existingGuestId = getIntent().getStringExtra("guest_id");
        isViewMode = getIntent().getBooleanExtra("view_mode", false);

        if (existingGuestId != null) {
            isEditMode = true;
            loadExistingGuest();
        } else {
            generateNewGuestId();
        }
    }

    private void setupToolbar() {
        String title;
        if (isViewMode) {
            title = "Guest Details";
        } else if (isEditMode) {
            title = "Edit Guest";
        } else {
            title = "Add Guest";
        }
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTypeDropdown() {
        String[] types = {"Regular Musallee", "Visitor", "New Muslim"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, types);
        binding.spinnerType.setAdapter(adapter);
    }

    private void generateNewGuestId() {
        String shortId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String guestId = "GST-" + shortId;
        binding.tvGuestId.setText(guestId);
    }

    private void loadExistingGuest() {
        binding.progressBar.setVisibility(View.VISIBLE);

        guestsRef.child(existingGuestId).get().addOnCompleteListener(task -> {
            binding.progressBar.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult() != null) {
                Guest guest = task.getResult().getValue(Guest.class);
                if (guest != null) {
                    displayGuest(guest);
                } else {
                    Toast.makeText(this, "Guest not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "Error loading guest", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayGuest(Guest guest) {
        binding.tvGuestId.setText(guest.getDisplayId());
        binding.etName.setText(guest.getName());
        binding.etPhone.setText(guest.getPhone());
        binding.etEmail.setText(guest.getEmail());
        binding.etNotes.setText(guest.getNotes());

        // Set type spinner
        String type = guest.getType();
        String[] types = {"regular", "visitor", "new_muslim"};
        String[] displayTypes = {"Regular Musallee", "Visitor", "New Muslim"};
        for (int i = 0; i < types.length; i++) {
            if (types[i].equalsIgnoreCase(type)) {
                binding.spinnerType.setSelection(i);
                break;
            }
        }

        // Show visit info
        binding.tvVisitInfo.setVisibility(View.VISIBLE);
        binding.tvVisitInfo.setText("Visits: " + guest.getVisitCount() +
                "\nFirst Visit: " + (guest.getFirstVisitDate() != null ? guest.getFirstVisitDate() : "N/A") +
                "\nLast Visit: " + (guest.getLastVisitDate() != null ? guest.getLastVisitDate() : "N/A"));

        if (isViewMode) {
            // Disable editing
            binding.etName.setEnabled(false);
            binding.etPhone.setEnabled(false);
            binding.etEmail.setEnabled(false);
            binding.etNotes.setEnabled(false);
            binding.spinnerType.setEnabled(false);
            binding.btnRecordVisit.setVisibility(View.VISIBLE);
            binding.btnRecordVisit.setOnClickListener(v -> recordVisit());
        }
    }

    private void recordVisit() {
        binding.progressBar.setVisibility(View.VISIBLE);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        guestsRef.child(existingGuestId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Guest guest = task.getResult().getValue(Guest.class);
                if (guest != null) {
                    int newCount = guest.getVisitCount() + 1;
                    guestsRef.child(existingGuestId).child("visitCount").setValue(newCount);
                    guestsRef.child(existingGuestId).child("lastVisitDate").setValue(today);

                    Toast.makeText(this, "Visit recorded!", Toast.LENGTH_SHORT).show();
                    loadExistingGuest(); // Refresh
                }
            }
            binding.progressBar.setVisibility(View.GONE);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isViewMode) {
            getMenuInflater().inflate(R.menu.menu_save, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_edit, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            saveGuest();
            return true;
        } else if (id == R.id.action_edit) {
            // Switch to edit mode
            isViewMode = false;
            isEditMode = true;
            setupToolbar();
            enableEditing();
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableEditing() {
        binding.etName.setEnabled(true);
        binding.etPhone.setEnabled(true);
        binding.etEmail.setEnabled(true);
        binding.etNotes.setEnabled(true);
        binding.spinnerType.setEnabled(true);
        binding.btnRecordVisit.setVisibility(View.GONE);
    }

    private void saveGuest() {
        String name = binding.etName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String notes = binding.etNotes.getText().toString().trim();

        String typeDisplay = binding.spinnerType.getSelectedItem().toString();
        String type;
        switch (typeDisplay) {
            case "Visitor":
                type = "visitor";
                break;
            case "New Muslim":
                type = "new_muslim";
                break;
            case "Regular Musallee":
            default:
                type = "regular";
        }

        // Validation
        if (name.isEmpty()) {
            binding.tilName.setError("Name is required");
            return;
        } else {
            binding.tilName.setError(null);
        }

        if (phone.isEmpty()) {
            binding.tilPhone.setError("Phone is required");
            return;
        } else {
            binding.tilPhone.setError(null);
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        Guest guest = new Guest();
        guest.setName(name);
        guest.setPhone(phone);
        guest.setEmail(email.isEmpty() ? null : email);
        guest.setNotes(notes.isEmpty() ? null : notes);
        guest.setType(type);
        guest.setUpdatedAt(System.currentTimeMillis());

        String guestId = binding.tvGuestId.getText().toString().replace("GST-", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        if (isEditMode) {
            // Update existing
            guestsRef.child(existingGuestId).updateChildren(guest.toMap())
                    .addOnCompleteListener(task -> {
                        binding.progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            AuditLogger.log(this, AuditLogger.ACTION_GUEST_EDIT,
                                    "Updated guest: " + guestId);
                            Toast.makeText(this, "Guest updated", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Create new
            guest.setCreatedAt(System.currentTimeMillis());
            guest.setFirstVisitDate(today);
            guest.setLastVisitDate(today);
            guest.setVisitCount(1);

            guestsRef.child(guestId).setValue(guest)
                    .addOnCompleteListener(task -> {
                        binding.progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            AuditLogger.log(this, AuditLogger.ACTION_GUEST_ADD,
                                    "Created guest: " + guestId);
                            Toast.makeText(this, "Guest created", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
