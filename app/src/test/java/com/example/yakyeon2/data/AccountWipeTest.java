package com.example.yakyeon2.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountWipeTest {

    @Test
    public void discardRemovesMedicinesPillboxLinkAndCareLink() {
        YakyeonSession session = new YakyeonSession();
        MedicationStore store = new MedicationStore(session);
        store.connectPillbox();
        session.setCareLink(YakyeonSession.CareLink.LINKED);
        session.notifications().setDoseTime(false);
        store.discardAccountData();
        session.notifications().reset();
        assertFalse(store.pillboxConnected());
        assertEquals(YakyeonSession.CareLink.NONE, session.getCareLink());
        assertTrue(session.medicationIds().isEmpty());
        assertTrue(session.notifications().isDoseTime());
        assertFalse(store.exportState().contains("fixture"));
    }
}
