package com.trackiq.ummah.ui.audit;

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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.databinding.ActivityAuditLogBinding;
import com.trackiq.ummah.utils.AuditLogAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AuditLogActivity - View system audit logs (Admin only)
 * 
 * Displays all tracked actions in chronological order
 */
public class AuditLogActivity extends AppCompatActivity {

    private ActivityAuditLogBinding binding;
    private DatabaseReference auditRef;
    private AuditLogAdapter adapter;
    private List<Map<String, Object>> auditLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuditLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auditRef = FirebaseDatabase.getInstance().getReference("audit_logs");

        setupToolbar();
        setupRecyclerView();
        loadAuditLogs();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Audit Log");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        auditLogs = new ArrayList<>();
        adapter = new AuditLogAdapter(auditLogs);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    /**
     * Load audit logs from Firebase (limited to last 100)
     */
    private void loadAuditLogs() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        Query query = auditRef.orderByChild("timestamp").limitToLast(100);
        
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                auditLogs.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> log = (Map<String, Object>) child.getValue();
                    if (log != null) {
                        auditLogs.add(log);
                    }
                }

                // Sort by timestamp descending (newest first)
                Collections.sort(auditLogs, (a, b) -> {
                    Long tsA = (Long) a.get("timestamp");
                    Long tsB = (Long) b.get("timestamp");
                    return tsB.compareTo(tsA);
                });

                adapter.notifyDataSetChanged();

                if (auditLogs.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(AuditLogActivity.this, 
                        "Error loading logs: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
