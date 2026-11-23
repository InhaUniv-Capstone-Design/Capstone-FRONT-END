package com.example.yakyeon2.data;

/**
 * Clears debug trial medicines. It does not touch {@link AuthTokenStore}.
 * Call this only from the logged-out debug tools.
 */
public final class DevTrialData {

    private DevTrialData() {
    }

    public static void clear(MedicationStore store) {
        if (store == null) {
            return;
        }
        DevFixtures.reset(store);
        ScenarioPrescriptions.removeScenario(store);
        store.clearGraphChecks();
        store.setDevGraphRelationScenario(false);
    }
}
