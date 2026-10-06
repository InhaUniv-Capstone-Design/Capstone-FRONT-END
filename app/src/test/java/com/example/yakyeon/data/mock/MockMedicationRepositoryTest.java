package com.example.yakyeon.data.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.yakyeon.data.model.Medication;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;

public class MockMedicationRepositoryTest {

    @Test
    public void updateMedicationChangesSavedFields() {
        MockMedicationRepository repository = new MockMedicationRepository();
        Medication original = repository.findById("metformin");

        assertTrue(repository.updateMedication(
                "metformin",
                "변경된 메트포르민",
                2,
                "정",
                "1회",
                "09:00",
                "식전",
                "14일"
        ));

        Medication updated = repository.findById("metformin");
        assertEquals("변경된 메트포르민", updated.getName());
        assertEquals(2, updated.getDoseAmount(), 0);
        assertEquals("정", updated.getDoseUnit());
        assertEquals("2정", updated.formatDose());
        assertEquals("1회", updated.getTimesPerDay());
        assertEquals("09:00", updated.getDoseTimes());
        assertEquals("식전", updated.getDoseTiming());
        assertEquals("14일", updated.getDuration());
        assertEquals("09:00 · 1회 · 식전 · 2정", updated.getFrequency());
        assertEquals(original.getId(), updated.getId());
        assertEquals(3, repository.countActive());
    }

    @Test
    public void formatDoseSeparatesAmountAndUnit() {
        MockMedicationRepository repository = new MockMedicationRepository();

        assertEquals(0.5, dose(repository, 0.5, "정").getDoseAmount(), 0);
        assertEquals("0.5정", dose(repository, 0.5, "정").formatDose());
        assertEquals("1정", dose(repository, 1, "정").formatDose());
        assertEquals("1캡슐", dose(repository, 1, "캡슐").formatDose());
    }

    @Test
    public void periodEndingTodayStaysActiveAndPreviousDayCompletesWithoutDeleting() {
        MockMedicationRepository repository = new MockMedicationRepository();

        assertTrue(repository.updateMedication(
                "metformin",
                "메트포르민정 500mg",
                1,
                "정",
                "2회",
                "08:00",
                "식후 30분",
                period(0, 0)
        ));
        repository.updateExpiredMedications();
        Medication stillActive = repository.findById("metformin");
        assertEquals(Medication.Status.ACTIVE, stillActive.getStatus());
        assertEquals(Medication.Classification.ACTIVE, stillActive.getClassification());

        assertTrue(repository.updateMedication(
                "amlodipine",
                "암로디핀정 5mg",
                0.5,
                "정",
                "1회",
                "08:00",
                "아침",
                period(-1, -1)
        ));
        repository.updateExpiredMedications();

        Medication completed = repository.findById("amlodipine");
        assertEquals(Medication.Status.COMPLETED, completed.getStatus());
        assertEquals(Medication.Classification.ENDED, completed.getClassification());
        assertEquals("0.5정", completed.formatDose());
        assertEquals(2, repository.getActiveMedications().size());
        assertEquals(2, repository.getEndedMedications().size());
        assertEquals(4, repository.getMedications().size());
    }

    @Test
    public void deleteMedicationRemovesTheItem() {
        MockMedicationRepository repository = new MockMedicationRepository();

        assertTrue(repository.deleteMedication("amlodipine"));

        assertNull(repository.findById("amlodipine"));
        assertEquals(2, repository.countActive());
        assertFalse(repository.deleteMedication("amlodipine"));
    }

    @Test
    public void endMedicationMovesTheItemOutOfTheActiveList() {
        MockMedicationRepository repository = new MockMedicationRepository();

        assertTrue(repository.endMedication("atorvastatin"));

        Medication ended = repository.findById("atorvastatin");
        assertEquals(Medication.Classification.ENDED, ended.getClassification());
        assertEquals(Medication.Status.COMPLETED, ended.getStatus());
        assertTrue(ended.getFrequency().startsWith("복용 종료 · "));
        assertEquals(2, repository.countActive());
        assertEquals(2, repository.getEndedMedications().size());
        assertFalse(repository.endMedication("atorvastatin"));
    }

    private static Medication dose(MockMedicationRepository repository, double amount, String unit) {
        assertTrue(repository.updateMedication(
                "metformin",
                "메트포르민정 500mg",
                amount,
                unit,
                "2회",
                "08:00",
                "식후 30분",
                "30일"
        ));
        return repository.findById("metformin");
    }

    private static String period(int startOffsetDays, int endOffsetDays) {
        return format(offset(startOffsetDays)) + " ~ " + format(offset(endOffsetDays));
    }

    private static Calendar offset(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar;
    }

    private static String format(Calendar calendar) {
        return String.format(
                Locale.KOREA,
                "%04d.%02d.%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }
}
