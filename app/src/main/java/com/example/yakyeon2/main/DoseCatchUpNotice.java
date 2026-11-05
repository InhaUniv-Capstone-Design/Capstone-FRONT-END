package com.example.yakyeon2.main;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.yakyeon2.MainActivity;
import com.example.yakyeon2.R;
import com.example.yakyeon2.api.HomeContracts;

import java.util.HashSet;
import java.util.Set;

public final class DoseCatchUpNotice {

    private static final String CHANNEL = "dose_catchup";
    private static final String PREFS = "yakyeon_catchup";
    private static final String KEY = "notified";

    private DoseCatchUpNotice() {
    }

    public static void post(Context context, HomeContracts.Dose dose, String title, String body) {
        if (context == null || dose == null || !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet(KEY, new HashSet<>());
        Set<String> ids = new HashSet<>(saved == null ? new HashSet<>() : saved);
        String id = String.valueOf(dose.scheduledDoseId);
        if (!ids.add(id)) {
            return;
        }
        prefs.edit().putStringSet(KEY, ids).apply();
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL,
                context.getString(R.string.med_catchup_notice_title),
                NotificationManager.IMPORTANCE_HIGH
        );
        manager.createNotificationChannel(channel);
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                context,
                (int) (dose.scheduledDoseId % Integer.MAX_VALUE),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_yakyeon_pill)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        try {
            NotificationManagerCompat.from(context).notify((int) (dose.scheduledDoseId % Integer.MAX_VALUE), builder.build());
        } catch (SecurityException ignored) {
            ids.remove(id);
            prefs.edit().putStringSet(KEY, ids).apply();
        }
    }
}
