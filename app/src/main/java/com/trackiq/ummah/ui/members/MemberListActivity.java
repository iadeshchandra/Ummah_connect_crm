package com.trackiq.ummah.ui.members;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.R;
import com.trackiq.ummah.databinding.ActivityMemberListBinding;
import com.trackiq.ummah.model.Member;
import com.trackiq.ummah.utils.AuditLogger;
import com.trackiq.ummah.utils.MemberAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * MemberListActivity - Display and manage members (MBR-XXXX)
 * 
 * Features:
 * - List all members with MBR- prefix IDs
 * - Real-time search/filter
 * - Offline-first data loading
 * - Add/Edit navigation
 */
public class MemberListActivity extends AppCompatActivity {

    private ActivityMemberListBinding binding;
    private DatabaseReference membersRef;
    private MemberAdapter adapter;
    private List<Member> memberList;
    private List<Member> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMemberListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFab();
        
        // Initialize Firebase reference
        membersRef = FirebaseDatabase.getInstance().getReference("members");
        
        loadMembers();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Members");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
     * Load members from Firebase (works offline via persistence)
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
                memberList.sort((m1, m2) -> m1.getName().compareToIgnoreCase(m2.getName()));
                
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
                if (member.getName().toLowerCase().contains(lowerQuery) ||
                    member.getId().toLowerCase().contains(lowerQuery) ||
                    (member.getPhone() != null && member.getPhone().contains(lowerQuery))) {
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
        // Refresh list
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
