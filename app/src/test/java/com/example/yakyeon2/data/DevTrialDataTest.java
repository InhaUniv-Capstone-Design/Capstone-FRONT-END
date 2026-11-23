package com.example.yakyeon2.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DevTrialDataTest {

    @Test
    public void clearRemovesScenarioAndGraphChecksAndLeavesNoSessionStore() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        assertTrue(store.installGraphChecks(
                1,
                ScenarioPrescriptions.ZONE,
                ScenarioPrescriptions.startOf(ScenarioPrescriptions.REFERENCE_DAY)
        ));
        store.setDevGraphRelationScenario(true);

        DevTrialData.clear(store);

        assertNull(ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC));
        assertNull(ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.INTERNAL));
        assertEquals(0, DrugRelations.takingNodes(store).size());
        assertFalse(store.isDevGraphRelationScenario());
        assertFalse(store.pillboxConnected());
    }
}
