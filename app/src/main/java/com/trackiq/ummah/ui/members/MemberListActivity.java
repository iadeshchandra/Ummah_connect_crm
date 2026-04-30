package com.trackiq.ummah.ui.members;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.databinding.ActivityMemberListBinding;
import com.trackiq.ummah.model.Member;
import com.trackiq.ummah.utils.MemberAdapter;

import java.util.ArrayList;
import java.util.List;

public class MemberListActivity extends AppCompatActivity {

    private ActivityMemberListBinding binding;
    private DatabaseReference membersRef;
    private MemberAdapter adapter;
    private List<Member> memberList;
    private List<Member> filteredList;
    private String currentWorkspaceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMemberListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Check for Workspace ID (SaaS Silo Security)
        SharedPreferences prefs = getSharedPreferences("UmmahPrefs", MODE_PRIVATE);
        currentWorkspaceId = prefs.getString("current_workspace_id", null);

        if (currentWorkspaceId == null) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFab();

        // 2. Initialize Firebase reference specifically for THIS Workspace
        membersRef = FirebaseDatabase.getInstance().getReference("members").child(currentWorkspaceId);

        // 3. Check Role and Hide/Show FAB
        determineUserRoleAndSetupUI();

        loadMembers();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Community Members");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        memberList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new MemberAdapter(filteredList, member -> {
            // Click - open detail
            Intent intent = new Intent(this, MemberDetailActivity.class);
            intent.putExtra("member_id", member.getId());
            startActivity(intent);
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMembers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditMemberActivity.class);
            startActivity(intent);
        });
    }

    /**
     * RBAC Enforcement: Hide FAB by default, show only if Admin or Manager
     */
    private void determineUserRoleAndSetupUI() {
        binding.fabAdd.setVisibility(View.GONE); // Zero Trust

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
                            if ("SUPER_ADMIN".equals(role) || "MANAGER".equals(role)) {
                                binding.fabAdd.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        } else {
            // Staff members logged in via PIN act as Managers
            SharedPreferences prefs = getSharedPreferences("UmmahPrefs", MODE_PRIVATE);
            if ("staff".equals(prefs.getString("cached_user_type", ""))) {
                binding.fabAdd.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Load members from Firebase specific to the Workspace Silo
     */
    private void loadMembers() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        membersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                memberList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Member member = dataSnapshot.getValue(Member.class);
                    if (member != null) {
                        member.setId(dataSnapshot.getKey());
                        memberList.add(member);
                    }
                }

                // Sort by name
                memberList.sort((m1, m2) -> {
                    String name1 = m1.getName() != null ? m1.getName() : "";
                    String name2 = m2.getName() != null ? m2.getName() : "";
                    return name1.compareToIgnoreCase(name2);
                });

                filteredList.clear();
                filteredList.addAll(memberList);
                adapter.notifyDataSetChanged();

                if (memberList.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberListActivity.this, 
                        "Error loading members: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Filter members by search query
     */
    private void filterMembers(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(memberList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Member member : memberList) {
                boolean matchName = member.getName() != null && member.getName().toLowerCase().contains(lowerQuery);
                boolean matchId = member.getId() != null && member.getId().toLowerCase().contains(lowerQuery);
                boolean matchPhone = member.getPhone() != null && member.getPhone().contains(lowerQuery);

                if (matchName || matchId || matchPhone) {
                    filteredList.add(member);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredList.isEmpty() && !query.isEmpty()) {
            binding.tvEmpty.setText("No members found");
            binding.tvEmpty.setVisibility(View.VISIBLE);
        } else if (memberList.isEmpty()) {
            binding.tvEmpty.setText("No members yet");
            binding.tvEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (membersRef != null) {
            loadMembers();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
