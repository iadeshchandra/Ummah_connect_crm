package com.trackiq.ummah.ui.members;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.trackiq.ummah.R;
import com.trackiq.ummah.UmmahConnectApp;
import com.trackiq.ummah.databinding.ActivityMemberDetailBinding;
import com.trackiq.ummah.model.Member;
import com.trackiq.ummah.utils.AuditLogger;

/**
 * MemberDetailActivity - View member details with edit/delete options
 */
public class MemberDetailActivity extends AppCompatActivity {

    private ActivityMemberDetailBinding binding;
    private DatabaseReference membersRef;
    private String memberId;
    private Member currentMember;
    private UmmahConnectApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMemberDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        membersRef = FirebaseDatabase.getInstance().getReference("members");
        app = UmmahConnectApp.getInstance();
        memberId = getIntent().getStringExtra("member_id");

        if (memberId == null) {
            Toast.makeText(this, "No member ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        loadMemberDetails();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Member Details");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadMemberDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);

        membersRef.child(memberId).get().addOnCompleteListener(task -> {
            binding.progressBar.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult() != null) {
                currentMember = task.getResult().getValue(Member.class);
                if (currentMember != null) {
                    displayMemberDetails();
                } else {
                    Toast.makeText(this, "Member not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "Error loading member", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayMemberDetails() {
        binding.tvMemberId.setText(currentMember.getDisplayId());
        binding.tvName.setText(currentMember.getName());
        binding.tvPhone.setText(currentMember.getPhone());
        binding.tvEmail.setText(currentMember.getEmail() != null ? 
                currentMember.getEmail() : "Not provided");
        binding.tvAddress.setText(currentMember.getAddress() != null ? 
                currentMember.getAddress() : "Not provided");
        binding.tvStatus.setText(currentMember.getStatus() != null ? 
                currentMember.getStatus().toUpperCase() : "ACTIVE");
        binding.tvJoinDate.setText(currentMember.getJoinDate() != null ? 
                currentMember.getJoinDate() : "Unknown");
        binding.tvNotes.setText(currentMember.getNotes() != null ? 
                currentMember.getNotes() : "No notes");

        // Status color
        int statusColor;
        String status = currentMember.getStatus();
        if ("active".equalsIgnoreCase(status)) {
            statusColor = R.color.status_active;
        } else if ("inactive".equalsIgnoreCase(status)) {
            statusColor = R.color.status_inactive;
        } else if ("vip".equalsIgnoreCase(status)) {
            statusColor = R.color.status_vip;
        } else {
            statusColor = R.color.status_active;
        }
        binding.tvStatus.setTextColor(getColor(statusColor));

        // Show edit/delete only for admin
        boolean isAdmin = "admin".equals(app.getPreferences()
                .getString("cached_user_type", ""));
        binding.btnEdit.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        binding.btnDelete.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

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
                .setMessage("Are you sure you want to delete " + currentMember.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMember())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMember() {
        binding.progressBar.setVisibility(View.VISIBLE);

        membersRef.child(memberId).removeValue()
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        AuditLogger.log(this, AuditLogger.ACTION_MEMBER_DELETE,
                                "Deleted member: " + memberId);
                        Toast.makeText(this, "Member deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Error deleting member", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show edit menu for admin
        if ("admin".equals(app.getPreferences().getString("cached_user_type", ""))) {
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
        // Refresh data
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
