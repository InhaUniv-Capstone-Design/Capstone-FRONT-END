package com.example.yakyeon2.navigation;

import com.example.yakyeon2.data.MedicationStore;

import java.time.LocalDate;
import java.time.ZoneId;

public final class DevSampleGate {

    private DevSampleGate() {
    }

    public static boolean toggleListSample(MedicationStore store, boolean debuggable, ZoneId zone) {
        if (!debuggable || store == null || zone == null) {
            return false;
        }
        if (store.hasDevListSample()) {
            return store.clearDevListSample();
        }
        return store.loadDevListSample(zone);
    }

    public static boolean showEnvelopeSample(MedicationStore store, boolean debuggable, LocalDate today) {
        if (!debuggable || store == null || today == null) {
            return false;
        }
        return store.loadDevEnvelopeSample(today);
    }
}
