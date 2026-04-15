package com.trackiq.ummah.utils;

import android.content.Context;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.trackiq.ummah.UmmahConnectApp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AuditLogger - Silent Audit Log System
 * 
 * Tracks all manager actions without disrupting UI flow.
 * All logs are queued and synced when online.
 */
public class AuditLogger {

    // Action constants
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_MEMBER_ADD = "MEMBER_ADD";
    public static final String ACTION_MEMBER_EDIT = "MEMBER_EDIT";
    public static final String ACTION_MEMBER_DELETE = "MEMBER_DELETE";
    public static final String ACTION_GUEST_ADD = "GUEST_ADD";
    public static final String ACTION_GUEST_EDIT = "GUEST_EDIT";
    public static final String ACTION_TRANSACTION_ADD = "TRANSACTION_ADD";
    public static final String ACTION_TRANSACTION_EDIT = "TRANSACTION_EDIT";
    public static final String ACTION_PDF_GENERATED = "PDF_GENERATED";
    public static final String ACTION_ANNOUNCEMENT_SENT = "ANNOUNCEMENT_SENT";
    public static final String ACTION_POLL_CREATED = "POLL_CREATED";
    public static final String ACTION_DEEP_SCAN = "DEEP_SCAN";
    public static final String ACTION_FALLBACK_SCAN = "FALLBACK_SCAN";
    public static final String ACTION_PIN_GENERATED = "PIN_GENERATED";

    private static final String AUDIT_PATH = "audit_logs";

    /**
     * Log an action silently (non-blocking)
     */
    public static void log(Context context, String action, String details) {
        try {
            UmmahConnectApp app = UmmahConnectApp.getInstance();
            DatabaseReference auditRef = FirebaseDatabase.getInstance().getReference(AUDIT_PATH);
            
            String logId = UUID.randomUUID().toString();
            Map<String, Object> logEntry = new HashMap<>();
            
            // User info
            String userType = app.getPreferences().getString("cached_user_type", "unknown");
            String userId = getUserIdentifier(app);
            
            logEntry.put("timestamp", System.currentTimeMillis());
            logEntry.put("action", action);
            logEntry.put("details", details);
            logEntry.put("user_type", userType);
            logEntry.put("user_id", userId);
            logEntry.put("device_info", android.os.Build.MODEL);
            logEntry.put("app_version", getAppVersion(context));
            
            // Save to Firebase (works offline via persistence)
            auditRef.child(logId).setValue(logEntry);
            
        } catch (Exception e) {
            // Silent fail - never crash the app for audit logging
            e.printStackTrace();
        }
    }

    /**
     * Log with additional data
     */
    public static void logWithData(Context context, String action, String details, 
                                   Map<String, Object> additionalData) {
        try {
            UmmahConnectApp app = UmmahConnectApp.getInstance();
            DatabaseReference auditRef = FirebaseDatabase.getInstance().getReference(AUDIT_PATH);
            
            String logId = UUID.randomUUID().toString();
            Map<String, Object> logEntry = new HashMap<>();
            
            logEntry.put("timestamp", System.currentTimeMillis());
            logEntry.put("action", action);
            logEntry.put("details", details);
            logEntry.put("user_type", app.getPreferences().getString("cached_user_type", "unknown"));
            logEntry.put("user_id", getUserIdentifier(app));
            logEntry.put("additional_data", additionalData);
            
            auditRef.child(logId).setValue(logEntry);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get current user identifier
     */
    private static String getUserIdentifier(UmmahConnectApp app) {
        String userType = app.getPreferences().getString("cached_user_type", "unknown");
        
        if ("admin".equals(userType)) {
            return app.getPreferences().getString("admin_email", "unknown_admin");
        } else if ("staff".equals(userType)) {
            return app.getPreferences().getString("staff_workspace", "unknown_staff");
        }
        
        return "anonymous";
    }

    /**
     * Get app version
     */
    private static String getAppVersion(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
