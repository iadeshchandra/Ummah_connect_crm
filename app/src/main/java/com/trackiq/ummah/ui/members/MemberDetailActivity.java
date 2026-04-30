package com.trackiq.ummah.ui.members;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.R;
import com.trackiq.ummah.databinding.ActivityMemberDetailBinding;
import com.trackiq.ummah.model.Member;
import com.trackiq.ummah.utils.AuditLogger;

/**
 * MemberDetailActivity - View member details with edit/delete options
 * Strictly enforces Workspace Silo for Multi-Tenant SaaS
 * Strictly enforces RBAC (Super Admin/Manager can edit/delete, Members view-only)
 */
public class MemberDetailActivity extends AppCompatActivity {

    private ActivityMemberDetailBinding binding;
    private DatabaseReference membersRef;
    private String memberId;
    private Member currentMember;
    private String currentWorkspaceId;
    private boolean canEditDelete = false; // Controls RBAC visibility

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMemberDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Fetch Workspace ID to secure the data silo
        SharedPreferences prefs = getSharedPreferences("UmmahPrefs", MODE_PRIVATE);
        currentWorkspaceId = prefs.getString("current_workspace_id", null);

        if (currentWorkspaceId == null) {
            Toast.makeText(this, "Session error. Workspace not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Initialize Firebase reference locked to THIS Workspace ONLY
        membersRef = FirebaseDatabase.getInstance().getReference("members").child(currentWorkspaceId);
        
        memberId = getIntent().getStringExtra("member_id");
        if (memberId == null) {
            Toast.makeText(this, "No member ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        
        // 3. Check User Role for permissions, then load data
        determineUserRoleAndSetupUI();
        loadMemberDetails();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Member Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * RBAC Enforcement: Determine if user is Admin/Manager to show Edit/Delete options
     */
    private void determineUserRoleAndSetupUI() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // Check Super Admin or Manager in Database
            FirebaseDatabase.getInstance().getReference("users")
                    .child(auth.getCurrentUser().getUid())
                    .child("role")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String role = snapshot.getValue(String.class);
                            canEditDelete = "SUPER_ADMIN".equals(role) || "MANAGER".equals(role);
                            updateActionButtons();
                            invalidateOptionsMenu(); // Refresh top menu
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        } else {
            // Staff members logged in via PIN act as Managers
            SharedPreferences prefs = getSharedPreferences("UmmahPrefs", MODE_PRIVATE);
            canEditDelete = "staff".equals(prefs.getString("cached_user_type", ""));
            updateActionButtons();
            invalidateOptionsMenu();
        }
    }

    private void updateActionButtons() {
        binding.btnEdit.setVisibility(canEditDelete ? View.VISIBLE : View.GONE);
        binding.btnDelete.setVisibility(canEditDelete ? View.VISIBLE : View.GONE);
    }

    private void loadMemberDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);

        membersRef.child(memberId).get().addOnCompleteListener(task -> {
            binding.progressBar.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                currentMember = task.getResult().getValue(Member.class);
                if (currentMember != null) {
                    displayMemberDetails();
                } else {
                    Toast.makeText(this, "Data corrupted or missing", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "Member not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayMemberDetails() {
        binding.tvMemberId.setText(currentMember.getDisplayId() != null ? currentMember.getDisplayId() : memberId);
        binding.tvName.setText(currentMember.getName());
        binding.tvPhone.setText(currentMember.getPhone());
        binding.tvEmail.setText(currentMember.getEmail() != null ? currentMember.getEmail() : "Not provided");
        binding.tvAddress.setText(currentMember.getAddress() != null ? currentMember.getAddress() : "Not provided");
        binding.tvStatus.setText(currentMember.getStatus() != null ? currentMember.getStatus().toUpperCase() : "ACTIVE");
        binding.tvJoinDate.setText(currentMember.getJoinDate() != null ? currentMember.getJoinDate() : "Unknown");
        binding.tvNotes.setText(currentMember.getNotes() != null ? currentMember.getNotes() : "No notes");

        // Status color mapping
        int statusColor;
        String status = currentMember.getStatus() != null ? currentMember.getStatus().toLowerCase() : "";
        if ("active".equals(status)) {
            statusColor = R.color.status_active;
        } else if ("inactive".equals(status)) {
            statusColor = R.color.status_inactive;
        } else if ("vip".equals(status)) {
            statusColor = R.color.status_vip;
        } else {
            statusColor = R.color.status_active; // Default fallback
        }
        binding.tvStatus.setTextColor(getColor(statusColor));

        // Click listeners for Admin/Manager actions
        binding.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditMemberActivity.class);
            intent.putExtra("member_id", memberId);
            startActivity(intent);
        });

        binding.btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Member")
                .setMessage("Are you sure you want to delete " + currentMember.getName() + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteMember())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMember() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Delete from the secure Workspace Silo
        membersRef.child(memberId).removeValue()
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        AuditLogger.log(this, AuditLogger.ACTION_MEMBER_DELETE, "Deleted member: " + memberId);
                        Toast.makeText(this, "Member deleted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Error deleting member", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show edit/delete toolbar menu if the user has RBAC permission
        if (canEditDelete) {
            getMenuInflater().inflate(R.menu.menu_edit_delete, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            Intent intent = new Intent(this, AddEditMemberActivity.class);
            intent.putExtra("member_id", memberId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from Edit screen
        if (memberId != null) {
            loadMemberDetails();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
