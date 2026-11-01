package com.example.yakyeon2.data;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SettingsStateTest {

    @Test
    public void pillboxSwitchOffClearsChildrenAndOnRestoresThem() {
        NotificationPreferences preferences = new NotificationPreferences();
        preferences.setPillboxStatus(true);
        preferences.setBoxOpened(true);
        preferences.setDoseButton(false);
        preferences.setMissedByDevice(true);
        preferences.setDeviceFault(false);

        preferences.setPillboxStatus(false);

        assertFalse(preferences.isPillboxStatus());
        assertFalse(preferences.isBoxOpened());
        assertFalse(preferences.isDoseButton());
        assertFalse(preferences.isMissedByDevice());
        assertFalse(preferences.isDeviceFault());
        preferences.setBoxOpened(true);
        assertFalse(preferences.isBoxOpened());

        preferences.setPillboxStatus(true);

        assertTrue(preferences.isBoxOpened());
        assertFalse(preferences.isDoseButton());
        assertTrue(preferences.isMissedByDevice());
        assertFalse(preferences.isDeviceFault());
    }

    @Test
    public void notificationStateSurvivesExportWithoutTouchingMedications() {
        YakyeonSession session = new YakyeonSession();
        MedicationStore store = ScenarioPrescriptions.knownStore(session);
        String medications = store.exportState();
        NotificationPreferences preferences = session.notifications();
        preferences.setPillboxStatus(true);
        preferences.setBoxOpened(true);
        preferences.setDoseButton(false);
        preferences.setPillboxStatus(false);
        String saved = preferences.exportState();

        NotificationPreferences restored = new NotificationPreferences();
        assertTrue(restored.restoreState(saved));
        restored.setPillboxStatus(true);

        assertTrue(restored.isBoxOpened());
        assertFalse(restored.isDoseButton());
        assertEquals(medications, store.exportState());
        assertFalse(restored.restoreState("11"));
        assertFalse(store.pillboxConnected());
    }

    @Test
    public void shareKeepsCurrentMedicinesAndOnlyKnownPeriods() {
        ClinicalShareOptions options = new ClinicalShareOptions();
        assertEquals(ClinicalShareOptions.DAYS_30, options.getPeriodDays());
        assertTrue(options.includesCurrentMedicines());
        assertFalse(options.setPeriodDays(14));
        assertEquals(ClinicalShareOptions.DAYS_30, options.getPeriodDays());
        assertTrue(options.setPeriodDays(ClinicalShareOptions.DAYS_7));
        options.setIncludeRecords(false);
        options.setIncludeAdherence(true);

        ClinicalShareOptions restored = new ClinicalShareOptions();
        assertTrue(restored.restoreState(options.exportState()));
        assertEquals(ClinicalShareOptions.DAYS_7, restored.getPeriodDays());
        assertFalse(restored.isIncludeRecords());
        assertTrue(restored.isIncludeAdherence());
        assertTrue(restored.includesCurrentMedicines());
        assertFalse(restored.restoreState("14,0000"));
    }

    @Test
    public void customShareRangeSurvivesExportAndCanDropMedicineGroups() {
        ClinicalShareOptions options = new ClinicalShareOptions();
        assertTrue(options.setPeriodRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14)));
        options.setIncludeCurrent(false);
        options.setIncludeFinished(false);
        options.setIncludeDur(false);

        ClinicalShareOptions restored = new ClinicalShareOptions();
        assertTrue(restored.restoreState(options.exportState()));
        assertEquals(LocalDate.of(2026, 9, 1), restored.resolveStart(LocalDate.of(2026, 10, 29)));
        assertEquals(LocalDate.of(2026, 9, 14), restored.resolveEnd(LocalDate.of(2026, 10, 29)));
        assertEquals(14, restored.getPeriodDays());
        assertFalse(restored.isIncludeCurrent());
        assertFalse(restored.isIncludeFinished());
        assertFalse(restored.isIncludeDur());
        assertFalse(options.setPeriodRange(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 1)));
    }

    @Test
    public void legalMarksRoundTripAndDoNotConnectAPillbox() {
        LegalMarks marks = new LegalMarks();
        marks.setDrugCollection(true);
        marks.setDrugNotice(true);
        LegalMarks restored = new LegalMarks();
        assertTrue(restored.restoreState(marks.exportState()));
        assertTrue(restored.isDrugCollection());
        assertTrue(restored.isDrugNotice());
        assertFalse(restored.isTerms());
        YakyeonSession session = new YakyeonSession();
        assertFalse(session.isPillboxConnected());
    }

    @Test
    public void disconnectingThePillboxKeepsSlotsAndRecords() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        int records = store.records().size();
        Integer slot = ScenarioPrescriptions.envelope(store, "한의원").getSlotIndex();
        String exported = store.exportState();
        store.markFixturePillbox();
        assertTrue(store.pillboxConnected());

        store.disconnectPillbox();

        assertFalse(store.pillboxConnected());
        assertEquals(slot, ScenarioPrescriptions.envelope(store, "한의원").getSlotIndex());
        assertEquals(records, store.records().size());
        assertEquals(exported, store.exportState());
    }
}
