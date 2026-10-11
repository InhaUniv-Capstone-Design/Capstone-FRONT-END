package com.example.yakyeon.data.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.yakyeon.data.model.ReminderDay;
import com.example.yakyeon.data.model.ReminderRecord;

import org.junit.Test;

import java.util.List;

public class MockMedicationReminderRepositoryTest {

    @Test
    public void eachRecordKeepsItsOwnDoseAndExistingStatus() {
        MockMedicationReminderRepository reminders = new MockMedicationReminderRepository();
        ReminderRecord morning = reminders.find("2026-09-25-0805");
        ReminderRecord noon = reminders.find("2026-09-25-1210");
        ReminderRecord delayed = reminders.find("2026-09-25-1845");

        assertNotNull(morning);
        assertEquals("아스피린 100mg", morning.getMedicationName());
        assertEquals("오전 8:05", reminders.alertTimeLabel(morning));
        assertEquals("오늘 아침 약물 리스트", reminders.checklistTitle(morning));
        assertEquals(ReminderRecord.STATUS_ON_TIME, morning.getStatus());
        assertTrue(morning.isCompleted());
        assertFalse(morning.isUserCompleted());

        assertEquals("메트포르민 500mg", noon.getMedicationName());
        assertEquals("오후 12:10", reminders.alertTimeLabel(noon));
        assertEquals("오늘 낮 약물 리스트", reminders.checklistTitle(noon));

        assertEquals("와파린 2mg", delayed.getMedicationName());
        assertEquals("오후 6:45", reminders.alertTimeLabel(delayed));
        assertEquals("오늘 저녁 약물 리스트", reminders.checklistTitle(delayed));
        assertEquals(ReminderRecord.STATUS_DELAYED, delayed.getStatus());
        assertFalse(delayed.isCompleted());
        assertNull(reminders.find("missing"));
    }

    @Test
    public void userCompletionChangesOnlyTheSelectedDoseToStarredNormal() {
        MockMedicationReminderRepository reminders = new MockMedicationReminderRepository();

        assertTrue(reminders.markUserCompleted("2026-09-25-1845"));
        ReminderRecord completed = reminders.find("2026-09-25-1845");
        assertEquals(ReminderRecord.STATUS_USER_COMPLETED, completed.getStatus());
        assertTrue(completed.isUserCompleted());
        assertEquals("와파린 2mg", completed.getMedicationName());
        assertEquals("18:45", completed.getClock());

        assertEquals(ReminderRecord.STATUS_ON_TIME, reminders.find("2026-09-25-1210").getStatus());
        assertEquals(ReminderRecord.STATUS_DELAYED, reminders.find("2026-09-23-0840").getStatus());
        assertEquals(ReminderRecord.STATUS_MISSED, reminders.find("2026-09-24-1200").getStatus());
        assertEquals(12, count(reminders.days(ReminderRecord.FILTER_ALL)));

        assertTrue(reminders.markUserCompleted("2026-09-25-1845"));
        assertEquals(ReminderRecord.STATUS_USER_COMPLETED, reminders.find("2026-09-25-1845").getStatus());
        assertEquals(12, count(reminders.days(ReminderRecord.FILTER_ALL)));
    }

    @Test
    public void existingNormalRecordStaysNormalWhenCompletedAgain() {
        MockMedicationReminderRepository reminders = new MockMedicationReminderRepository();

        assertTrue(reminders.markUserCompleted("2026-09-25-1210"));
        ReminderRecord noon = reminders.find("2026-09-25-1210");
        assertEquals(ReminderRecord.STATUS_ON_TIME, noon.getStatus());
        assertFalse(noon.isUserCompleted());
        assertTrue(noon.isCompleted());
    }

    @Test
    public void normalFilterIncludesRecordedAndUserCompletedDoses() {
        MockMedicationReminderRepository reminders = new MockMedicationReminderRepository();
        reminders.markUserCompleted("2026-09-24-1200");

        List<ReminderDay> missed = reminders.days(ReminderRecord.STATUS_MISSED);
        assertEquals(1, missed.size());
        assertEquals("19:00", missed.get(0).getRecords().get(0).getClock());

        boolean foundUserCompleted = false;
        for (ReminderDay day : reminders.days(ReminderRecord.STATUS_ON_TIME)) {
            for (ReminderRecord record : day.getRecords()) {
                assertTrue(record.isCompleted());
                if ("2026-09-24-1200".equals(record.getId())) {
                    foundUserCompleted = true;
                    assertEquals(ReminderRecord.STATUS_USER_COMPLETED, record.getStatus());
                }
            }
        }
        assertTrue(foundUserCompleted);
    }

    private static int count(List<ReminderDay> days) {
        int count = 0;
        for (ReminderDay day : days) {
            count += day.getRecords().size();
        }
        return count;
    }
}
