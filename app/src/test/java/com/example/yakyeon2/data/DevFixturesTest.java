package com.example.yakyeon2.data;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DevFixturesTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 8);

    @Test
    public void fixturesStayOutOfTheSavedFileAndDoNotReplaceUserData() {
        long now = DAY.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();
        MedicationStore store = userStore();
        String userId = store.envelopes().get(0).getId();
        int userRecords = store.records().size();

        assertTrue(DevFixtures.apply(store, DevFixtures.Scenario.HOME_REMAINING, ZONE, now));
        assertTrue(store.hasFixture());
        assertTrue(store.exportState().contains(userId));
        assertFalse(store.exportState().contains("fixture-"));

        DevFixtures.reset(store);
        assertFalse(store.hasFixture());
        assertEquals(1, store.envelopes().size());
        assertEquals(userId, store.envelopes().get(0).getId());
        assertEquals(userRecords, store.records().size());
    }

    @Test
    public void homeRemainingUsesFutureRoundsFromTheInjectedClock() {
        long now = DAY.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();
        MedicationStore store = new MedicationStore(new YakyeonSession());
        assertTrue(DevFixtures.apply(store, DevFixtures.Scenario.HOME_REMAINING, ZONE, now));
        List<DoseRecord> left = MedicationRules.remainingToday(store.records(), now, ZONE);
        assertEquals(4, left.size());
        for (DoseRecord record : left) {
            assertEquals(DoseStatus.SCHEDULED, MedicationRules.displayStatus(record, now));
            assertTrue(record.getScheduledAtMillis() > now);
            assertTrue(record.getEnvelopeName().startsWith("개발용 예시"));
        }
        assertEquals(2, store.activeEnvelopeCount());
    }

    @Test
    public void scheduleFixtureShowsEveryStatusAroundTheInjectedClock() {
        long now = DAY.atTime(17, 0).atZone(ZONE).toInstant().toEpochMilli();
        MedicationStore store = new MedicationStore(new YakyeonSession());
        assertTrue(DevFixtures.apply(store, DevFixtures.Scenario.SCHEDULE_STATUSES, ZONE, now));
        assertEquals(DoseStatus.COMPLETED, status(store, DoseRound.MORNING, now));
        assertEquals(DoseStatus.DELAYED, status(store, DoseRound.LUNCH, now));
        assertEquals(DoseStatus.MISSED, status(store, DoseRound.EVENING, now));
        assertEquals(DoseStatus.SCHEDULED, status(store, DoseRound.BEDTIME, now));
    }

    @Test
    public void slotFixturesDoNotClearAUserSlotAndResetDropsOnlyTheExample() {
        MedicationStore store = userStore();
        long now = DAY.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();
        assertFalse(DevFixtures.apply(store, DevFixtures.Scenario.SLOTS_FULL, ZONE, now));
        assertEquals(1, store.envelopes().size());
        assertTrue(store.sessionSlotOccupied(0));

        assertTrue(DevFixtures.apply(store, DevFixtures.Scenario.SLOTS_PARTIAL, ZONE, now));
        assertTrue(store.sessionSlotOccupied(0));
        assertTrue(store.sessionSlotOccupied(1));
        assertTrue(store.sessionSlotOccupied(2));
        assertTrue(store.pillboxConnected());
        DevFixtures.reset(store);
        assertEquals(1, store.envelopes().size());
        assertTrue(store.sessionSlotOccupied(0));
        assertFalse(store.sessionSlotOccupied(1));
        assertFalse(store.pillboxConnected());
    }

    @Test
    public void alarmPracticeIsNotSavedUntilCommit() {
        long now = DAY.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();
        MedicationStore store = new MedicationStore(new YakyeonSession());
        assertTrue(DevFixtures.apply(store, DevFixtures.Scenario.ALARM_DRAFT, ZONE, now));
        assertTrue(store.draft().isFixturePractice());
        assertFalse(store.draft().isDevSample());
        assertEquals(Integer.valueOf(8 * 60), store.draft().alarmMinutes().get(DoseRound.MORNING));
        assertTrue(store.exportState().isEmpty() || !store.exportState().contains("fixture-"));
        assertEquals(MedicationRules.SaveBlock.OK, store.commit(ZONE, now));
        assertEquals(1, store.envelopes().size());
        assertFalse(MedicationStore.isSampleId(store.envelopes().get(0).getId()));
        assertTrue(store.exportState().contains(store.envelopes().get(0).getId()));
    }

    private static DoseStatus status(MedicationStore store, DoseRound round, long now) {
        for (DoseRecord record : store.records()) {
            if (record.getRound() == round
                    && Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(ZONE).toLocalDate().equals(DAY)) {
                return MedicationRules.displayStatus(record, now);
            }
        }
        return null;
    }

    private static MedicationStore userStore() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        EnvelopeDraft draft = store.beginDraft();
        MedicineDraft medicine = store.addMedicine(DAY);
        medicine.setName("user-medicine");
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.setStart(DAY);
        medicine.setEnd(DAY);
        medicine.setConfirmed(true);
        draft.setHospitalName("user-hospital");
        draft.setSlotIndex(0);
        draft.alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        assertEquals(
                MedicationRules.SaveBlock.OK,
                store.commit(ZONE, DAY.atStartOfDay(ZONE).toInstant().toEpochMilli())
        );
        return store;
    }
}
