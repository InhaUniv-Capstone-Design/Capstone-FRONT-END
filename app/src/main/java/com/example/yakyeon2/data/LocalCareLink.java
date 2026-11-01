package com.example.yakyeon2.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class LocalCareLink {

    private static final String FILE = "yakyeon_care_link";
    private static final String KEY = "state";

    private LocalCareLink() {
    }

    public static void load(Context context, YakyeonSession session) {
        String raw = preferences(context).getString(KEY, YakyeonSession.CareLink.NONE.name());
        YakyeonSession.CareLink parsed = YakyeonSession.CareLink.NONE;
        if (raw != null) {
            try {
                parsed = YakyeonSession.CareLink.valueOf(raw);
            } catch (IllegalArgumentException ignored) {
                parsed = YakyeonSession.CareLink.NONE;
            }
        }
        session.setCareLink(parsed);
    }

    public static void save(Context context, YakyeonSession session) {
        preferences(context).edit().putString(KEY, session.getCareLink().name()).commit();
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
