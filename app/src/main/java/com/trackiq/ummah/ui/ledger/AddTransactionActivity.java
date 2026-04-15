package com.trackiq.ummah.ui.ledger;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.R;
import com.trackiq.ummah.UmmahConnectApp;
import com.trackiq.ummah.databinding.ActivityAddTransactionBinding;
import com.trackiq.ummah.model.Transaction;
import com.trackiq.ummah.utils.AuditLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * AddTransactionActivity - Record donations or expenses
 */
public class AddTransactionActivity extends AppCompatActivity {

    private ActivityAddTransactionBinding binding;
    private DatabaseReference ledgerRef;
    private DatabaseReference membersRef;
    private DatabaseReference guestsRef;
    private UmmahConnectApp app;
    private String category = Transaction.CATEGORY_INCOME; // Default to donation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ledgerRef = FirebaseDatabase.getInstance().getReference("ledger");
        membersRef = FirebaseDatabase.getInstance().getReference("members");
        guestsRef = FirebaseDatabase.getInstance().getReference("guests");
        app = UmmahConnectApp.getInstance();

        setupToolbar();
        setupCategoryToggle();
        setupTypeDropdown();
        setupPersonSearch();
        setupDatePicker();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Add Transaction");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategoryToggle() {
        binding.radioGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioIncome) {
                category = Transaction.CATEGORY_INCOME;
                binding.tvCategoryLabel.setText("Donation Type");
                setupDonationTypes();
            } else {
                category = Transaction.CATEGORY_EXPENSE;
                binding.tvCategoryLabel.setText("Expense Type");
                setupExpenseTypes();
            }
        });
        
        // Default to income
        binding.radioIncome.setChecked(true);
        setupDonationTypes();
    }

    private void setupDonationTypes() {
        String[] types = {"Sadaqah", "Zakat", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, types);
        binding.spinnerType.setAdapter(adapter);
    }

    private void setupExpenseTypes() {
        String[] types = {"Masjid Maintenance", "Iftar Program", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, types);
        binding.spinnerType.setAdapter(adapter);
    }

    private void setupTypeDropdown() {
        binding.spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateUIForType();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void updateUIForType() {
        String selected = binding.spinnerType.getSelectedItem().toString();
        // Show person search only for donations
        boolean isDonation = category.equals(Transaction.CATEGORY_INCOME);
        binding.tilPerson.setVisibility(isDonation ? View.VISIBLE : View.GONE);
    }

    private void setupPersonSearch() {
        // Load members and guests for autocomplete
        List<String> personSuggestions = new ArrayList<>();
        
        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    String id = child.getKey();
                    if (name != null) {
                        personSuggestions.add(name + " (MBR-" + id + ")");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        guestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    String id = child.getKey();
                    if (name != null) {
                        personSuggestions.add(name + " (GST-" + id + ")");
                    }
                }
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddTransactionActivity.this,
                        android.R.layout.simple_dropdown_item_1line, personSuggestions);
                binding.etPerson.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupDatePicker() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        binding.etDate.setText(today);
        
        binding.etDate.setOnClickListener(v -> {
            // Date picker dialog would open here
            // For simplicity, using current date
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_save, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveTransaction();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveTransaction() {
        // Get values
        String typeDisplay = binding.spinnerType.getSelectedItem().toString();
        String type;
        switch (typeDisplay) {
            case "Sadaqah":
                type = Transaction.TYPE_SADAQAH;
                break;
            case "Zakat":
                type = Transaction.TYPE_ZAKAT;
                break;
            case "Masjid Maintenance":
                type = Transaction.TYPE_MAINTENANCE;
                break;
            case "Iftar Program":
                type = Transaction.TYPE_IFTAR;
                break;
            default:
                type = Transaction.TYPE_OTHER;
        }

        String amountStr = binding.etAmount.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String date = binding.etDate.getText().toString().trim();
        String personInfo = binding.etPerson.getText().toString().trim();

        // Validation
        if (amountStr.isEmpty()) {
            binding.tilAmount.setError("Amount required");
            return;
        }
        binding.tilAmount.setError(null);

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            binding.tilAmount.setError("Invalid amount");
            return;
        }

        // Parse person info
        String personId = null;
        String personName = null;
        if (!personInfo.isEmpty() && category.equals(Transaction.CATEGORY_INCOME)) {
            // Extract ID from format "Name (MBR-XXXX)" or "Name (GST-XXXX)"
            int start = personInfo.lastIndexOf("(");
            int end = personInfo.lastIndexOf(")");
            if (start != -1 && end != -1) {
                personId = personInfo.substring(start + 1, end);
                personName = personInfo.substring(0, start).trim();
            }
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        // Create transaction
        String recordedBy = app.getPreferences().getString("cached_user_type", "unknown");
        if ("admin".equals(recordedBy)) {
            recordedBy = app.getPreferences().getString("admin_email", "admin");
        } else {
            recordedBy = app.getPreferences().getString("staff_workspace", "staff");
        }

        Transaction transaction = new Transaction(type, category, amount, 
                description, date, recordedBy);
        transaction.setPersonId(personId);
        transaction.setPersonName(personName);
        transaction.setTimestamp(System.currentTimeMillis());

        String transactionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ledgerRef.child(transactionId).setValue(transaction)
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        AuditLogger.log(this, AuditLogger.ACTION_TRANSACTION_ADD,
                                "Recorded " + category + ": " + type + " - $" + amount);
                        Toast.makeText(this, "Transaction recorded", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(),
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
