package com.example.yakyeon2.data;

import android.content.Context;

/**
 * This device keeps one account's cache. Wiping these files removes that account only.
 * Call this after account deletion or logout succeeds, not before.
 */
public final class LocalAccountData {

    private LocalAccountData() {
    }

    public static void wipe(Context context) {
        AuthTokenStore.clear(context);
        LocalMedicationState.clear(context);
        LocalAppSettings.clear(context);
        LocalCareLink.clear(context);
        LocalMedicineNames.clear(context);
    }
}
