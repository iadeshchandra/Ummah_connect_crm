package com.trackiq.ummah.ui.members;

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
import com.trackiq.ummah.UmmahConnectApp;
import com.trackiq.ummah.databinding.ActivityAddEditMemberBinding;
import com.trackiq.ummah.model.Member;
import com.trackiq.ummah.utils.AuditLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * AddEditMemberActivity - Create or edit member (MBR-XXXX)
 * 
 * Auto-generates MBR- prefix IDs
 * Validates required fields
 * Works offline via Firebase persistence
 */
public class AddEditMemberActivity extends AppCompatActivity {

    private ActivityAddEditMemberBinding binding;
    private DatabaseReference membersRef;
    private String existingMemberId = null;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditMemberBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        membersRef = FirebaseDatabase.getInstance().getReference("members");

        setupToolbar();
        setupStatusDropdown();
        
        // Check if editing existing member
        existingMemberId = getIntent().getStringExtra("member_id");
        if (existingMemberId != null) {
            isEditMode = true;
            loadExistingMember();
        } else {
            generateNewMemberId();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(isEditMode ? "Edit Member" : "Add Member");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupStatusDropdown() {
        String[] statuses = {"Active", "Inactive", "VIP"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, statuses);
        binding.spinnerStatus.setAdapter(adapter);
    }

    /**
     * Generate new MBR-XXXX ID
     */
    private void generateNewMemberId() {
        String shortId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String memberId = "MBR-" + shortId;
        binding.tvMemberId.setText(memberId);
    }

    /**
     * Load existing member data for editing
     */
    private void loadExistingMember() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        membersRef.child(existingMemberId).get().addOnCompleteListener(task -> {
            binding.progressBar.setVisibility(View.GONE);
            
            if (task.isSuccessful() && task.getResult() != null) {
                Member member = task.getResult().getValue(Member.class);
                if (member != null) {
                    binding.tvMemberId.setText(member.getDisplayId());
                    binding.etName.setText(member.getName());
                    binding.etPhone.setText(member.getPhone());
                    binding.etEmail.setText(member.getEmail());
                    binding.etAddress.setText(member.getAddress());
                    binding.etNotes.setText(member.getNotes());
                    
                    // Set status spinner
                    String status = member.getStatus();
                    if (status != null) {
                        String[] statuses = {"Active", "Inactive", "VIP"};
                        for (int i = 0; i < statuses.length; i++) {
                            if (statuses[i].equalsIgnoreCase(status)) {
                                binding.spinnerStatus.setSelection(i);
                                break;
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Error loading member", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_save, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveMember();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Validate and save member to Firebase
     */
    private void saveMember() {
        String name = binding.etName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String notes = binding.etNotes.getText().toString().trim();
        String status = binding.spinnerStatus.getSelectedItem().toString().toLowerCase();

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

        Member member = new Member();
        member.setName(name);
        member.setPhone(phone);
        member.setEmail(email.isEmpty() ? null : email);
        member.setAddress(address.isEmpty() ? null : address);
        member.setNotes(notes.isEmpty() ? null : notes);
        member.setStatus(status);
        member.setUpdatedAt(System.currentTimeMillis());

        String memberId = binding.tvMemberId.getText().toString().replace("MBR-", "");

        if (isEditMode) {
            // Update existing
            membersRef.child(existingMemberId).updateChildren(member.toMap())
                    .addOnCompleteListener(task -> {
                        binding.progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            AuditLogger.log(this, AuditLogger.ACTION_MEMBER_EDIT,
                                    "Updated member: " + memberId);
                            Toast.makeText(this, "Member updated", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Create new
            member.setCreatedAt(System.currentTimeMillis());
            member.setJoinDate(new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .format(new Date()));
            
            membersRef.child(memberId).setValue(member)
                    .addOnCompleteListener(task -> {
                        binding.progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            AuditLogger.log(this, AuditLogger.ACTION_MEMBER_ADD,
                                    "Created member: " + memberId);
                            Toast.makeText(this, "Member created", Toast.LENGTH_SHORT).show();
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
