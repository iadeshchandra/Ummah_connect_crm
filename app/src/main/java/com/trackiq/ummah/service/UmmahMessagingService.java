package com.trackiq.ummah.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.trackiq.ummah.R;
import com.trackiq.ummah.ui.main.DashboardActivity;

import java.util.Map;

/**
 * UmmahMessagingService - Firebase Cloud Messaging service
 * 
 * Handles push notifications for:
 * - Shura alerts
 * - Announcements
 * - Prayer reminders
 */
public class UmmahMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "ummah_connect_channel";
    private static final String CHANNEL_NAME = "Ummah Connect Notifications";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Send token to server if needed
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> data = message.getData();
        String title = message.getNotification() != null ? 
                message.getNotification().getTitle() : "Ummah Connect";
        String body = message.getNotification() != null ? 
                message.getNotification().getBody() : "New notification";

        // Check notification type
        String type = data.get("type");
        if ("shura_alert".equals(type)) {
            showNotification(title, body, R.color.error_red);
        } else if ("announcement".equals(type)) {
            showNotification(title, body, R.color.info_blue);
        } else if ("prayer_reminder".equals(type)) {
            showNotification(title, body, R.color.islamic_green);
        } else {
            showNotification(title, body, R.color.islamic_gold);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications from Ummah Connect CRM");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String title, String message, int colorRes) {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setColor(getColor(colorRes));

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
