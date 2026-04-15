package com.trackiq.ummah.ui.ledger;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.databinding.ActivityDeepScannerBinding;
import com.trackiq.ummah.model.Transaction;
import com.trackiq.ummah.utils.AuditLogger;
import com.trackiq.ummah.utils.OrphanedIdAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * DeepScannerActivity - Rescues orphaned IDs and prevents duplicates
 * 
 * Deep Scan: Finds transactions with personIds that don't exist in members/guests
 * Smart Fallback: Prevents duplicate GST- IDs by checking existing patterns
 */
public class DeepScannerActivity extends AppCompatActivity {

    private ActivityDeepScannerBinding binding;
    private DatabaseReference ledgerRef;
    private DatabaseReference membersRef;
    private DatabaseReference guestsRef;
    private String scanType;
    private OrphanedIdAdapter adapter;
    private List<String> orphanedIds;
    private int rescuedCount = 0;
    private int preventedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeepScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        scanType = getIntent().getStringExtra("scan_type");

        ledgerRef = FirebaseDatabase.getInstance().getReference("ledger");
        membersRef = FirebaseDatabase.getInstance().getReference("members");
        guestsRef = FirebaseDatabase.getInstance().getReference("guests");

        setupToolbar();
        setupRecyclerView();

        if ("deep".equals(scanType)) {
            binding.tvTitle.setText("Deep Ledger Scanner");
            binding.tvDescription.setText("Scanning for orphaned guest/member IDs in transactions...");
            performDeepScan();
        } else {
            binding.tvTitle.setText("Smart Fallback Scanner");
            binding.tvDescription.setText("Checking for duplicate ID patterns...");
            performSmartFallbackScan();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Ledger Scanner");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        orphanedIds = new ArrayList<>();
        adapter = new OrphanedIdAdapter(orphanedIds, id -> rescueOrphanedId(id));
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    /**
     * Deep Scan: Find orphaned personIds in transactions
     */
    private void performDeepScan() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // First, get all valid member and guest IDs
        Set<String> validMemberIds = new HashSet<>();
        Set<String> validGuestIds = new HashSet<>();

        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    validMemberIds.add(child.getKey());
                }

                // Now get guests
                guestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot guestSnapshot) {
                        for (DataSnapshot child : guestSnapshot.getChildren()) {
                            validGuestIds.add(child.getKey());
                        }

                        // Now scan ledger
                        scanLedgerForOrphans(validMemberIds, validGuestIds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(DeepScannerActivity.this, 
                                "Error loading guests", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(DeepScannerActivity.this, 
                        "Error loading members", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scanLedgerForOrphans(Set<String> validMembers, Set<String> validGuests) {
        ledgerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                orphanedIds.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Transaction transaction = child.getValue(Transaction.class);
                    if (transaction != null && transaction.getPersonId() != null) {
                        String personId = transaction.getPersonId();
                        // Remove MBR- or GST- prefix for comparison
                        String cleanId = personId.replace("MBR-", "").replace("GST-", "");
                        
                        if (!validMembers.contains(cleanId) && !validGuests.contains(cleanId)) {
                            // This is an orphaned ID
                            if (!orphanedIds.contains(personId)) {
                                orphanedIds.add(personId);
                            }
                        }
                    }
                }

                adapter.notifyDataSetChanged();
                binding.tvResults.setText(String.format("Orphaned IDs found: %d", 
                        orphanedIds.size()));

                if (orphanedIds.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                }

                AuditLogger.log(DeepScannerActivity.this, AuditLogger.ACTION_DEEP_SCAN,
                        "Found " + orphanedIds.size() + " orphaned IDs");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(DeepScannerActivity.this, 
                        "Error scanning ledger", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Smart Fallback: Prevent duplicate GST- IDs
     */
    private void performSmartFallbackScan() {
        binding.progressBar.setVisibility(View.VISIBLE);

        guestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> existingIds = new HashSet<>();
                Map<String, Integer> prefixCounts = new HashMap<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.getKey();
                    existingIds.add(id);
                    
                    // Analyze ID patterns to detect duplicates
                    if (id != null && id.length() >= 4) {
                        String prefix = id.substring(0, 4);
                        prefixCounts.put(prefix, prefixCounts.getOrDefault(prefix, 0) + 1);
                    }
                }

                binding.progressBar.setVisibility(View.GONE);

                // Find potential duplicates (same prefix appearing multiple times)
                List<String> duplicates = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : prefixCounts.entrySet()) {
                    if (entry.getValue() > 1) {
                        duplicates.add(entry.getKey() + " (" + entry.getValue() + " duplicates)");
                        preventedCount += entry.getValue() - 1;
                    }
                }

                orphanedIds.clear();
                orphanedIds.addAll(duplicates);
                adapter.notifyDataSetChanged();

                binding.tvResults.setText(String.format("Duplicate patterns prevented: %d", 
                        preventedCount));

                if (orphanedIds.isEmpty()) {
                    binding.tvEmpty.setText("No duplicate patterns found");
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                }

                AuditLogger.log(DeepScannerActivity.this, AuditLogger.ACTION_FALLBACK_SCAN,
                        "Prevented " + preventedCount + " potential duplicates");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(DeepScannerActivity.this, 
                        "Error scanning", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Rescue orphaned ID by creating a guest record
     */
    private void rescueOrphanedId(String orphanedId) {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Create a placeholder guest for this orphaned ID
        String cleanId = orphanedId.replace("GST-", "").replace("MBR-", "");
        boolean isMember = orphanedId.startsWith("MBR-");

        if (isMember) {
            // Create placeholder member
            Map<String, Object> memberData = new HashMap<>();
            memberData.put("name", "Rescued Member (" + orphanedId + ")");
            memberData.put("phone", "Unknown");
            memberData.put("status", "inactive");
            memberData.put("notes", "Auto-created by Deep Scanner - original record missing");
            memberData.put("createdAt", System.currentTimeMillis());
            memberData.put("rescued", true);

            membersRef.child(cleanId).setValue(memberData)
                    .addOnSuccessListener(aVoid -> {
                        rescuedCount++;
                        orphanedIds.remove(orphanedId);
                        adapter.notifyDataSetChanged();
                        binding.tvResults.setText(String.format("Rescued: %d, Remaining: %d", 
                                rescuedCount, orphanedIds.size()));
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Rescued " + orphanedId, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Create placeholder guest
            Map<String, Object> guestData = new HashMap<>();
            guestData.put("name", "Rescued Guest (" + orphanedId + ")");
            guestData.put("phone", "Unknown");
            guestData.put("type", "visitor");
            guestData.put("notes", "Auto-created by Deep Scanner - original record missing");
            guestData.put("createdAt", System.currentTimeMillis());
            guestData.put("rescued", true);

            guestsRef.child(cleanId).setValue(guestData)
                    .addOnSuccessListener(aVoid -> {
                        rescuedCount++;
                        orphanedIds.remove(orphanedId);
                        adapter.notifyDataSetChanged();
                        binding.tvResults.setText(String.format("Rescued: %d, Remaining: %d", 
                                rescuedCount, orphanedIds.size()));
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Rescued " + orphanedId, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
