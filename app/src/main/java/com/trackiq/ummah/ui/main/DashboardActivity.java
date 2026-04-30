package com.trackiq.ummah.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.databinding.ActivityDashboardBinding;
import com.trackiq.ummah.ui.auth.StaffLoginActivity;

public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference database;
    private String currentWorkspaceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        // 1. Get the Workspace ID that was saved during Login/Registration
        SharedPreferences prefs = getSharedPreferences("UmmahPrefs", MODE_PRIVATE);
        currentWorkspaceId = prefs.getString("current_workspace_id", null);

        if (currentWorkspaceId == null) {
            // Safety catch: If no workspace is cached, force them back to login
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            logoutUser();
            return;
        }

        binding.tvWorkspaceName.setText("Workspace: " + currentWorkspaceId);

        // 2. Determine User Type & Setup UI (Role-Based Access Control)
        determineUserRoleAndSetupUI();

        // Logout Button listener
        binding.btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void determineUserRoleAndSetupUI() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            // It's an Authenticated User (Super Admin, Manager, or App Member)
            database.child("users").child(currentUser.getUid()).child("role")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String role = snapshot.getValue(String.class);
                            if (role == null) role = "MEMBER"; // Default fallback
                            configureUIForRole(role);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(DashboardActivity.this, "Failed to load role", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // It's a Staff member who logged in with a Workspace ID and PIN
            // For now, we will treat them as a "MANAGER" level for daily operations
            configureUIForRole("MANAGER");
        }
    }

    private void configureUIForRole(String role) {
        // First, hide everything sensitive by default (Zero Trust Principle)
        binding.btnAddExpense.setVisibility(View.GONE); // The Floating Action Button
        binding.btnSendBroadcast.setVisibility(View.GONE); // The Broadcast Card
        binding.btnManageStaff.setVisibility(View.GONE); // The Staff Management Card
        binding.btnWorkspaceSettings.setVisibility(View.GONE); // The Settings Card

        // Then, reveal buttons based on their exact rank
        switch (role) {
            case "SUPER_ADMIN":
                // Highest level: Sees everything
                binding.btnManageStaff.setVisibility(View.VISIBLE);
                binding.btnWorkspaceSettings.setVisibility(View.VISIBLE);
                binding.btnAddExpense.setVisibility(View.VISIBLE);
                binding.btnSendBroadcast.setVisibility(View.VISIBLE);
                binding.tvUserRole.setText("Role: Super Admin");
                break;

            case "MANAGER":
                // Middle level: Can do daily tasks but cannot change app settings
                binding.btnAddExpense.setVisibility(View.VISIBLE);
                binding.btnSendBroadcast.setVisibility(View.VISIBLE);
                binding.tvUserRole.setText("Role: Manager / Staff");
                break;

            case "MEMBER":
            default:
                // Lowest level: Read-only (Buttons remain GONE)
                binding.tvUserRole.setText("Role: Community Member");
                break;
        }
    }

    private void logoutUser() {
        // 1. Sign out of Firebase
        if (firebaseAuth.getCurrentUser() != null) {
            firebaseAuth.signOut();
        }
        
        // 2. Clear the cached workspace ID so the next user doesn't load the wrong data
        getSharedPreferences("UmmahPrefs", MODE_PRIVATE).edit().clear().apply();
        
        // 3. Return to Universal Login screen
        Intent intent = new Intent(this, StaffLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
