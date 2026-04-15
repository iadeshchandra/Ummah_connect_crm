package com.trackiq.ummah.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseUser;
import com.trackiq.ummah.R;
import com.trackiq.ummah.UmmahConnectApp;
import com.trackiq.ummah.databinding.ActivityAdminLoginBinding;
import com.trackiq.ummah.ui.main.DashboardActivity;
import com.trackiq.ummah.utils.AuditLogger;

/**
 * AdminLoginActivity - Email/Password Authentication
 * 
 * Features:
 * - Standard Firebase Email/Pass login
 * - Offline cache fallback for network errors
 * - Automatic credential caching for offline mode
 */
public class AdminLoginActivity extends AppCompatActivity {

    private ActivityAdminLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private UmmahConnectApp app;
    private boolean isOfflineMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        firebaseAuth = FirebaseAuth.getInstance();
        app = UmmahConnectApp.getInstance();
        
        // Check if coming from offline cache
        isOfflineMode = getIntent().getBooleanExtra("offline_mode", false);
        
        setupUI();
    }

    private void setupUI() {
        // Toggle between online and offline mode
        binding.btnToggleMode.setOnClickListener(v -> toggleOfflineMode());
        
        // Login button
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        
        // Switch to staff login
        binding.tvStaffLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, StaffLoginActivity.class));
            finish();
        });
        
        if (isOfflineMode) {
            showOfflineModeUI();
        }
    }

    /**
     * Toggle offline/online mode
     */
    private void toggleOfflineMode() {
        isOfflineMode = !isOfflineMode;
        if (isOfflineMode) {
            showOfflineModeUI();
        } else {
            showOnlineModeUI();
        }
    }

    private void showOfflineModeUI() {
        binding.tvModeIndicator.setText(R.string.offline_mode);
        binding.tvModeIndicator.setVisibility(View.VISIBLE);
        binding.etEmail.setEnabled(false);
        binding.etPassword.setEnabled(false);
        binding.btnLogin.setText(R.string.cache_login);
        
        // Check if we have cached credentials
        String cachedEmail = app.getPreferences().getString("admin_email", null);
        if (cachedEmail != null) {
            binding.etEmail.setText(cachedEmail);
            binding.etOfflineHint.setText("Cached: " + cachedEmail);
            binding.etOfflineHint.setVisibility(View.VISIBLE);
        }
    }

    private void showOnlineModeUI() {
        binding.tvModeIndicator.setVisibility(View.GONE);
        binding.etEmail.setEnabled(true);
        binding.etPassword.setEnabled(true);
        binding.btnLogin.setText(R.string.btn_login);
        binding.etOfflineHint.setVisibility(View.GONE);
    }

    /**
     * Attempt login (online or offline)
     */
    private void attemptLogin() {
        if (isOfflineMode) {
            attemptOfflineLogin();
        } else {
            attemptOnlineLogin();
        }
    }

    /**
     * Online Firebase login
     */
    private void attemptOnlineLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            // Cache credentials for offline use
                            cacheAdminCredentials(email, password);
                            
                            // Log audit
                            AuditLogger.log(this, AuditLogger.ACTION_LOGIN, 
                                    "Admin logged in: " + email);
                            
                            // Go to dashboard
                            startActivity(new Intent(this, DashboardActivity.class));
                            finish();
                        } else if (user != null) {
                            // Email not verified
                            Toast.makeText(this, "Please verify your email first", 
                                    Toast.LENGTH_LONG).show();
                            firebaseAuth.signOut();
                        }
                    } else {
                        handleLoginError(task.getException());
                    }
                });
    }

    /**
     * Offline login using cached credentials
     */
    private void attemptOfflineLogin() {
        String cachedEmail = app.getPreferences().getString("admin_email", null);
        String cachedPasswordHash = app.getPreferences().getString("admin_pass_hash", null);
        
        if (cachedEmail == null || cachedPasswordHash == null) {
            new AlertDialog.Builder(this)
                    .setTitle("No Cache Available")
                    .setMessage(R.string.error_offline_no_cache)
                    .setPositiveButton("Go Online", (d, w) -> {
                        isOfflineMode = false;
                        showOnlineModeUI();
                    })
                    .show();
            return;
        }

        // Verify password hash (simplified - use proper encryption in production)
        String inputPassword = binding.etPassword.getText().toString().trim();
        String inputHash = String.valueOf(inputPassword.hashCode());
        
        if (inputHash.equals(cachedPasswordHash)) {
            // Offline login successful
            app.setOfflineMode(true);
            Toast.makeText(this, "Offline login successful", Toast.LENGTH_SHORT).show();
            
            AuditLogger.log(this, AuditLogger.ACTION_LOGIN, 
                    "Admin offline login: " + cachedEmail);
            
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Invalid cached credentials", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Cache credentials for offline use
     */
    private void cacheAdminCredentials(String email, String password) {
        app.getPreferences().edit()
                .putString("cached_user_type", "admin")
                .putString("admin_email", email)
                .putString("admin_pass_hash", String.valueOf(password.hashCode()))
                .putLong("cache_timestamp", System.currentTimeMillis())
                .apply();
    }

    /**
     * Handle Firebase login errors
     */
    private void handleLoginError(Exception exception) {
        String message;
        
        if (exception instanceof FirebaseNetworkException) {
            message = "Network error. Switch to offline mode?";
            new AlertDialog.Builder(this)
                    .setTitle("Network Error")
                    .setMessage(message)
                    .setPositiveButton("Offline Mode", (d, w) -> {
                        isOfflineMode = true;
                        showOfflineModeUI();
                    })
                    .setNegativeButton("Retry", null)
                    .show();
            return;
        } else if (exception instanceof FirebaseAuthInvalidUserException) {
            message = "No account found with this email";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            message = "Invalid password";
        } else {
            message = "Login failed: " + exception.getMessage();
        }
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
