package com.trackiq.ummah.ui.ledger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.R;
import com.trackiq.ummah.databinding.ActivityLedgerBinding;
import com.trackiq.ummah.model.Transaction;
import com.trackiq.ummah.utils.LedgerPagerAdapter;
import com.trackiq.ummah.utils.TransactionAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * LedgerActivity - Financial management with tabs for Donations and Expenses
 */
public class LedgerActivity extends AppCompatActivity {

    private ActivityLedgerBinding binding;
    private DatabaseReference ledgerRef;
    private LedgerPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLedgerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ledgerRef = FirebaseDatabase.getInstance().getReference("ledger");

        setupToolbar();
        setupViewPager();
        setupFab();
        setupScannerButtons();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Finance Ledger");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupViewPager() {
        pagerAdapter = new LedgerPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText(R.string.tab_donations);
                    } else {
                        tab.setText(R.string.tab_expenses);
                    }
                }).attach();
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            startActivity(intent);
        });
    }

    private void setupScannerButtons() {
        binding.btnDeepScan.setOnClickListener(v -> {
            Intent intent = new Intent(this, DeepScannerActivity.class);
            intent.putExtra("scan_type", "deep");
            startActivity(intent);
        });

        binding.btnSmartFallback.setOnClickListener(v -> {
            Intent intent = new Intent(this, DeepScannerActivity.class);
            intent.putExtra("scan_type", "fallback");
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
