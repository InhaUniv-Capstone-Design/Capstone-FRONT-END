package com.example.yakyeon2.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class LocalMedicationState {

    private static final String FILE = "yakyeon_medications";
    private static final String KEY = "state";

    private LocalMedicationState() {
    }

    public static void load(Context context, MedicationStore store) {
        String raw = preferences(context).getString(KEY, "");
        if (raw == null || raw.isEmpty()) {
            return;
        }
        store.importState(raw);
    }

    public static void save(Context context, MedicationStore store) {
        preferences(context).edit().putString(KEY, store.exportState()).commit();
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
