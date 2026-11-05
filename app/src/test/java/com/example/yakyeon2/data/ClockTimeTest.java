package com.example.yakyeon2.data;

import org.junit.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ClockTimeTest {

    @Test
    public void twelveHourLabelsUseMinutesNotThePrintedHour() {
        assertEquals(0, ClockTime.of12(false, 12, 0));
        assertEquals(12 * 60, ClockTime.of12(true, 12, 0));
        assertEquals(8 * 60, ClockTime.of12(false, 8, 0));
        assertEquals(13 * 60 + 5, ClockTime.of12(true, 1, 5));
        assertEquals(23 * 60 + 59, ClockTime.of12(true, 11, 59));

        assertEquals("오전 12:00", ClockTime.format12(0));
        assertEquals("오후 12:00", ClockTime.format12(12 * 60));
        assertEquals("오전 8:00", ClockTime.format12(8 * 60));
        assertEquals("오후 1:05", ClockTime.format12(13 * 60 + 5));
        assertEquals("오후 7:10", ClockTime.format12(19 * 60 + 10));
    }

    @Test
    public void wheelStepsKeepHourAndMinuteIndependent() {
        int at = ClockTime.of24(8, 0);
        assertEquals(ClockTime.of24(9, 0), ClockTime.shiftHour(at, 1));
        assertEquals(ClockTime.of24(7, 0), ClockTime.shiftHour(at, -1));
        assertEquals(ClockTime.of24(0, 10), ClockTime.shiftHour(ClockTime.of24(23, 10), 1));
        assertEquals(ClockTime.of24(8, 1), ClockTime.shiftMinute(at, 1));
        assertEquals(ClockTime.of24(8, 59), ClockTime.shiftMinute(at, -1));
        assertEquals(ClockTime.of24(8, 0), ClockTime.shiftMinute(ClockTime.of24(8, 59), 1));
    }

    @Test
    public void changingAWheelValueUpdatesTheFourHourCheck() {
        Map<DoseRound, Integer> minutes = new EnumMap<>(DoseRound.class);
        minutes.put(DoseRound.MORNING, ClockTime.of12(false, 8, 0));
        minutes.put(DoseRound.LUNCH, ClockTime.of12(true, 12, 0));
        minutes.put(DoseRound.EVENING, ClockTime.of12(true, 6, 0));
        EnumSet<DoseRound> meals = EnumSet.of(DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING);

        assertEquals(MedicationRules.TimeCheck.OK, MedicationRules.checkAlarmTimes(meals, minutes));

        minutes.put(DoseRound.LUNCH, ClockTime.shiftHour(minutes.get(DoseRound.LUNCH), -1));
        assertEquals(11 * 60, minutes.get(DoseRound.LUNCH).intValue());
        assertEquals(MedicationRules.TimeCheck.TOO_CLOSE, MedicationRules.checkAlarmTimes(meals, minutes));

        minutes.put(DoseRound.LUNCH, ClockTime.shiftHour(minutes.get(DoseRound.LUNCH), 1));
        assertEquals(MedicationRules.TimeCheck.OK, MedicationRules.checkAlarmTimes(meals, minutes));
    }

    @Test
    public void fourHourGapFollowsSlotOrderAndIncludesBedtime() {
        Map<DoseRound, Integer> minutes = new EnumMap<>(DoseRound.class);
        minutes.put(DoseRound.MORNING, ClockTime.of24(8, 0));
        minutes.put(DoseRound.LUNCH, ClockTime.of24(12, 0));
        assertEquals(
                MedicationRules.TimeCheck.OK,
                MedicationRules.checkAlarmTimes(EnumSet.of(DoseRound.MORNING, DoseRound.LUNCH), minutes)
        );

        minutes.put(DoseRound.LUNCH, ClockTime.of24(11, 0));
        assertEquals(
                MedicationRules.TimeCheck.TOO_CLOSE,
                MedicationRules.checkAlarmTimes(EnumSet.of(DoseRound.MORNING, DoseRound.LUNCH), minutes)
        );

        minutes.put(DoseRound.EVENING, ClockTime.of24(20, 0));
        minutes.put(DoseRound.MORNING, ClockTime.of24(8, 0));
        assertEquals(
                MedicationRules.TimeCheck.OK,
                MedicationRules.checkAlarmTimes(EnumSet.of(DoseRound.MORNING, DoseRound.EVENING), minutes)
        );

        minutes.put(DoseRound.EVENING, ClockTime.of24(21, 0));
        assertEquals(
                MedicationRules.TimeCheck.TOO_CLOSE,
                MedicationRules.checkAlarmTimes(EnumSet.of(DoseRound.MORNING, DoseRound.EVENING), minutes)
        );

        minutes.put(DoseRound.MORNING, ClockTime.of24(0, 0));
        minutes.put(DoseRound.LUNCH, ClockTime.of24(20, 0));
        assertEquals(
                MedicationRules.TimeCheck.OK,
                MedicationRules.checkAlarmTimes(EnumSet.of(DoseRound.MORNING, DoseRound.LUNCH), minutes)
        );

        minutes.put(DoseRound.MORNING, ClockTime.of24(8, 0));
        minutes.put(DoseRound.BEDTIME, ClockTime.of24(8, 10));
        assertEquals(
                MedicationRules.TimeCheck.TOO_CLOSE,
                MedicationRules.checkAlarmTimes(EnumSet.of(DoseRound.MORNING, DoseRound.BEDTIME), minutes)
        );
        minutes.put(DoseRound.BEDTIME, ClockTime.of24(22, 0));
        assertEquals(
                MedicationRules.TimeCheck.OK,
                MedicationRules.checkAlarmTimes(EnumSet.of(DoseRound.MORNING, DoseRound.BEDTIME), minutes)
        );
        minutes.put(DoseRound.BEDTIME, ClockTime.of24(23, 59));
        assertEquals(
                MedicationRules.TimeCheck.OK,
                MedicationRules.checkAlarmTimes(EnumSet.of(DoseRound.BEDTIME), minutes)
        );
    }
}
