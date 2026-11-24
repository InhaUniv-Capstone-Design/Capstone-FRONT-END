package com.example.yakyeon2.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
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

    /**
     * Removes lid-open notices already sitting in the shade, including ones the
     * system posted from an FCM notification payload before this process ran.
     */
    public static void dismissOpenNotices(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        StatusBarNotification[] active;
        try {
            active = manager.getActiveNotifications();
        } catch (RuntimeException exception) {
            Log.e(FcmRegistrar.TAG, "열린 알림을 확인하지 못함", exception);
            return;
        }
        if (active == null) {
            return;
        }
        for (StatusBarNotification posted : active) {
            if (posted == null || posted.getNotification() == null) {
                continue;
            }
            if (!isOpenNotice(posted.getNotification())) {
                continue;
            }
            manager.cancel(posted.getTag(), posted.getId());
        }
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

    private static boolean isOpenNotice(Notification notification) {
        Bundle extras = notification.extras;
        if (extras == null) {
            return false;
        }
        String title = text(extras, Notification.EXTRA_TITLE);
        String body = text(extras, Notification.EXTRA_TEXT);
        String big = text(extras, Notification.EXTRA_BIG_TEXT);
        if (PushMessages.confirmsIntake(title, body, big) && !PushMessages.mentionsOpen(title, body, big)) {
            return false;
        }
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        String[] lineText = new String[lines == null ? 0 : lines.length];
        if (lines != null) {
            for (int index = 0; index < lines.length; index++) {
                lineText[index] = lines[index] == null ? null : lines[index].toString();
            }
        }
        return PushMessages.mentionsOpen(
                title,
                body,
                big,
                text(extras, Notification.EXTRA_SUMMARY_TEXT)
        ) || PushMessages.mentionsOpen(lineText);
    }

    private static String text(Bundle extras, String key) {
        CharSequence value = extras.getCharSequence(key);
        return value == null ? null : value.toString();
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
