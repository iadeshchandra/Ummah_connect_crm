package com.trackiq.ummah.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.trackiq.ummah.R;
import com.trackiq.ummah.databinding.ActivityPinGenerationBinding;
import com.trackiq.ummah.utils.AuditLogger;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * PinGenerationActivity - Generate/Reset Staff PIN
 * 
 * Features:
 * - Generate new 6-digit secure PIN
 * - Update existing staff record
 * - Create new staff if workspace doesn't exist
 */
public class PinGenerationActivity extends AppCompatActivity {

    private ActivityPinGenerationBinding binding;
    private FirebaseDatabase database;
    private String workspaceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPinGenerationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        database = FirebaseDatabase.getInstance();
        workspaceId = getIntent().getStringExtra("workspace_id");
        
        setupUI();
    }

    private void setupUI() {
        if (workspaceId != null) {
            binding.etWorkspaceId.setText(workspaceId);
            binding.etWorkspaceId.setEnabled(false);
        }
        
        binding.btnGeneratePin.setOnClickListener(v -> generateAndSavePin());
        binding.btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Generate secure 6-digit PIN and save to Firebase
     */
    private void generateAndSavePin() {
        String workspace = binding.etWorkspaceId.getText().toString().trim();
        String staffName = binding.etStaffName.getText().toString().trim();
        String adminCode = binding.etAdminCode.getText().toString().trim();

        if (workspace.isEmpty() || staffName.isEmpty()) {
            Toast.makeText(this, "Workspace ID and Name required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify admin code (simple validation - enhance in production)
        if (!"UMMAH2024".equals(adminCode)) {
            Toast.makeText(this, "Invalid admin authorization code", Toast.LENGTH_SHORT).show();
            return();
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnGeneratePin.setEnabled(false);

        // Generate secure 6-digit PIN
        String newPin = generateSecurePin();
        
        // Prepare staff data
        Map<String, Object> staffData = new HashMap<>();
        staffData.put("name", staffName);
        staffData.put("pin", newPin);
        staffData.put("active", true);
        staffData.put("created_at", System.currentTimeMillis());
        staffData.put("pin_generated_at", System.currentTimeMillis());

        DatabaseReference staffRef = database.getReference("staff").child(workspace);
        
        staffRef.updateChildren(staffData)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnGeneratePin.setEnabled(true);
                    
                    // Display the new PIN
                    binding.tvGeneratedPin.setText("New PIN: " + newPin);
                    binding.tvGeneratedPin.setVisibility(View.VISIBLE);
                    
                    // Save PIN history for audit
                    DatabaseReference pinHistory = database.getReference("pin_history")
                            .child(workspace).push();
                    pinHistory.child("pin").setValue(newPin);
                    pinHistory.child("timestamp").setValue(System.currentTimeMillis());
                    
                    AuditLogger.log(this, "PIN_GENERATED", 
                            "New PIN generated for workspace: " + workspace);
                    
                    Toast.makeText(this, "PIN generated successfully", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnGeneratePin.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Generate cryptographically secure 6-digit PIN
     */
    private String generateSecurePin() {
        SecureRandom random = new SecureRandom();
        int pin = 100000 + random.nextInt(900000); // 100000 to 999999
        return String.valueOf(pin);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
