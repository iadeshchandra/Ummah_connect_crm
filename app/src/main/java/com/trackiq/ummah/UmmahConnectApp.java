package com.trackiq.ummah;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;

/**
 * UmmahConnectApp - Application Class
 * 
 * Initializes Firebase with Offline-First architecture using Disk Persistence.
 * All Firebase operations will work offline and sync when connectivity returns.
 */
public class UmmahConnectApp extends Application {

    private static final String TAG = "UmmahConnectApp";
    private static final String PREFS_NAME = "UmmahConnectPrefs";
    private static final String KEY_OFFLINE_MODE = "offline_mode";
    private static final String KEY_LAST_SYNC = "last_sync_timestamp";
    
    private static UmmahConnectApp instance;
    private SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Initialize SharedPreferences
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Initialize Firebase with Offline-First configuration
        initializeFirebase();
        
        Log.i(TAG, "Ummah Connect CRM Initialized - Offline-First Mode Enabled");
    }

    /**
     * Initialize Firebase with disk persistence enabled (Offline-First)
     */
    private void initializeFirebase() {
        // Initialize Firebase App
        FirebaseApp.initializeApp(this);
        
        // Enable disk persistence for Realtime Database (CRITICAL for Offline-First)
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(true);
        
        // Set database log level for debugging (remove in production)
        if (BuildConfig.DEBUG) {
            database.setLogLevel(Logger.Level.DEBUG);
        }
        
        // Keep critical data synced even when not actively listening
        // This ensures data is available offline immediately
        database.getReference("members").keepSynced(true);
        database.getReference("guests").keepSynced(true);
        database.getReference("ledger").keepSynced(true);
        database.getReference("audit_logs").keepSynced(true);
        
        Log.i(TAG, "Firebase Disk Persistence Enabled - Data will be cached locally");
    }

    /**
     * Get application instance
     */
    public static UmmahConnectApp getInstance() {
        return instance;
    }

    /**
     * Get SharedPreferences for app-wide settings
     */
    public SharedPreferences getPreferences() {
        return preferences;
    }

    /**
     * Check if app is in offline mode
     */
    public boolean isOfflineMode() {
        return preferences.getBoolean(KEY_OFFLINE_MODE, false);
    }

    /**
     * Set offline mode status
     */
    public void setOfflineMode(boolean offline) {
        preferences.edit().putBoolean(KEY_OFFLINE_MODE, offline).apply();
    }

    /**
     * Update last sync timestamp
     */
    public void updateLastSync() {
        preferences.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply();
    }

    /**
     * Get last sync timestamp
     */
    public long getLastSync() {
        return preferences.getLong(KEY_LAST_SYNC, 0);
    }

    /**
     * Clear all cached data (use with caution)
     */
    public void clearCache() {
        // Clear Firebase disk cache
        FirebaseDatabase.getInstance().purgeOutstandingWrites();
        Log.i(TAG, "Firebase cache cleared");
    }
}
