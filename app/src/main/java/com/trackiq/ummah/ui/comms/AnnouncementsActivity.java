package com.trackiq.ummah.ui.comms;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.trackiq.ummah.R;
import com.trackiq.ummah.UmmahConnectApp;
import com.trackiq.ummah.databinding.ActivityAnnouncementsBinding;
import com.trackiq.ummah.utils.AuditLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * AnnouncementsActivity - Mass communications with WhatsApp Bypass
 * 
 * Features:
 * - Predefined templates (Shura Alert, Ramadan, Eid, Jumuah)
 * - WhatsApp Bypass for manual forwarding
 * - Empty contact list handling
 * - Smart routing
 */
public class AnnouncementsActivity extends AppCompatActivity {

    private ActivityAnnouncementsBinding binding;
    private DatabaseReference announcementsRef;
    private UmmahConnectApp app;
    private String selectedTemplate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnnouncementsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        announcementsRef = FirebaseDatabase.getInstance().getReference("announcements");
        app = UmmahConnectApp.getInstance();

        setupToolbar();
        setupTemplateButtons();
        setupSendButtons();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Mass Announcements");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTemplateButtons() {
        binding.btnShuraAlert.setOnClickListener(v -> loadTemplate("shura"));
        binding.btnRamadan.setOnClickListener(v -> loadTemplate("ramadan"));
        binding.btnEid.setOnClickListener(v -> loadTemplate("eid"));
        binding.btnJumuah.setOnClickListener(v -> loadTemplate("jumuah"));
    }

    private void loadTemplate(String template) {
        String message;
        String title;
        
        switch (template) {
            case "shura":
                title = "Shura Alert";
                message = "🚨 URGENT SHURA MEETING 🚨\n\n" +
                        "As-Salamu Alaykum dear community members,\n\n" +
                        "An emergency Shura meeting has been scheduled.\n\n" +
                        "Date: " + new SimpleDateFormat("EEEE, MMMM dd", Locale.US).format(new Date()) + "\n" +
                        "Time: After Maghrib\n" +
                        "Location: Main Prayer Hall\n\n" +
                        "Your presence is urgently requested.\n\n" +
                        "JazakAllahu Khairan,\n" +
                        "Ummah Connect Team";
                break;
                
            case "ramadan":
                title = "Ramadan Mubarak";
                message = "🌙 RAMADAN MUBARAK 🌙\n\n" +
                        "As-Salamu Alaykum dear brothers and sisters,\n\n" +
                        "The blessed month of Ramadan has begun. May Allah accept our fasting, " +
                        "prayers, and good deeds.\n\n" +
                        "📅 Iftar will be served daily at Maghrib\n" +
                        "🕌 Taraweeh prayers begin at 8:30 PM\n" +
                        "🤲 Jumuah Khutbah topic: \"Mercy and Forgiveness\"\n\n" +
                        "Ramadan Kareem!\n\n" +
                        "Ummah Connect Team";
                break;
                
            case "eid":
                title = "Eid Mubarak";
                message = "🎉 EID MUBARAK 🎉\n\n" +
                        "As-Salamu Alaykum dear community,\n\n" +
                        "Taqabbal Allahu minna wa minkum. " +
                        "May Allah accept our sacrifices and grant us joy.\n\n" +
                        "🕌 Eid Prayer Details:\n" +
                        "Location: Community Center\n" +
                        "Time: 8:00 AM\n" +
                        "Breakfast to follow\n\n" +
                        "Eid Mubarak to you and your families!\n\n" +
                        "Ummah Connect Team";
                break;
                
            case "jumuah":
                title = "Jumuah Reminder";
                message = "🕌 JUMUAH REMINDER 🕌\n\n" +
                        "As-Salamu Alaykum,\n\n" +
                        "Don't forget today's Jumuah prayer!\n\n" +
                        "⏰ Khutbah begins at 1:00 PM\n" +
                        "📖 Topic: \"Patience in Times of Trial\"\n" +
                        "🍲 Community lunch served after prayer\n\n" +
                        "Bring your friends and family!\n\n" +
                        "Ummah Connect Team";
                break;
                
            default:
                title = "Announcement";
                message = "";
        }
        
        selectedTemplate = template;
        binding.tvTemplateTitle.setText(title);
        binding.etMessage.setText(message);
    }

    private void setupSendButtons() {
        // In-App Send (to members with phone numbers)
        binding.btnSendInApp.setOnClickListener(v -> sendInApp());
        
        // WhatsApp Bypass - Copy to clipboard for manual forward
        binding.btnWhatsAppBypass.setOnClickListener(v -> whatsAppBypass());
        
        // Direct WhatsApp (if contacts available)
        binding.btnWhatsAppDirect.setOnClickListener(v -> sendWhatsAppDirect());
    }

    /**
     * Send via in-app announcement system
     */
    private void sendInApp() {
        String message = binding.etMessage.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        String announcementId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> announcement = new HashMap<>();
        announcement.put("id", announcementId);
        announcement.put("title", binding.tvTemplateTitle.getText().toString());
        announcement.put("message", message);
        announcement.put("template", selectedTemplate);
        announcement.put("timestamp", System.currentTimeMillis());
        announcement.put("sentBy", app.getPreferences().getString("cached_user_type", "unknown"));
        announcement.put("status", "sent");

        announcementsRef.child(announcementId).setValue(announcement)
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        AuditLogger.log(this, AuditLogger.ACTION_ANNOUNCEMENT_SENT,
                                "In-app announcement: " + selectedTemplate);
                        Toast.makeText(this, "Announcement sent!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * WhatsApp Bypass - Copy to clipboard for manual forwarding
     * Works even with empty contact list
     */
    private void whatsAppBypass() {
        String message = binding.etMessage.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Copy to clipboard
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Ummah Connect Announcement", message);
        clipboard.setPrimaryClip(clip);

        // Show bypass dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("WhatsApp Bypass")
                .setMessage("Template copied to clipboard!\n\n" +
                        "1. Open WhatsApp manually\n" +
                        "2. Select your broadcast list or contacts\n" +
                        "3. Paste and send\n\n" +
                        "This bypass works even with empty contact lists.")
                .setPositiveButton("Open WhatsApp", (d, w) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setPackage("com.whatsapp");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Copy Only", null)
                .show();

        AuditLogger.log(this, AuditLogger.ACTION_ANNOUNCEMENT_SENT,
                "WhatsApp bypass used for: " + selectedTemplate);
    }

    /**
     * Direct WhatsApp send (requires contacts)
     */
    private void sendWhatsAppDirect() {
        String message = binding.etMessage.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if WhatsApp installed
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            return;
        }

        // For direct send, we would need contact numbers from database
        // This is a simplified version that opens WhatsApp with pre-filled message
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String url = "https://api.whatsapp.com/send?text=" + Uri.encode(message);
        intent.setData(Uri.parse(url));
        
        try {
            startActivity(intent);
            AuditLogger.log(this, AuditLogger.ACTION_ANNOUNCEMENT_SENT,
                    "WhatsApp direct for: " + selectedTemplate);
        } catch (Exception e) {
            // Fallback to bypass
            Toast.makeText(this, "Direct send failed. Use Bypass mode.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
