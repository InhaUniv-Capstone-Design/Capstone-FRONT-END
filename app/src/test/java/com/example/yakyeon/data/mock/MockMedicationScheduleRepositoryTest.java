package com.example.yakyeon.data.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.yakyeon.data.model.ScheduleDose;

import org.junit.Test;

import java.util.Calendar;
import java.util.List;

public class MockMedicationScheduleRepositoryTest {

    @Test
    public void activeMedicationsExpandIntoSeparateDoseTimes() {
        MockMedicationScheduleRepository schedule =
                new MockMedicationScheduleRepository(new MockMedicationRepository());
        List<ScheduleDose> today = schedule.dosesOn(Calendar.getInstance());

        assertEquals(4, today.size());
        assertEquals("08:00", today.get(0).getClock());
        assertEquals("메트포르민정 500mg", today.get(0).getMedicationName());
        assertEquals("1정", today.get(0).getDoseLabel());
        assertEquals("08:00", today.get(1).getClock());
        assertEquals("암로디핀정 5mg", today.get(1).getMedicationName());
        assertEquals("19:00", today.get(2).getClock());
        assertEquals("메트포르민정 500mg", today.get(2).getMedicationName());
        assertEquals("19:00", today.get(3).getClock());
        assertEquals("아토르바스타틴정 10mg", today.get(3).getMedicationName());
        assertTrue(today.get(0).getStatus().equals(ScheduleDose.DONE)
                || today.get(0).getStatus().equals(ScheduleDose.PLANNED));
    }

    @Test
    public void anotherDateKeepsTheSameDosesWithItsOwnStatus() {
        MockMedicationScheduleRepository schedule =
                new MockMedicationScheduleRepository(new MockMedicationRepository());
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        List<ScheduleDose> past = schedule.dosesOn(yesterday);
        List<ScheduleDose> future = schedule.dosesOn(tomorrow);
        assertEquals(4, past.size());
        assertEquals(4, future.size());
        assertEquals(ScheduleDose.PLANNED, future.get(0).getStatus());
        assertTrue(past.get(0).getStatus().equals(ScheduleDose.DONE)
                || past.get(0).getStatus().equals(ScheduleDose.MISSED));
    }
}
