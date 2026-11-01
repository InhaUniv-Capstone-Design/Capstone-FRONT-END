package com.example.yakyeon2.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InMemoryYakyeonRepositoryTest {

    @Test
    public void startsWithoutUserMedicationsOrConnectedPillbox() {
        YakyeonSession session = new InMemoryYakyeonRepository().session();

        assertNull(session.getAccountType());
        assertTrue(session.medicationIds().isEmpty());
        assertFalse(session.isPillboxConnected());
        assertEquals(3, session.slotCount());
        assertEquals(0, session.occupiedSlotCount());
        assertEquals(10, new InMemoryYakyeonRepository().medicineNames().examples().size());
        assertTrue(new InMemoryYakyeonRepository().medicineNames().customNames().isEmpty());
        assertEquals(4, DoseRound.values().length);
        assertTrue(session.notifications().isDoseTime());
        assertTrue(session.notifications().isMissedDose());
        assertTrue(session.notifications().isEndingSoon());
        assertTrue(session.notifications().isPillboxStatus());
        assertTrue(session.notifications().isBoxOpened());
        assertTrue(session.notifications().isDoseButton());
        assertTrue(session.notifications().isMissedByDevice());
        assertTrue(session.notifications().isDeviceFault());
    }
}
