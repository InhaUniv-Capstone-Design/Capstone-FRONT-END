package com.example.yakyeon.data.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class MockMedicationSearchRepositoryTest {

    @Test
    public void searchMedicationNamesMatchesContainedText() {
        MockMedicationSearchRepository repository = new MockMedicationSearchRepository();

        List<String> matched = repository.searchMedicationNames("타이");

        assertEquals(List.of("타이레놀정"), matched);
        assertTrue(repository.isKnownName("메트포르민정 500mg"));
        assertFalse(repository.isKnownName("없는약"));
    }
}
