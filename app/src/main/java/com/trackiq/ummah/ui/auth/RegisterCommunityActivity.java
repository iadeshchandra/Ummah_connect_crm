package com.trackiq.ummah.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.trackiq.ummah.UmmahConnectApp;
import com.trackiq.ummah.databinding.ActivityRegisterCommunityBinding;
import com.trackiq.ummah.ui.main.DashboardActivity;

import java.util.HashMap;
import java.util.Map;

public class RegisterCommunityActivity extends AppCompatActivity {

    private ActivityRegisterCommunityBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterCommunityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        // Setup Click Listeners
        binding.btnRegister.setOnClickListener(v -> registerNewCommunity());
        binding.tvLogin.setOnClickListener(v -> finish()); // Returns to the Login screen
    }

    private void registerNewCommunity() {
        // Retrieve data from UI
        String masjidName = binding.etMasjidName.getText().toString().trim();
        String adminName = binding.etAdminName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        
        // Check which Workspace Type is selected
        String workspaceType = binding.rbMasjid.isChecked() ? "Masjid" : "Community";

        // Basic Validation
        if (masjidName.isEmpty() || adminName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);

        // Step 1: Create the Authentication Account
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            setupCommunitySilo(user.getUid(), masjidName, adminName, phone, email, workspaceType);
                        }
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnRegister.setEnabled(true);
                        Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupCommunitySilo(String adminUid, String masjidName, String adminName, String phone, String email, String workspaceType) {
        // Step 2: Generate a unique Workspace ID (e.g., WS-A4F9B2)
        String uniqueKey = database.child("communities").push().getKey();
        String workspaceId = "WS-" + (uniqueKey != null ? uniqueKey.substring(uniqueKey.length() - 6).toUpperCase() : "000000");

        // Step 3: Prepare the data mapping for atomic multi-path updates
        Map<String, Object> updates = new HashMap<>();
        long timestamp = System.currentTimeMillis();

        // 3a. Create Community Metadata (Including SaaS details)
        Map<String, Object> communityData = new HashMap<>();
        communityData.put("name", masjidName);
        communityData.put("type", workspaceType);
        communityData.put("adminName", adminName);
        communityData.put("phone", phone);
        communityData.put("createdAt", timestamp);
        communityData.put("subscription_status", "trial"); 
        updates.put("/communities/" + workspaceId + "/metadata", communityData);

        // 3b. Add this user as the Super Admin for this workspace
        updates.put("/communities/" + workspaceId + "/admins/" + adminUid, true);

        // 3c. Create the Admin's central user profile
        Map<String, Object> adminData = new HashMap<>();
        adminData.put("name", adminName);
        adminData.put("email", email);
        adminData.put("phone", phone);
        adminData.put("workspaceId", workspaceId);
        adminData.put("role", "SUPER_ADMIN");
        updates.put("/users/" + adminUid, adminData);

        // Step 4: Execute the database write
        database.updateChildren(updates).addOnCompleteListener(task -> {
            binding.progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                
                // Cache the workspace ID locally so the app knows which silo to load
                UmmahConnectApp.getInstance().getPreferences().edit()
                        .putString("current_workspace_id", workspaceId)
                        .putString("cached_user_type", "admin")
                        .apply();

                Toast.makeText(this, "Workspace Created! ID: " + workspaceId, Toast.LENGTH_LONG).show();
                
                // Proceed to Dashboard and clear backstack so they can't go back to registration
                Intent intent = new Intent(RegisterCommunityActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                binding.btnRegister.setEnabled(true);
                Toast.makeText(this, "Database Setup Failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
