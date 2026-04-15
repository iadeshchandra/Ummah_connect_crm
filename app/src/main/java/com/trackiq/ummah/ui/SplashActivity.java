package com.trackiq.ummah.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.trackiq.ummah.R;
import com.trackiq.ummah.UmmahConnectApp;
import com.trackiq.ummah.ui.auth.AdminLoginActivity;
import com.trackiq.ummah.ui.auth.StaffLoginActivity;
import com.trackiq.ummah.ui.main.DashboardActivity;

/**
 * SplashActivity - Entry point with routing logic
 * 
 * Routes to:
 * - Dashboard if already authenticated
 * - AdminLogin if cached admin session exists
 * - StaffLogin if no session (default)
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1500; // 1.5 seconds
    private FirebaseAuth firebaseAuth;
    private UmmahConnectApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        firebaseAuth = FirebaseAuth.getInstance();
        app = UmmahConnectApp.getInstance();
        
        // Delay for splash screen visibility
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthAndRoute, SPLASH_DELAY);
    }

    /**
     * Check authentication status and route accordingly
     */
    private void checkAuthAndRoute() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser != null && currentUser.isEmailVerified()) {
            // User is signed in - go to Dashboard
            startActivity(new Intent(this, DashboardActivity.class));
        } else {
            // Check for offline cache login
            String cachedUserType = app.getPreferences().getString("cached_user_type", null);
            
            if ("admin".equals(cachedUserType)) {
                // Cached admin - try offline login
                startActivity(new Intent(this, AdminLoginActivity.class)
                        .putExtra("offline_mode", true));
            } else if ("staff".equals(cachedUserType)) {
                // Cached staff - try offline login
                startActivity(new Intent(this, StaffLoginActivity.class)
                        .putExtra("offline_mode", true));
            } else {
                // No cache - default to staff login
                startActivity(new Intent(this, StaffLoginActivity.class));
            }
        }
        
        finish();
    }
}
