package com.example.yakyeon.data.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.yakyeon.data.model.Medication;
import com.example.yakyeon.data.model.PrescriptionOcrResult;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MockPrescriptionOcrRepositoryTest {

    @Test
    public void recognizeReturnsSplitDoseAndReviewItem() {
        List<PrescriptionOcrResult> results = new MockPrescriptionOcrRepository().recognize();

        assertEquals(3, results.size());
        PrescriptionOcrResult metformin = results.get(0);
        assertEquals("메트포르민정 500mg", metformin.getName());
        assertEquals(1, metformin.getDoseAmount(), 0);
        assertEquals("정", metformin.getDoseUnit());
        assertEquals("1정", metformin.formatDose());
        assertTrue(metformin.getDuration().contains("~"));

        PrescriptionOcrResult amlodipine = results.get(2);
        assertEquals(0.5, amlodipine.getDoseAmount(), 0);
        assertEquals("0.5정", amlodipine.formatDose());
        assertTrue(amlodipine.needsReview());
    }

    @Test
    public void registeredOcrResultAppearsInTheActiveList() {
        MockMedicationRepository medications = new MockMedicationRepository();
        PrescriptionOcrResult result = new MockPrescriptionOcrRepository().recognize().get(2);
        int before = medications.getActiveMedications().size();

        String id = medications.addMedication(
                result.getName(),
                result.getDosage(),
                result.getDoseAmount(),
                result.getDoseUnit(),
                result.getTimesPerDay(),
                result.getDoseTimes(),
                result.getDoseTiming(),
                result.getDuration()
        );

        Medication saved = medications.findById(id);
        assertEquals("0.5정", saved.formatDose());
        assertEquals(Medication.Status.ACTIVE, saved.getStatus());
        assertFalse(saved.getDuration().isEmpty());
        assertEquals(before + 1, medications.getActiveMedications().size());
    }

    @Test
    public void editingAnOcrResultDoesNotRegisterUntilSavedIntoTheList() {
        MockMedicationRepository medications = new MockMedicationRepository();
        List<PrescriptionOcrResult> recognized = new ArrayList<>(new MockPrescriptionOcrRepository().recognize());
        int before = medications.getMedications().size();

        recognized.set(0, recognized.get(0).withEdits(
                "메트포르민정 250mg",
                0.5,
                "정",
                "1회",
                "19:00",
                "저녁",
                recognized.get(0).getDuration()
        ));

        assertEquals(before, medications.getMedications().size());
        assertEquals("메트포르민정 250mg", recognized.get(0).getName());
        assertEquals("250mg", recognized.get(0).getDosage());
        assertEquals("0.5정 · 1회 · 저녁", recognized.get(0).formatSchedule());
        assertEquals("1정 · 1회 · 저녁", recognized.get(1).formatSchedule());
        assertEquals("0.5정", recognized.get(2).formatDose());
    }
}
