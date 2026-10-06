package com.example.yakyeon.data.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import com.example.yakyeon.data.model.DurInteraction;

import org.junit.Test;

import java.util.List;

public class MockDurInteractionRepositoryTest {

    @Test
    public void interactionsStaySeparateFromMedicationData() {
        MockDurInteractionRepository repository = new MockDurInteractionRepository();
        List<DurInteraction> interactions = repository.getInteractions();

        assertEquals(3, interactions.size());
        DurInteraction first = repository.findById("dur-001");
        DurInteraction second = repository.findById("dur-002");
        assertEquals("아스피린", first.getDrugAName());
        assertEquals("와파린", first.getDrugBName());
        assertEquals("메트포르민정 500mg", second.getDrugAName());
        assertEquals("오메프라졸캡슐 20mg", second.getDrugBName());
        assertNotEquals(first.getInteractionDescription(), second.getInteractionDescription());
        assertNull(repository.findById("missing"));
    }
}
