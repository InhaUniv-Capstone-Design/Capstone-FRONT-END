package com.example.yakyeon2.navigation;

import com.example.yakyeon2.data.MedicationRules;
import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.YakyeonSession;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegistrationChromeTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Test
    public void registrationScreensKeepBottomNav() {
        assertTrue(RegistrationChrome.showsBottomNav(false, MedScreen.LIST));
        assertTrue(RegistrationChrome.showsBottomNav(false, MedScreen.CONSENT));
        assertTrue(RegistrationChrome.showsBottomNav(false, MedScreen.ENVELOPE));
        assertTrue(RegistrationChrome.showsBottomNav(false, MedScreen.SLOT));
        assertFalse(RegistrationChrome.showsBottomNav(false, MedScreen.ALARM));
        assertTrue(RegistrationChrome.showsBottomNav(false, MedScreen.SCHEDULE));
        assertTrue(RegistrationChrome.showsBottomNav(false, MedScreen.HISTORY));
        assertTrue(RegistrationChrome.showsBottomNav(true, MedScreen.SLOT));
    }

    @Test
    public void devSampleStaysOutOfReleaseAndDoesNotCommit() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        assertFalse(DevSampleGate.toggleListSample(store, false, ZONE));
        assertFalse(store.hasDevListSample());

        assertTrue(DevSampleGate.toggleListSample(store, true, ZONE));
        assertTrue(store.hasDevListSample());
        assertEquals(1, store.activeEnvelopeCount());
        assertFalse(store.records().isEmpty());
        assertTrue(DevSampleGate.toggleListSample(store, true, ZONE));
        assertFalse(store.hasDevListSample());

        store.beginDraft();
        assertTrue(DevSampleGate.showEnvelopeSample(store, true, LocalDate.of(2026, 10, 8)));
        assertEquals(1, store.editingIndex());
        assertEquals(3, store.draft().medicines().size());
        assertEquals(MedicationRules.SaveBlock.MISSING_REQUIRED, store.commit(ZONE));
        assertEquals(0, store.activeEnvelopeCount());
    }
}
