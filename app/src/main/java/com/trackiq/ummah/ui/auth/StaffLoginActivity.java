package com.trackiq.ummah.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.UmmahConnectApp;
import com.trackiq.ummah.databinding.ActivityStaffLoginBinding; // Adjust if your binding name differs
import com.trackiq.ummah.ui.main.DashboardActivity;

public class StaffLoginActivity extends AppCompatActivity {

    private ActivityStaffLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStaffLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        // 1. Handle the unified Secure Login button
        binding.btnSecureLogin.setOnClickListener(v -> processUnifiedLogin());

        // 2. Handle the Registration Link
        binding.tvRegisterCommunity.setOnClickListener(v -> {
            startActivity(new Intent(StaffLoginActivity.this, RegisterCommunityActivity.class));
        });
        
        // (Optional) Forgot Password logic
        binding.tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password flow coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void processUnifiedLogin() {
        String primaryInput = binding.etWorkspaceOrEmail.getText().toString().trim();
        String userId = binding.etUserId.getText().toString().trim();
        String passwordOrPin = binding.etPasswordOrPin.getText().toString().trim();

        if (primaryInput.isEmpty() || passwordOrPin.isEmpty()) {
            Toast.makeText(this, "Workspace/Email and Password/PIN are required", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSecureLogin.setEnabled(false);

        // THE SWITCHBOARD LOGIC
        if (primaryInput.contains("@")) {
            // It's an Email -> Route to Admin Firebase Auth
            loginAsAdmin(primaryInput, passwordOrPin);
        } else {
            // It's a Workspace ID -> Route to Staff Database Check
            if (userId.isEmpty()) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSecureLogin.setEnabled(true);
                Toast.makeText(this, "User ID required for Staff login", Toast.LENGTH_SHORT).show();
                return;
            }
            loginAsStaff(primaryInput.toUpperCase(), userId, passwordOrPin);
        }
    }

    private void loginAsAdmin(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Success! Proceed to dashboard
                        Toast.makeText(StaffLoginActivity.this, "Welcome, Admin", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(StaffLoginActivity.this, DashboardActivity.class));
                        finish();
                    } else {
                        // Failed Auth
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSecureLogin.setEnabled(true);
                        Toast.makeText(StaffLoginActivity.this, "Admin Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loginAsStaff(String workspaceId, String userId, String pin) {
        // Query the specific staff member in the database: /staff/MASJID-01/userId/
        database.child("staff").child(workspaceId).child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSecureLogin.setEnabled(true);

                        if (snapshot.exists()) {
                            // Check if the PIN matches and account is active
                            String dbPin = snapshot.child("pin").getValue(String.class);
                            Boolean isActive = snapshot.child("active").getValue(Boolean.class);

                            if (Boolean.TRUE.equals(isActive) && pin.equals(dbPin)) {
                                // Cache the workspace ID so the dashboard knows what to load
                                UmmahConnectApp.getInstance().getPreferences().edit()
                                        .putString("current_workspace_id", workspaceId)
                                        .putString("cached_user_type", "staff")
                                        .apply();

                                Toast.makeText(StaffLoginActivity.this, "Staff Login Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(StaffLoginActivity.this, DashboardActivity.class));
                                finish();
                            } else {
                                Toast.makeText(StaffLoginActivity.this, "Invalid PIN or Inactive Account", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(StaffLoginActivity.this, "User ID not found in this Workspace", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSecureLogin.setEnabled(true);
                        Toast.makeText(StaffLoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
