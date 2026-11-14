package com.example.yakyeon2.data;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MedicationRulesTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 8);

    @Test
    public void mealTimesMustBeAtLeastFourHoursIncludingBedtime() {
        Map<DoseRound, Integer> minutes = new EnumMap<>(DoseRound.class);
        minutes.put(DoseRound.MORNING, 8 * 60);
        minutes.put(DoseRound.LUNCH, 12 * 60);
        minutes.put(DoseRound.EVENING, 18 * 60);
        minutes.put(DoseRound.BEDTIME, 22 * 60);

        assertEquals(
                MedicationRules.TimeCheck.OK,
                MedicationRules.checkAlarmTimes(EnumSet.allOf(DoseRound.class), minutes)
        );

        minutes.put(DoseRound.LUNCH, 11 * 60);
        assertEquals(
                MedicationRules.TimeCheck.TOO_CLOSE,
                MedicationRules.checkAlarmTimes(EnumSet.of(DoseRound.MORNING, DoseRound.LUNCH), minutes)
        );

        minutes.remove(DoseRound.LUNCH);
        assertEquals(
                MedicationRules.TimeCheck.MISSING,
                MedicationRules.checkAlarmTimes(EnumSet.of(DoseRound.MORNING, DoseRound.LUNCH), minutes)
        );
    }

    @Test
    public void missedRecordCanBeDelayedOnlyInsideThreeHours() {
        long scheduled = DAY.atStartOfDay(ZONE).plusHours(8).toInstant().toEpochMilli();
        DoseRecord record = sample(scheduled);

        assertEquals(DoseStatus.SCHEDULED, MedicationRules.displayStatus(record, scheduled - 1));
        assertEquals(MedicationRules.ReviseBlock.NOT_YET, MedicationRules.reviseBlock(record, scheduled - 1));
        assertFalse(MedicationRules.canRevise(record, scheduled - 1));
        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(record, scheduled));
        assertEquals(MedicationRules.ReviseBlock.ALLOWED, MedicationRules.reviseBlock(record, scheduled));

        long inside = scheduled + MedicationRules.THREE_HOURS_MS;
        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(record, inside));
        assertTrue(MedicationRules.recordManual(record, inside, inside));
        assertEquals(DoseStatus.DELAYED, record.getManualStatus());
        assertEquals(Long.valueOf(inside), record.getTakenAtMillis());
        assertFalse(MedicationRules.canRevise(record, inside));
        assertEquals(MedicationRules.ReviseBlock.MANUAL, MedicationRules.reviseBlock(record, inside));

        DoseRecord expired = sample(scheduled);
        long after = scheduled + MedicationRules.THREE_HOURS_MS + 1;
        assertFalse(MedicationRules.recordManual(expired, after, after));
        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(expired, after));
        assertEquals(MedicationRules.ReviseBlock.TOO_LATE, MedicationRules.reviseBlock(expired, after));
        assertFalse(MedicationRules.canRevise(expired, after));
    }

    @Test
    public void openingAPillboxDoesNotMarkTheDoseTaken() {
        long scheduled = DAY.atStartOfDay(ZONE).plusHours(8).toInstant().toEpochMilli();
        DoseRecord record = sample(scheduled);

        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(record, scheduled + 60_000L));
        assertEquals(null, record.getManualStatus());
    }

    @Test
    public void hardwareEarlyIntakeCompletesTheNextRoundAndKeepsItsTime() {
        LocalDate day = LocalDate.of(2026, 10, 8);
        long registeredAt = day.atStartOfDay(ZONE).toInstant().toEpochMilli();
        MedicationStore store = new MedicationStore(new YakyeonSession());
        EnvelopeDraft draft = store.beginDraft();
        MedicineDraft medicine = store.addMedicine(day);
        medicine.setName("name-for-test");
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.rounds().add(DoseRound.LUNCH);
        medicine.rounds().add(DoseRound.EVENING);
        medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        medicine.setEnd(day);
        medicine.setConfirmed(true);
        draft.alarmMinutes().put(DoseRound.MORNING, ClockTime.of12(false, 8, 0));
        draft.alarmMinutes().put(DoseRound.LUNCH, ClockTime.of12(true, 12, 0));
        draft.alarmMinutes().put(DoseRound.EVENING, ClockTime.of12(true, 6, 0));
        draft.setSlotIndex(0);
        assertEquals(MedicationRules.SaveBlock.OK, store.commit(ZONE, registeredAt));

        DoseRecord morning = find(store.records(), day, DoseRound.MORNING);
        long early = morning.getScheduledAtMillis() - 30L * 60L * 1000L;
        assertEquals(1, store.recordEarlyHardwareIntake(0, early));
        assertEquals(DoseStatus.COMPLETED, morning.getManualStatus());
        assertEquals(Long.valueOf(early), morning.getTakenAtMillis());
        assertEquals(DoseStatus.COMPLETED, MedicationRules.displayStatus(morning, early));
        assertEquals(null, find(store.records(), day, DoseRound.LUNCH).getManualStatus());

        DoseRecord lunch = find(store.records(), day, DoseRound.LUNCH);
        long beforeLunch = lunch.getScheduledAtMillis() - 60_000L;
        assertEquals(1, store.recordEarlyHardwareIntake(0, beforeLunch));
        assertEquals(DoseStatus.COMPLETED, lunch.getManualStatus());
        assertEquals(Long.valueOf(beforeLunch), lunch.getTakenAtMillis());

        DoseRecord evening = find(store.records(), day, DoseRound.EVENING);
        assertEquals(0, store.recordEarlyHardwareIntake(0, evening.getScheduledAtMillis()));
        assertEquals(null, evening.getManualStatus());
        assertEquals(0, store.recordEarlyHardwareIntake(1, early));
        assertFalse(MedicationRules.recordHardwareEarly(evening, evening.getScheduledAtMillis()));
    }

    @Test
    public void historyFiltersSeparateEmptyDataFromEmptyFilter() {
        long scheduled = DAY.atStartOfDay(ZONE).plusHours(8).toInstant().toEpochMilli();
        DoseRecord missed = sample(scheduled);
        long now = scheduled + 60_000L;
        List<DoseRecord> records = java.util.Collections.singletonList(missed);

        MedicationRules.HistoryPage all = MedicationRules.history(
                records,
                HistoryFilter.ALL,
                scheduled,
                scheduled + 1,
                now
        );
        assertTrue(all.hasRecords);
        assertEquals(1, all.visible.size());

        MedicationRules.HistoryPage completed = MedicationRules.history(
                records,
                HistoryFilter.COMPLETED,
                scheduled,
                scheduled + 1,
                now
        );
        assertTrue(completed.hasRecords);
        assertTrue(completed.visible.isEmpty());

        MedicationRules.HistoryPage none = MedicationRules.history(
                records,
                HistoryFilter.ALL,
                scheduled + 10_000L,
                scheduled + 20_000L,
                now
        );
        assertFalse(none.hasRecords);
        assertTrue(none.visible.isEmpty());
    }

    @Test
    public void historyRangeDefaultsToFourteenDaysAndRejectsMoreThanOneYear() {
        LocalDate today = LocalDate.of(2026, 10, 25);
        assertEquals(LocalDate.of(2026, 10, 12), MedicationRules.defaultHistoryStart(today));
        assertTrue(MedicationRules.rangeAllowed(today.minusDays(364), today));
        assertFalse(MedicationRules.rangeAllowed(today.minusDays(365), today));
    }

    @Test
    public void unnamedDraftIsNotStored() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        EnvelopeDraft draft = store.beginDraft();
        MedicineDraft medicine = store.addMedicine(DAY);
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        medicine.setEnd(DAY);
        medicine.setConfirmed(true);
        draft.alarmMinutes().put(DoseRound.MORNING, 8 * 60);

        assertEquals(MedicationRules.SaveBlock.MISSING_REQUIRED, store.commit(ZONE));
        assertTrue(store.records().isEmpty());
        assertTrue(store.envelopes().isEmpty());
    }

    @Test
    public void validDraftCommitsLocallyWithoutMarkingDosesTaken() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        EnvelopeDraft draft = store.beginDraft();
        MedicineDraft medicine = store.addMedicine(DAY);
        medicine.setName("name-for-test");
        medicine.setAmount(DoseAmount.CAPSULE_1);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.rounds().add(DoseRound.BEDTIME);
        medicine.timings().add(DoseTiming.AFTER_MEAL_30);
        medicine.timings().add(DoseTiming.BEDTIME);
        medicine.setEnd(DAY);
        medicine.setConfirmed(true);
        draft.alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        draft.alarmMinutes().put(DoseRound.BEDTIME, 22 * 60);
        draft.setSlotIndex(0);

        assertEquals(MedicationRules.SaveBlock.OK, store.commit(ZONE));
        assertEquals(1, store.activeEnvelopeCount());
        assertEquals(1, store.activeMedicineCount());
        assertEquals(2, store.records().size());
        assertTrue(store.records().get(0).getManualStatus() == null);
        YakyeonSession session = new YakyeonSession();
        MedicationStore linked = new MedicationStore(session);
        EnvelopeDraft again = linked.beginDraft();
        MedicineDraft copy = linked.addMedicine(DAY);
        copy.setName("name-for-test");
        copy.setAmount(DoseAmount.TABLET_HALF);
        copy.rounds().add(DoseRound.EVENING);
        copy.setTiming(DoseTiming.BEFORE_MEAL);
        copy.setEnd(DAY);
        copy.setConfirmed(true);
        again.alarmMinutes().put(DoseRound.EVENING, 18 * 60);
        again.setSlotIndex(2);
        assertEquals(MedicationRules.SaveBlock.OK, linked.commit(ZONE));
        assertTrue(session.isSlotOccupied(2));
        assertEquals(1, session.medicationIds().size());
        assertFalse(session.isPillboxConnected());
    }

    @Test
    public void futureDosesStayScheduledAndDoNotAppearAsHistory() {
        long now = DAY.atStartOfDay(ZONE).plusHours(7).toInstant().toEpochMilli();
        long scheduled = DAY.atStartOfDay(ZONE).plusHours(8).toInstant().toEpochMilli();
        DoseRecord record = sample(scheduled);

        assertEquals(DoseStatus.SCHEDULED, MedicationRules.displayStatus(record, now));
        assertEquals(1, MedicationRules.remainingToday(java.util.Collections.singletonList(record), now, ZONE).size());
        MedicationRules.HistoryPage page = MedicationRules.history(
                java.util.Collections.singletonList(record),
                HistoryFilter.ALL,
                DAY.atStartOfDay(ZONE).toInstant().toEpochMilli(),
                DAY.plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli(),
                now
        );
        assertFalse(page.hasRecords);
    }

    @Test
    public void weekStartsMondayAtLocalMidnightWithoutClearingRecords() {
        LocalDate sunday = LocalDate.of(2026, 10, 25);
        LocalDate monday = LocalDate.of(2026, 10, 26);
        long justBefore = sunday.atTime(23, 59).atZone(ZONE).toInstant().toEpochMilli();
        long atMonday = monday.atStartOfDay(ZONE).toInstant().toEpochMilli();

        assertEquals(LocalDate.of(2026, 10, 19), MedicationRules.weekStart(LocalDate.ofInstant(java.time.Instant.ofEpochMilli(justBefore), ZONE)));
        assertEquals(monday, MedicationRules.weekStart(LocalDate.ofInstant(java.time.Instant.ofEpochMilli(atMonday), ZONE)));
        assertFalse(MedicationRules.sameWeek(sunday, monday));

        long registeredAt = sunday.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();
        MedicationStore store = committedStore(sunday, monday, registeredAt);
        int before = store.records().size();
        store.setScheduleDate(sunday);
        assertEquals(monday, store.scheduleDate(monday));
        assertEquals(before, store.records().size());
        assertTrue(before > 0);
    }

    @Test
    public void registrationDaySkipsEarlierRoundsAndKeepsLaterOnesScheduled() {
        LocalDate day = LocalDate.of(2026, 10, 8);
        long registeredAt = day.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();
        MedicationStore store = committedStore(day, day.plusDays(1), registeredAt);
        List<DoseRecord> records = store.records();

        assertEquals(null, find(records, day, DoseRound.MORNING));
        DoseRecord noon = find(records, day, DoseRound.LUNCH);
        DoseRecord evening = find(records, day, DoseRound.EVENING);
        assertEquals(DoseStatus.SCHEDULED, MedicationRules.displayStatus(noon, registeredAt));
        assertEquals(DoseStatus.SCHEDULED, MedicationRules.displayStatus(evening, registeredAt));
        assertEquals(ClockTime.of12(true, 12, 0), minutesOf(noon));
        assertEquals(ClockTime.of12(true, 6, 0), minutesOf(evening));

        DoseRecord nextMorning = find(records, day.plusDays(1), DoseRound.MORNING);
        assertEquals(DoseStatus.SCHEDULED, MedicationRules.displayStatus(nextMorning, registeredAt));
        assertEquals(8 * 60, minutesOf(nextMorning));

        long twoHoursAfterNoon = noon.getScheduledAtMillis() + 2L * 60L * 60L * 1000L;
        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(noon, twoHoursAfterNoon));
        assertTrue(MedicationRules.canRevise(noon, twoHoursAfterNoon));
        long afterThreeHours = noon.getScheduledAtMillis() + MedicationRules.THREE_HOURS_MS + 1;
        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(noon, afterThreeHours));
        assertFalse(MedicationRules.canRevise(noon, afterThreeHours));

        int size = records.size();
        store.mergeExpanded(ZONE);
        assertEquals(size, store.records().size());
    }

    @Test
    public void equalRegistrationAndScheduledTimeIsKeptAndBecomesMissedAtThatInstant() {
        LocalDate day = LocalDate.of(2026, 10, 8);
        long noon = day.atTime(12, 0).atZone(ZONE).toInstant().toEpochMilli();
        MedicationStore store = committedStore(day, day, noon);
        DoseRecord record = find(store.records(), day, DoseRound.LUNCH);
        assertEquals(noon, record.getScheduledAtMillis());
        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(record, noon));
        assertTrue(MedicationRules.canRevise(record, noon));
        assertEquals(DoseStatus.SCHEDULED, MedicationRules.displayStatus(record, noon - 1));
    }

    @Test
    public void restartKeepsSavedRoundsAndManualStatus() {
        LocalDate day = LocalDate.of(2026, 10, 8);
        long registeredAt = day.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();
        MedicationStore store = committedStore(day, day.plusDays(1), registeredAt);
        DoseRecord noon = find(store.records(), day, DoseRound.LUNCH);
        long taken = noon.getScheduledAtMillis() + 60_000L;
        assertTrue(store.recordManual(noon, noon.getScheduledAtMillis() + 1_000L, taken));

        MedicationStore restarted = new MedicationStore(new YakyeonSession());
        restarted.importState(store.exportState());
        assertEquals(store.records().size(), restarted.records().size());
        assertEquals(null, find(restarted.records(), day, DoseRound.MORNING));
        DoseRecord restored = find(restarted.records(), day, DoseRound.LUNCH);
        assertEquals(DoseStatus.DELAYED, restored.getManualStatus());
        assertEquals(Long.valueOf(taken), restored.getTakenAtMillis());
        assertEquals(store.records().size(), restarted.records().size());
        restarted.mergeExpanded(ZONE);
        assertEquals(store.records().size(), restarted.records().size());
    }

    @Test
    public void updatingAlarmMinutesRewritesOpenRoundsAndKeepsManualOnes() {
        LocalDate day = LocalDate.of(2026, 10, 8);
        long registeredAt = day.atStartOfDay(ZONE).toInstant().toEpochMilli();
        MedicationStore store = committedStore(day, day, registeredAt);
        SavedEnvelope envelope = store.envelopes().get(0);
        assertEquals(Integer.valueOf(8 * 60), envelope.alarmMinutes().get(DoseRound.MORNING));
        assertEquals(Integer.valueOf(12 * 60), envelope.alarmMinutes().get(DoseRound.LUNCH));
        assertEquals(Integer.valueOf(18 * 60), envelope.alarmMinutes().get(DoseRound.EVENING));

        DoseRecord lunch = find(store.records(), day, DoseRound.LUNCH);
        assertTrue(store.recordManual(lunch, lunch.getScheduledAtMillis() + 1_000L, lunch.getScheduledAtMillis() + 60_000L));
        long delayedAt = lunch.getScheduledAtMillis();

        long now = day.atTime(17, 0).atZone(ZONE).toInstant().toEpochMilli();
        Map<DoseRound, Integer> tooClose = new EnumMap<>(DoseRound.class);
        tooClose.put(DoseRound.MORNING, 8 * 60);
        tooClose.put(DoseRound.LUNCH, 10 * 60);
        tooClose.put(DoseRound.EVENING, 18 * 60);
        assertFalse(store.updateAlarmMinutes(envelope.getId(), tooClose, ZONE, now));
        assertEquals(Integer.valueOf(12 * 60), envelope.alarmMinutes().get(DoseRound.LUNCH));

        Map<DoseRound, Integer> next = new EnumMap<>(DoseRound.class);
        next.put(DoseRound.MORNING, ClockTime.of12(false, 8, 0));
        next.put(DoseRound.LUNCH, ClockTime.of12(true, 12, 0));
        next.put(DoseRound.EVENING, ClockTime.of24(19, 0));
        assertTrue(store.updateAlarmMinutes(envelope.getId(), next, ZONE, now));

        assertEquals(19 * 60, minutesOf(find(store.records(), day, DoseRound.EVENING)));
        DoseRecord kept = find(store.records(), day, DoseRound.LUNCH);
        assertEquals(delayedAt, kept.getScheduledAtMillis());
        assertEquals(DoseStatus.DELAYED, kept.getManualStatus());
        assertEquals(3, store.records().size());

        MedicationStore restarted = new MedicationStore(new YakyeonSession());
        restarted.importState(store.exportState());
        assertEquals(Integer.valueOf(19 * 60), restarted.envelopes().get(0).alarmMinutes().get(DoseRound.EVENING));
        assertEquals(DoseStatus.DELAYED, find(restarted.records(), day, DoseRound.LUNCH).getManualStatus());
        assertEquals(3, restarted.records().size());
    }

    @Test
    public void savedAlarmEditLoadsStoredMinutesAndCancelKeepsThem() {
        LocalDate day = LocalDate.of(2026, 10, 8);
        long registeredAt = day.atStartOfDay(ZONE).toInstant().toEpochMilli();
        MedicationStore store = committedStore(day, day, registeredAt);
        SavedEnvelope envelope = store.envelopes().get(0);
        assertFalse(store.beginAlarmEdit("missing"));
        assertTrue(store.beginAlarmEdit(envelope.getId()));
        assertEquals(Integer.valueOf(8 * 60), store.alarmEditMinutes().get(DoseRound.MORNING));
        assertEquals(Integer.valueOf(12 * 60), store.alarmEditMinutes().get(DoseRound.LUNCH));
        assertEquals(Integer.valueOf(18 * 60), store.alarmEditMinutes().get(DoseRound.EVENING));
        assertFalse(store.alarmEditMinutes().containsKey(DoseRound.BEDTIME));
        store.alarmEditMinutes().put(DoseRound.EVENING, 19 * 60);
        assertEquals(Integer.valueOf(18 * 60), envelope.alarmMinutes().get(DoseRound.EVENING));
        store.cancelAlarmEdit();
        assertFalse(store.isAlarmEdit());
        assertTrue(store.alarmEditMinutes().isEmpty());
        assertEquals(Integer.valueOf(18 * 60), envelope.alarmMinutes().get(DoseRound.EVENING));
        assertTrue(store.stopEnvelope(envelope.getId(), registeredAt));
        assertFalse(store.beginAlarmEdit(envelope.getId()));
    }

    @Test
    public void savingAlarmEditChangesOnlyThatEnvelope() {
        LocalDate day = LocalDate.of(2026, 10, 8);
        long registeredAt = day.atStartOfDay(ZONE).toInstant().toEpochMilli();
        MedicationStore store = committedStore(day, day, registeredAt);
        SavedEnvelope first = store.envelopes().get(0);
        commitEnvelope(store, day, day, registeredAt, "second", 9 * 60, 14 * 60, 20 * 60);
        SavedEnvelope second = store.envelopes().get(1);
        long now = day.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();

        assertTrue(store.beginAlarmEdit(first.getId()));
        store.alarmEditMinutes().put(DoseRound.EVENING, 19 * 60);
        assertTrue(store.saveAlarmEdit(ZONE, now));

        assertEquals(Integer.valueOf(19 * 60), first.alarmMinutes().get(DoseRound.EVENING));
        assertEquals(19 * 60, minutesOf(findIn(store.records(), first.getId(), day, DoseRound.EVENING)));
        assertEquals(Integer.valueOf(20 * 60), second.alarmMinutes().get(DoseRound.EVENING));
        assertEquals(20 * 60, minutesOf(findIn(store.records(), second.getId(), day, DoseRound.EVENING)));
        assertEquals(Integer.valueOf(9 * 60), second.alarmMinutes().get(DoseRound.MORNING));
        assertFalse(store.isAlarmEdit());
    }

    @Test
    public void alarmEditKeepsFourHourRuleIncludingBedtime() {
        LocalDate day = LocalDate.of(2026, 10, 8);
        long registeredAt = day.atStartOfDay(ZONE).toInstant().toEpochMilli();
        MedicationStore store = committedStore(day, day, registeredAt);
        SavedEnvelope envelope = store.envelopes().get(0);
        envelope.medicines().get(0).rounds().add(DoseRound.BEDTIME);
        long now = day.atTime(7, 0).atZone(ZONE).toInstant().toEpochMilli();

        assertTrue(store.beginAlarmEdit(envelope.getId()));
        store.alarmEditMinutes().put(DoseRound.MORNING, 8 * 60);
        store.alarmEditMinutes().put(DoseRound.LUNCH, 12 * 60);
        store.alarmEditMinutes().put(DoseRound.EVENING, 16 * 60);
        store.alarmEditMinutes().put(DoseRound.BEDTIME, 20 * 60);
        assertTrue(store.saveAlarmEdit(ZONE, now));
        assertEquals(Integer.valueOf(12 * 60), envelope.alarmMinutes().get(DoseRound.LUNCH));
        assertEquals(Integer.valueOf(20 * 60), envelope.alarmMinutes().get(DoseRound.BEDTIME));

        assertTrue(store.beginAlarmEdit(envelope.getId()));
        store.alarmEditMinutes().put(DoseRound.LUNCH, 11 * 60 + 59);
        assertFalse(store.saveAlarmEdit(ZONE, now));
        assertTrue(store.isAlarmEdit());
        assertEquals(Integer.valueOf(12 * 60), envelope.alarmMinutes().get(DoseRound.LUNCH));
        assertEquals(12 * 60, minutesOf(find(store.records(), day, DoseRound.LUNCH)));
        store.cancelAlarmEdit();
        assertEquals(Integer.valueOf(12 * 60), envelope.alarmMinutes().get(DoseRound.LUNCH));
    }

    @Test
    public void savedAlarmEditUpdatesOpenScheduleAndHomeAndKeepsRecordedRounds() {
        LocalDate day = LocalDate.of(2026, 10, 8);
        LocalDate next = day.plusDays(1);
        long registeredAt = day.atStartOfDay(ZONE).toInstant().toEpochMilli();
        MedicationStore store = committedStore(day, next, registeredAt);
        SavedEnvelope envelope = store.envelopes().get(0);
        long now = day.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();

        DoseRecord missed = find(store.records(), day, DoseRound.MORNING);
        long missedAt = missed.getScheduledAtMillis();
        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(missed, now));

        DoseRecord lunch = find(store.records(), day, DoseRound.LUNCH);
        long lunchAt = lunch.getScheduledAtMillis();
        long delayedTaken = lunchAt + 60_000L;
        assertTrue(store.recordManual(lunch, lunchAt + 1_000L, delayedTaken));

        DoseRecord completed = find(store.records(), next, DoseRound.MORNING);
        long completedAt = completed.getScheduledAtMillis();
        long completedTaken = completedAt + 5_000L;
        completed.restoreManual(DoseStatus.COMPLETED, completedTaken);

        assertTrue(store.beginAlarmEdit(envelope.getId()));
        store.alarmEditMinutes().put(DoseRound.MORNING, 7 * 60);
        store.alarmEditMinutes().put(DoseRound.EVENING, 19 * 60);
        assertTrue(store.saveAlarmEdit(ZONE, now));

        DoseRecord missedAfter = find(store.records(), day, DoseRound.MORNING);
        assertEquals(missedAt, missedAfter.getScheduledAtMillis());
        assertEquals(null, missedAfter.getManualStatus());
        assertEquals(null, missedAfter.getTakenAtMillis());
        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(missedAfter, now));

        DoseRecord delayedAfter = find(store.records(), day, DoseRound.LUNCH);
        assertEquals(lunchAt, delayedAfter.getScheduledAtMillis());
        assertEquals(DoseStatus.DELAYED, delayedAfter.getManualStatus());
        assertEquals(Long.valueOf(delayedTaken), delayedAfter.getTakenAtMillis());

        DoseRecord completedAfter = find(store.records(), next, DoseRound.MORNING);
        assertEquals(completedAt, completedAfter.getScheduledAtMillis());
        assertEquals(DoseStatus.COMPLETED, completedAfter.getManualStatus());
        assertEquals(Long.valueOf(completedTaken), completedAfter.getTakenAtMillis());

        assertEquals(19 * 60, minutesOf(find(store.records(), day, DoseRound.EVENING)));
        assertEquals(19 * 60, minutesOf(find(store.records(), next, DoseRound.EVENING)));
        assertEquals(19 * 60, minutesOf(find(MedicationRules.onDate(store.records(), day, ZONE), day, DoseRound.EVENING)));

        List<DoseRecord> remaining = MedicationRules.remainingToday(store.records(), now, ZONE);
        assertEquals(1, remaining.size());
        assertEquals(DoseRound.EVENING, remaining.get(0).getRound());
        assertEquals(19 * 60, minutesOf(remaining.get(0)));
        assertEquals(Integer.valueOf(7 * 60), envelope.alarmMinutes().get(DoseRound.MORNING));
    }

    @Test
    public void savedAlarmEditSurvivesExportImport() {
        LocalDate day = LocalDate.of(2026, 10, 8);
        long registeredAt = day.atStartOfDay(ZONE).toInstant().toEpochMilli();
        MedicationStore store = committedStore(day, day, registeredAt);
        SavedEnvelope envelope = store.envelopes().get(0);
        long now = day.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();
        assertTrue(store.beginAlarmEdit(envelope.getId()));
        store.alarmEditMinutes().put(DoseRound.EVENING, 19 * 60);
        assertTrue(store.saveAlarmEdit(ZONE, now));

        MedicationStore restarted = new MedicationStore(new YakyeonSession());
        restarted.importState(store.exportState());
        assertEquals(Integer.valueOf(19 * 60), restarted.envelopes().get(0).alarmMinutes().get(DoseRound.EVENING));
        assertEquals(19 * 60, minutesOf(find(restarted.records(), day, DoseRound.EVENING)));
        assertEquals(Integer.valueOf(8 * 60), restarted.envelopes().get(0).alarmMinutes().get(DoseRound.MORNING));
        assertFalse(restarted.isAlarmEdit());
    }

    @Test
    public void newRegistrationStillCommitsWhenSavedAlarmEditIsCancelled() {
        LocalDate day = LocalDate.of(2026, 10, 8);
        long registeredAt = day.atStartOfDay(ZONE).toInstant().toEpochMilli();
        MedicationStore store = committedStore(day, day, registeredAt);
        SavedEnvelope first = store.envelopes().get(0);
        assertTrue(store.beginAlarmEdit(first.getId()));
        store.alarmEditMinutes().put(DoseRound.EVENING, 19 * 60);

        EnvelopeDraft draft = store.beginDraft();
        draft.setHospitalName("second");
        draft.alarmMinutes().put(DoseRound.MORNING, 9 * 60);
        assertEquals(Integer.valueOf(19 * 60), store.alarmEditMinutes().get(DoseRound.EVENING));
        assertEquals(Integer.valueOf(18 * 60), first.alarmMinutes().get(DoseRound.EVENING));
        assertEquals(Integer.valueOf(9 * 60), draft.alarmMinutes().get(DoseRound.MORNING));

        store.cancelAlarmEdit();
        commitEnvelope(store, day, day, registeredAt, "second", 9 * 60, 14 * 60, 20 * 60);
        assertEquals(2, store.envelopes().size());
        assertEquals(Integer.valueOf(18 * 60), first.alarmMinutes().get(DoseRound.EVENING));
        assertEquals(Integer.valueOf(20 * 60), store.envelopes().get(1).alarmMinutes().get(DoseRound.EVENING));
        assertFalse(store.isAlarmEdit());
        assertEquals(null, store.draft());
    }

    private static void commitEnvelope(
            MedicationStore store,
            LocalDate start,
            LocalDate end,
            long registeredAt,
            String hospital,
            int morning,
            int lunch,
            int evening
    ) {
        EnvelopeDraft draft = store.beginDraft();
        draft.setHospitalName(hospital);
        MedicineDraft medicine = store.addMedicine(start);
        medicine.setName("name-for-test");
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.rounds().add(DoseRound.LUNCH);
        medicine.rounds().add(DoseRound.EVENING);
        medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        medicine.setEnd(end);
        medicine.setConfirmed(true);
        draft.alarmMinutes().put(DoseRound.MORNING, morning);
        draft.alarmMinutes().put(DoseRound.LUNCH, lunch);
        draft.alarmMinutes().put(DoseRound.EVENING, evening);
        assertEquals(MedicationRules.SaveBlock.OK, store.commit(ZONE, registeredAt));
    }

    private static MedicationStore committedStore(LocalDate start, LocalDate end, long registeredAt) {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        EnvelopeDraft draft = store.beginDraft();
        MedicineDraft medicine = store.addMedicine(start);
        medicine.setName("name-for-test");
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.rounds().add(DoseRound.LUNCH);
        medicine.rounds().add(DoseRound.EVENING);
        medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        medicine.setEnd(end);
        medicine.setConfirmed(true);
        draft.alarmMinutes().put(DoseRound.MORNING, ClockTime.of12(false, 8, 0));
        draft.alarmMinutes().put(DoseRound.LUNCH, ClockTime.of12(true, 12, 0));
        draft.alarmMinutes().put(DoseRound.EVENING, ClockTime.of12(true, 6, 0));
        assertEquals(MedicationRules.SaveBlock.OK, store.commit(ZONE, registeredAt));
        return store;
    }

    private static DoseRecord find(List<DoseRecord> records, LocalDate day, DoseRound round) {
        return findIn(records, null, day, round);
    }

    private static DoseRecord findIn(
            List<DoseRecord> records,
            String envelopeId,
            LocalDate day,
            DoseRound round
    ) {
        for (DoseRecord record : records) {
            if (envelopeId != null && !envelopeId.equals(record.getEnvelopeId())) {
                continue;
            }
            java.time.ZonedDateTime time = java.time.Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(ZONE);
            if (time.toLocalDate().equals(day) && record.getRound() == round) {
                return record;
            }
        }
        return null;
    }

    private static int minutesOf(DoseRecord record) {
        java.time.ZonedDateTime time = java.time.Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(ZONE);
        return time.getHour() * 60 + time.getMinute();
    }

    @Test
    public void remainingRowsCollapseToOneEnvelopePerRoundAndKeepSeparateRounds() {
        long morning = DAY.atStartOfDay(ZONE).plusHours(8).toInstant().toEpochMilli();
        long evening = DAY.atStartOfDay(ZONE).plusHours(19).toInstant().toEpochMilli();
        DoseRecord first = new DoseRecord("a", "env", "med-a", "한의원", "약1", DoseRound.MORNING, morning);
        DoseRecord second = new DoseRecord("b", "env", "med-b", "한의원", "약2", DoseRound.MORNING, morning);
        DoseRecord later = new DoseRecord("c", "env", "med-a", "한의원", "약1", DoseRound.EVENING, evening);
        List<DoseRecord> grouped = MedicationRules.onePerEnvelopeRound(java.util.Arrays.asList(first, second, later));
        assertEquals(2, grouped.size());
        assertEquals("med-a", grouped.get(0).getMedicineId());
        assertEquals(DoseRound.EVENING, grouped.get(1).getRound());
        assertEquals(DoseAmount.POUCH_2, DoseAmount.from(DoseAmount.UNIT_POUCH, "2"));
        assertEquals(null, DoseAmount.from(DoseAmount.UNIT_CAPSULE, "0.5"));
        assertEquals(null, DoseAmount.from(DoseAmount.UNIT_TABLET, "0"));
        assertEquals("2", DoseAmount.POUCH_2.quantityText());
        assertEquals(DoseAmount.UNIT_G, DoseAmount.G_1.unitText());
        assertEquals("0.5", DoseAmount.canonicalQuantity("0.50"));
        assertEquals("1", DoseAmount.canonicalQuantity("1.0"));
        assertEquals("4", DoseAmount.canonicalQuantity("4"));
        assertEquals("2.5", DoseAmount.canonicalQuantity("2.50"));
        assertEquals(null, DoseAmount.canonicalQuantity(""));
    }

    @Test
    public void confirmNeedsEveryFieldAndMealTimingFollowsMealRounds() {
        MedicineDraft medicine = new MedicineDraft();
        medicine.setName("name-for-test");
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.setStart(DAY);
        medicine.setEnd(DAY);
        medicine.rounds().add(DoseRound.MORNING);
        assertFalse(MedicationRules.hasRequiredFields(medicine));

        medicine.setTiming(DoseTiming.BEFORE_MEAL);
        assertTrue(MedicationRules.hasRequiredFields(medicine));

        medicine.timings().add(DoseTiming.AFTER_MEAL_30);
        assertFalse(MedicationRules.mealTimingMatches(medicine));
        assertFalse(MedicationRules.hasRequiredFields(medicine));

        medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        medicine.rounds().clear();
        medicine.rounds().add(DoseRound.BEDTIME);
        assertFalse(MedicationRules.hasRequiredFields(medicine));

        medicine.timings().clear();
        assertTrue(MedicationRules.hasRequiredFields(medicine));

        medicine.setPrn(true);
        medicine.rounds().clear();
        medicine.setTiming(DoseTiming.BEFORE_MEAL);
        assertFalse(MedicationRules.hasRequiredFields(medicine));
        medicine.timings().clear();
        assertTrue(MedicationRules.hasRequiredFields(medicine));

        medicine.setAmount(null);
        assertFalse(MedicationRules.hasRequiredFields(medicine));
    }

    @Test
    public void registrationPinsMealClocksAndLeavesBedtime() {
        Map<DoseRound, Integer> minutes = new EnumMap<>(DoseRound.class);
        minutes.put(DoseRound.MORNING, 9 * 60);
        minutes.put(DoseRound.LUNCH, 14 * 60);
        minutes.put(DoseRound.EVENING, 19 * 60);
        minutes.put(DoseRound.BEDTIME, 23 * 60);
        MedicationRules.pinSelectedMealTimes(EnumSet.of(DoseRound.MORNING, DoseRound.EVENING, DoseRound.BEDTIME), minutes);
        assertEquals(Integer.valueOf(MedicationRules.MORNING_MINUTE), minutes.get(DoseRound.MORNING));
        assertEquals(Integer.valueOf(14 * 60), minutes.get(DoseRound.LUNCH));
        assertEquals(Integer.valueOf(MedicationRules.EVENING_MINUTE), minutes.get(DoseRound.EVENING));
        assertEquals(Integer.valueOf(23 * 60), minutes.get(DoseRound.BEDTIME));
    }

    private static DoseRecord sample(long scheduled) {
        return new DoseRecord("id", "env", "med", "", "name-for-test", DoseRound.MORNING, scheduled);
    }
}
