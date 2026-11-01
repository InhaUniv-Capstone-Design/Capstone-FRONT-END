package com.example.yakyeon2.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class LocalMedicineNames {

    private static final String FILE = "yakyeon_medicine_names";
    private static final String KEY = "custom";

    private LocalMedicineNames() {
    }

    public static void load(Context context, MedicineNameCatalog catalog) {
        catalog.restore(MedicineNameCodec.decode(preferences(context).getString(KEY, "")));
    }

    public static void save(Context context, MedicineNameCatalog catalog) {
        preferences(context).edit()
                .putString(KEY, MedicineNameCodec.encode(catalog.customNames()))
                .commit();
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
