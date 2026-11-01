package com.example.yakyeon2.navigation;

import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.ScenarioPrescriptions;

/**
 * Debug entry for the saved 한의원 prescription with the pillbox already connected.
 * Slot assignment inside that prescription is separate from this connection flag.
 */
public final class DevPillboxScenarioGate {

    private DevPillboxScenarioGate() {
    }

    public static boolean open(AppNavigator navigator, MedicationStore store, boolean debuggable) {
        if (!debuggable || navigator == null || store == null) {
            return false;
        }
        if (!ScenarioPrescriptions.hasHospital(store, ScenarioPrescriptions.CLINIC)) {
            try {
                ScenarioPrescriptions.installClinic(store);
            } catch (IllegalStateException ex) {
                return false;
            }
        }
        if (!ScenarioPrescriptions.hasHospital(store, ScenarioPrescriptions.CLINIC)) {
            return false;
        }
        store.connectPillbox();
        navigator.selectTab(ScreenId.HOME);
        return navigator.isMainArea()
                && navigator.currentTab() == ScreenId.HOME
                && store.pillboxConnected();
    }
}
