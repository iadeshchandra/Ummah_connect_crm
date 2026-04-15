package com.trackiq.ummah.ui.comms;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.UmmahConnectApp;
import com.trackiq.ummah.databinding.ActivityShuraPollsBinding;
import com.trackiq.ummah.utils.AuditLogger;
import com.trackiq.ummah.utils.PollAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ShuraPollsActivity - Create and manage community polls
 * 
 * Features:
 * - Create polls with multiple options
 * - Real-time voting
 * - Results display
 */
public class ShuraPollsActivity extends AppCompatActivity {

    private ActivityShuraPollsBinding binding;
    private DatabaseReference pollsRef;
    private UmmahConnectApp app;
    private PollAdapter adapter;
    private List<Map<String, Object>> polls;
    private List<String> currentOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShuraPollsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pollsRef = FirebaseDatabase.getInstance().getReference("shura_polls");
        app = UmmahConnectApp.getInstance();
        currentOptions = new ArrayList<>();

        setupToolbar();
        setupRecyclerView();
        setupFab();
        loadPolls();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Shura Polls");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        polls = new ArrayList<>();
        adapter = new PollAdapter(polls, (pollId, optionIndex) -> castVote(pollId, optionIndex));
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        // Only admin can create polls
        boolean isAdmin = "admin".equals(app.getPreferences().getString("cached_user_type", ""));
        binding.fabAdd.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        
        binding.fabAdd.setOnClickListener(v -> showCreatePollDialog());
    }

    private void loadPolls() {
        binding.progressBar.setVisibility(View.VISIBLE);

        pollsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                polls.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> poll = (Map<String, Object>) child.getValue();
                    if (poll != null) {
                        poll.put("id", child.getKey());
                        polls.add(0, poll); // Add to beginning (newest first)
                    }
                }

                adapter.notifyDataSetChanged();

                if (polls.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(ShuraPollsActivity.this, 
                        "Error loading polls", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreatePollDialog() {
        currentOptions.clear();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Shura Poll");

        View view = getLayoutInflater().inflate(com.trackiq.ummah.R.layout.dialog_create_poll, null);
        TextInputEditText etQuestion = view.findViewById(com.trackiq.ummah.R.id.etQuestion);
        
        builder.setView(view);

        builder.setPositiveButton("Add Option", (d, w) -> {
            String option = etQuestion.getText().toString().trim();
            if (!option.isEmpty()) {
                currentOptions.add(option);
                showAddOptionDialog(etQuestion.getText().toString());
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddOptionDialog(String question) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Option " + (currentOptions.size() + 1));

        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Option text");
        builder.setView(input);

        builder.setPositiveButton("Add", (d, w) -> {
            String option = input.getText().toString().trim();
            if (!option.isEmpty()) {
                currentOptions.add(option);
                // Ask if more options
                new AlertDialog.Builder(this)
                        .setTitle("Add another option?")
                        .setPositiveButton("Yes", (d2, w2) -> showAddOptionDialog(question))
                        .setNegativeButton("Publish Poll", (d2, w2) -> publishPoll(question))
                        .show();
            }
        });

        builder.setNegativeButton("Publish", (d, w) -> publishPoll(question));
        builder.show();
    }

    private void publishPoll(String question) {
        if (currentOptions.size() < 2) {
            Toast.makeText(this, "Need at least 2 options", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        String pollId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Map<String, Object> poll = new HashMap<>();
        poll.put("question", question);
        poll.put("options", currentOptions);
        poll.put("votes", new HashMap<String, Integer>());
        poll.put("timestamp", System.currentTimeMillis());
        poll.put("createdBy", app.getPreferences().getString("cached_user_type", "unknown"));
        poll.put("active", true);

        pollsRef.child(pollId).setValue(poll)
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        AuditLogger.log(this, AuditLogger.ACTION_POLL_CREATED,
                                "Created poll: " + question);
                        Toast.makeText(this, "Poll published!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void castVote(String pollId, int optionIndex) {
        String userId = app.getPreferences().getString("cached_user_type", "unknown");
        if ("admin".equals(userId)) {
            userId = app.getPreferences().getString("admin_email", "admin");
        } else {
            userId = app.getPreferences().getString("staff_workspace", "staff");
        }

        pollsRef.child(pollId).child("votes").child(userId.replace(".", "_"))
                .setValue(optionIndex)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Vote recorded!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
