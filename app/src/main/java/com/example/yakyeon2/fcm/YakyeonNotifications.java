package com.example.yakyeon2.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.yakyeon2.MainActivity;
import com.example.yakyeon2.R;

public final class YakyeonNotifications {

    public static final String CHANNEL_ID = "yakyeon_default";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    private YakyeonNotifications() {
    }

    public static void ensureChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.fcm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.fcm_channel_description));
        manager.createNotificationChannel(channel);
    }

    public static void show(Context context, String title, String body, String notificationId) {
        ensureChannel(context);
        String safeTitle = title == null ? "" : title.trim();
        String safeBody = body == null ? "" : body.trim();
        if (safeTitle.isEmpty() && safeBody.isEmpty()) {
            Log.d(FcmRegistrar.TAG, "제목과 내용이 없어 알림을 표시하지 않음");
            return;
        }
        if (safeTitle.isEmpty()) {
            safeTitle = context.getString(R.string.app_name);
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.d(FcmRegistrar.TAG, "알림 권한이 없어 표시하지 못함");
            return;
        }
        int id = notificationId(notificationId);
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (notificationId != null && !notificationId.isEmpty()) {
            open.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        }
        PendingIntent pending = PendingIntent.getActivity(
                context,
                id,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(context.getColor(R.color.yakyeon_brand))
                .setContentTitle(safeTitle)
                .setContentText(safeBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(safeBody))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
            Log.d(FcmRegistrar.TAG, "알림 표시: " + safeTitle);
        } catch (SecurityException exception) {
            Log.e(FcmRegistrar.TAG, "알림 표시 실패", exception);
        }
    }

    private static int notificationId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return (int) (System.currentTimeMillis() & 0x7fffffff);
        }
        try {
            long value = Long.parseLong(raw);
            if (value > 0L) {
                return (int) (value % Integer.MAX_VALUE);
            }
        } catch (NumberFormatException ignored) {
            return raw.hashCode() & 0x7fffffff;
        }
        return raw.hashCode() & 0x7fffffff;
    }
}
