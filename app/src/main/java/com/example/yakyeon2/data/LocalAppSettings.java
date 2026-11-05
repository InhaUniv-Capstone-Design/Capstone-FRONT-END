package com.example.yakyeon2.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class LocalAppSettings {

    private static final String FILE = "yakyeon_app_settings";
    private static final String NOTIFICATIONS = "notifications";
    private static final String SHARE = "share";
    private static final String LEGAL = "legal";
    private static final String BUZZER = "buzzer";
    private static final String LED = "led";
    private static final String ACCOUNT = "account";

    private LocalAppSettings() {
    }

    public static void loadNotifications(Context context, NotificationPreferences preferences) {
        String raw = preferences(context).getString(NOTIFICATIONS, null);
        if (raw != null) {
            preferences.restoreState(raw);
        }
    }

    public static void saveNotifications(Context context, NotificationPreferences preferences) {
        preferences(context).edit().putString(NOTIFICATIONS, preferences.exportState()).commit();
    }

    public static ClinicalShareOptions loadShare(Context context) {
        ClinicalShareOptions options = new ClinicalShareOptions();
        String raw = preferences(context).getString(SHARE, null);
        if (raw != null) {
            options.restoreState(raw);
        }
        options.setIncludeAdherence(true);
        return options;
    }

    public static void saveShare(Context context, ClinicalShareOptions options) {
        preferences(context).edit().putString(SHARE, options.exportState()).commit();
    }

    public static LegalMarks loadLegal(Context context) {
        LegalMarks marks = new LegalMarks();
        String raw = preferences(context).getString(LEGAL, null);
        if (raw != null) {
            marks.restoreState(raw);
        }
        return marks;
    }

    public static void saveLegal(Context context, LegalMarks marks) {
        preferences(context).edit().putString(LEGAL, marks.exportState()).commit();
    }

    public static boolean loadBuzzer(Context context) {
        return preferences(context).getBoolean(BUZZER, false);
    }

    public static boolean loadLed(Context context) {
        return preferences(context).getBoolean(LED, false);
    }

    public static void saveBuzzer(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(BUZZER, enabled).commit();
    }

    public static void saveLed(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(LED, enabled).commit();
    }

    public static AccountType loadAccountType(Context context) {
        String raw = preferences(context).getString(ACCOUNT, null);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return AccountType.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().commit();
    }

    public static void saveAccountType(Context context, AccountType type) {
        preferences(context).edit()
                .putString(ACCOUNT, type == null ? "" : type.name())
                .commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
