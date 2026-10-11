package com.example.yakyeon.ui.common;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;

public final class NotificationEntry {

    private NotificationEntry() {
    }

    public static void bind(AppCompatActivity activity) {
        View entry = activity.findViewById(R.id.header_notifications);
        if (entry == null) {
            return;
        }
        entry.setOnClickListener(view -> AppNavigator.openReminders(activity));
    }
}
