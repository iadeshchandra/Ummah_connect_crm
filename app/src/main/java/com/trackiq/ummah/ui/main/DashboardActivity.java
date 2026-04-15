package com.trackiq.ummah.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.trackiq.ummah.R;
import com.trackiq.ummah.UmmahConnectApp;
import com.trackiq.ummah.ui.audit.AuditLogActivity;
import com.trackiq.ummah.ui.auth.AdminLoginActivity;
import com.trackiq.ummah.ui.auth.StaffLoginActivity;
import com.trackiq.ummah.ui.calendar.HijriCalendarActivity;
import com.trackiq.ummah.ui.comms.AnnouncementsActivity;
import com.trackiq.ummah.ui.guests.GuestListActivity;
import com.trackiq.ummah.ui.ledger.LedgerActivity;
import com.trackiq.ummah.ui.members.MemberListActivity;
import com.trackiq.ummah.ui.reports.PdfReportsActivity;
import com.trackiq.ummah.utils.AuditLogger;

/**
 * DashboardActivity - Main navigation hub
 * 
 * Features:
 * - Grid menu for all modules
 * - Navigation drawer with user info
 * - Offline mode indicator
 * - Quick stats display
 */
public class DashboardActivity extends AppCompatActivity implements 
        NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private TextView tvOfflineIndicator;
    private UmmahConnectApp app;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        app = UmmahConnectApp.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        
        initViews();
        setupToolbar();
        setupNavigation();
        setupDashboardCards();
        updateUserInfo();
        checkOfflineMode();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        tvOfflineIndicator = findViewById(R.id.tvOfflineIndicator);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Ummah Connect");
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(this);
        
        // Update header with user info
        View headerView = navigationView.getHeaderView(0);
        TextView tvUserName = headerView.findViewById(R.id.tvNavUserName);
        TextView tvUserType = headerView.findViewById(R.id.tvNavUserType);
        
        String userType = app.getPreferences().getString("cached_user_type", "staff");
        if ("admin".equals(userType)) {
            tvUserName.setText(app.getPreferences().getString("admin_email", "Admin"));
            tvUserType.setText("Administrator");
        } else {
            tvUserName.setText(app.getPreferences().getString("staff_name", "Staff"));
            tvUserType.setText("Staff - " + app.getPreferences().getString("staff_workspace", ""));
        }
    }

    private void setupDashboardCards() {
        // Members Card
        MaterialCardView cardMembers = findViewById(R.id.cardMembers);
        cardMembers.setOnClickListener(v -> {
            startActivity(new Intent(this, MemberListActivity.class));
        });

        // Guests Card
        MaterialCardView cardGuests = findViewById(R.id.cardGuests);
        cardGuests.setOnClickListener(v -> {
            startActivity(new Intent(this, GuestListActivity.class));
        });

        // Ledger Card
        MaterialCardView cardLedger = findViewById(R.id.cardLedger);
        cardLedger.setOnClickListener(v -> {
            startActivity(new Intent(this, LedgerActivity.class));
        });

        // Reports Card
        MaterialCardView cardReports = findViewById(R.id.cardReports);
        cardReports.setOnClickListener(v -> {
            startActivity(new Intent(this, PdfReportsActivity.class));
        });

        // Announcements Card
        MaterialCardView cardAnnouncements = findViewById(R.id.cardAnnouncements);
        cardAnnouncements.setOnClickListener(v -> {
            startActivity(new Intent(this, AnnouncementsActivity.class));
        });

        // Calendar Card
        MaterialCardView cardCalendar = findViewById(R.id.cardCalendar);
        cardCalendar.setOnClickListener(v -> {
            startActivity(new Intent(this, HijriCalendarActivity.class));
        });
    }

    private void updateUserInfo() {
        // Quick stats could be loaded here
    }

    private void checkOfflineMode() {
        if (app.isOfflineMode()) {
            tvOfflineIndicator.setVisibility(View.VISIBLE);
        } else {
            tvOfflineIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_members) {
            startActivity(new Intent(this, MemberListActivity.class));
        } else if (id == R.id.nav_guests) {
            startActivity(new Intent(this, GuestListActivity.class));
        } else if (id == R.id.nav_ledger) {
            startActivity(new Intent(this, LedgerActivity.class));
        } else if (id == R.id.nav_reports) {
            startActivity(new Intent(this, PdfReportsActivity.class));
        } else if (id == R.id.nav_announcements) {
            startActivity(new Intent(this, AnnouncementsActivity.class));
        } else if (id == R.id.nav_calendar) {
            startActivity(new Intent(this, HijriCalendarActivity.class));
        } else if (id == R.id.nav_audit) {
            // Only admin can view audit logs
            if ("admin".equals(app.getPreferences().getString("cached_user_type", ""))) {
                startActivity(new Intent(this, AuditLogActivity.class));
            } else {
                Toast.makeText(this, "Admin access required", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_logout) {
            confirmLogout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("No", null)
                .show();
    }

    private void performLogout() {
        // Log the action
        AuditLogger.log(this, AuditLogger.ACTION_LOGOUT, "User logged out");
        
        // Clear Firebase auth
        firebaseAuth.signOut();
        
        // Clear cache
        app.getPreferences().edit()
                .remove("cached_user_type")
                .remove("admin_email")
                .remove("admin_pass_hash")
                .remove("staff_workspace")
                .remove("staff_pin_hash")
                .remove("staff_name")
                .apply();
        
        app.setOfflineMode(false);
        
        // Return to login
        Intent intent = new Intent(this, StaffLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOfflineMode();
    }
}
