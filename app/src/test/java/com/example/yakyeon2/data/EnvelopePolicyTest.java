package com.example.yakyeon2.data;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EnvelopePolicyTest {

    private static final java.time.ZoneId ZONE = ScenarioPrescriptions.ZONE;

    @Test
    public void medicineFiveKeepsTwoTimingsWithoutAddingAFifthRound() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        SavedMedicine medicine = ScenarioPrescriptions.medicine(store, "약5");
        assertEquals(2, medicine.timings().size());
        assertTrue(medicine.timings().contains(DoseTiming.AFTER_MEAL_30));
        assertTrue(medicine.timings().contains(DoseTiming.BEDTIME));
        assertNull(medicine.getTiming());
        assertEquals(4, medicine.rounds().size());
        long days = ChronoUnit.DAYS.between(
                ScenarioPrescriptions.INTERNAL_START,
                ScenarioPrescriptions.INTERNAL_END
        ) + 1;
        assertEquals(days * 4, forMedicine(store, "약5").size());
        assertEquals(days * 2, forMedicine(store, "약6").size());
    }

    @Test
    public void dermatologyMedicineNineCreatesOnlyTheEveningRound() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        assertTrue(ScenarioPrescriptions.installDermatology(store));
        long days = ChronoUnit.DAYS.between(
                ScenarioPrescriptions.DERMATOLOGY_START,
                ScenarioPrescriptions.DERMATOLOGY_END
        ) + 1;
        List<DoseRecord> records = forMedicine(store, "약9");
        assertEquals(days, records.size());
        for (DoseRecord record : records) {
            assertEquals(DoseRound.EVENING, record.getRound());
        }
        long internalDays = ChronoUnit.DAYS.between(
                ScenarioPrescriptions.INTERNAL_START,
                ScenarioPrescriptions.INTERNAL_END
        ) + 1;
        assertEquals(internalDays * 2, forMedicine(store, "약6").size());
    }

    @Test
    public void timingsSurviveExportAndASingleStoredTimingStillRestores() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        MedicationStore restored = new MedicationStore(new YakyeonSession());
        restored.importState(store.exportState());
        SavedMedicine five = ScenarioPrescriptions.medicine(restored, "약5");
        assertTrue(five.timings().contains(DoseTiming.AFTER_MEAL_30));
        assertTrue(five.timings().contains(DoseTiming.BEDTIME));
        assertEquals(DoseTiming.BEFORE_MEAL, ScenarioPrescriptions.medicine(restored, "약4").getTiming());
        assertEquals(DoseTiming.AFTER_MEAL_30, ScenarioPrescriptions.medicine(restored, "약1").getTiming());
        assertEquals(store.records().size(), restored.records().size());
    }

    @Test
    public void envelopeTimeEditMovesFutureRoundsAndLeavesTheOtherEnvelope() {
        YakyeonSession session = new YakyeonSession();
        MedicationStore store = ScenarioPrescriptions.knownStore(session);
        SavedEnvelope clinic = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC);
        long now = ScenarioPrescriptions.REFERENCE_DAY.atTime(14, 0).atZone(ZONE).toInstant().toEpochMilli();
        DoseRecord todayMorning = find(store, "약1", ScenarioPrescriptions.REFERENCE_DAY, DoseRound.MORNING);
        DoseRecord tomorrowMorning = find(store, "약1", ScenarioPrescriptions.REFERENCE_DAY.plusDays(1), DoseRound.MORNING);
        DoseRecord todayEvening = find(store, "약1", ScenarioPrescriptions.REFERENCE_DAY, DoseRound.EVENING);
        DoseRecord internalMorning = find(store, "약4", ScenarioPrescriptions.REFERENCE_DAY, DoseRound.MORNING);
        long missedAt = todayMorning.getScheduledAtMillis();
        long internalAt = internalMorning.getScheduledAtMillis();
        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(todayMorning, now));
        assertEquals(DoseStatus.SCHEDULED, MedicationRules.displayStatus(todayEvening, now));

        assertTrue(store.beginAlarmEdit(clinic.getId()));
        store.alarmEditMinutes().put(DoseRound.MORNING, ScenarioPrescriptions.minutes(9, 30));
        store.alarmEditMinutes().put(DoseRound.EVENING, ScenarioPrescriptions.minutes(20, 30));
        assertTrue(store.saveAlarmEdit(ZONE, now));

        DoseRecord missedAfter = find(store, "약1", ScenarioPrescriptions.REFERENCE_DAY, DoseRound.MORNING);
        assertEquals(missedAt, missedAfter.getScheduledAtMillis());
        assertNull(missedAfter.getManualStatus());
        assertEquals(ScenarioPrescriptions.minutes(9, 30), clock(tomorrowMorning(store)));
        assertEquals(
                DoseStatus.SCHEDULED,
                MedicationRules.displayStatus(tomorrowMorning(store), now)
        );
        long justBeforeNewMorning = ScenarioPrescriptions.REFERENCE_DAY.plusDays(1)
                .atTime(9, 0)
                .atZone(ZONE)
                .toInstant()
                .toEpochMilli();
        assertEquals(
                DoseStatus.SCHEDULED,
                MedicationRules.displayStatus(tomorrowMorning(store), justBeforeNewMorning)
        );
        long atNewMorning = ScenarioPrescriptions.REFERENCE_DAY.plusDays(1)
                .atTime(9, 30)
                .atZone(ZONE)
                .toInstant()
                .toEpochMilli();
        assertEquals(
                DoseStatus.MISSED,
                MedicationRules.displayStatus(tomorrowMorning(store), atNewMorning)
        );
        assertEquals(ScenarioPrescriptions.minutes(20, 30), clock(find(
                store,
                "약1",
                ScenarioPrescriptions.REFERENCE_DAY,
                DoseRound.EVENING
        )));
        assertSame(internalMorning, find(store, "약4", ScenarioPrescriptions.REFERENCE_DAY, DoseRound.MORNING));
        assertEquals(internalAt, internalMorning.getScheduledAtMillis());
        assertFalse(session.isPillboxConnected());

        MedicationStore restarted = new MedicationStore(new YakyeonSession());
        restarted.importState(store.exportState());
        assertEquals(
                Integer.valueOf(ScenarioPrescriptions.minutes(20, 30)),
                ScenarioPrescriptions.envelope(restarted, ScenarioPrescriptions.CLINIC)
                        .alarmMinutes()
                        .get(DoseRound.EVENING)
        );
        assertEquals(ScenarioPrescriptions.minutes(20, 30), clock(find(
                restarted,
                "약1",
                ScenarioPrescriptions.REFERENCE_DAY,
                DoseRound.EVENING
        )));
        assertEquals(
                DoseTiming.BEFORE_MEAL,
                ScenarioPrescriptions.medicine(restarted, "약4").getTiming()
        );
    }

    @Test
    public void manualDelayedRecordStaysWhenItsEnvelopeTimeChanges() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        SavedEnvelope clinic = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC);
        DoseRecord lunch = find(store, "약1", ScenarioPrescriptions.REFERENCE_DAY, DoseRound.LUNCH);
        long lunchAt = lunch.getScheduledAtMillis();
        long now = lunchAt + 60_000L;
        assertTrue(store.recordManual(lunch, now, now));
        assertEquals(DoseStatus.DELAYED, lunch.getManualStatus());

        assertTrue(store.beginAlarmEdit(clinic.getId()));
        store.alarmEditMinutes().put(DoseRound.EVENING, ScenarioPrescriptions.minutes(20, 30));
        assertTrue(store.saveAlarmEdit(ZONE, now));

        DoseRecord after = find(store, "약1", ScenarioPrescriptions.REFERENCE_DAY, DoseRound.LUNCH);
        assertSame(lunch, after);
        assertEquals(lunchAt, after.getScheduledAtMillis());
        assertEquals(DoseStatus.DELAYED, after.getManualStatus());
        assertEquals(DoseStatus.SCHEDULED, MedicationRules.displayStatus(
                find(store, "약2", ScenarioPrescriptions.REFERENCE_DAY, DoseRound.LUNCH),
                lunchAt - 1
        ));
    }

    @Test
    public void openingTheEditWheelsDoesNotStoreAnUnusedBedtime() {
        Map<DoseRound, Integer> minutes = new EnumMap<>(DoseRound.class);
        minutes.put(DoseRound.MORNING, ScenarioPrescriptions.minutes(9, 0));
        minutes.put(DoseRound.LUNCH, ScenarioPrescriptions.minutes(13, 30));
        minutes.put(DoseRound.EVENING, ScenarioPrescriptions.minutes(20, 0));
        assertEquals(
                22 * 60,
                MedicationRules.displayedAlarmMinute(minutes, DoseRound.BEDTIME, false)
        );
        assertFalse(minutes.containsKey(DoseRound.BEDTIME));

        Map<DoseRound, Integer> used = new EnumMap<>(DoseRound.class);
        assertEquals(8 * 60, MedicationRules.displayedAlarmMinute(used, DoseRound.MORNING, true));
        assertEquals(Integer.valueOf(8 * 60), used.get(DoseRound.MORNING));

        MedicationStore store = ScenarioPrescriptions.knownStore();
        SavedEnvelope clinic = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC);
        int before = recordsForEnvelope(store, clinic.getId()).size();
        long now = ScenarioPrescriptions.startOf(ScenarioPrescriptions.REFERENCE_DAY);
        assertTrue(store.beginAlarmEdit(clinic.getId()));
        assertFalse(store.alarmEditMinutes().containsKey(DoseRound.BEDTIME));
        assertTrue(store.saveAlarmEdit(ZONE, now));
        assertFalse(clinic.alarmMinutes().containsKey(DoseRound.BEDTIME));
        assertEquals(before, recordsForEnvelope(store, clinic.getId()).size());
    }

    @Test
    public void pendingSlotStaysChosenAcrossRefreshUntilConfirm() {
        YakyeonSession session = new YakyeonSession();
        MedicationStore store = ScenarioPrescriptions.knownStore(session);
        SavedEnvelope clinic = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC);
        SavedEnvelope internal = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.INTERNAL);
        assertTrue(store.beginSlotAssign(clinic.getId()));
        assertEquals(Integer.valueOf(ScenarioPrescriptions.CLINIC_SLOT), store.pendingSlotIndex());
        assertEquals(clinic.getId(), store.slotAssignEnvelopeId());

        store.choosePendingSlot(ScenarioPrescriptions.DERMATOLOGY_SLOT);
        assertEquals(Integer.valueOf(ScenarioPrescriptions.DERMATOLOGY_SLOT), store.pendingSlotIndex());
        assertEquals(Integer.valueOf(ScenarioPrescriptions.CLINIC_SLOT), clinic.getSlotIndex());
        assertEquals(Integer.valueOf(ScenarioPrescriptions.INTERNAL_SLOT), internal.getSlotIndex());
        assertFalse(session.isSlotOccupied(ScenarioPrescriptions.DERMATOLOGY_SLOT));
        assertTrue(session.isSlotOccupied(ScenarioPrescriptions.CLINIC_SLOT));

        assertEquals(Integer.valueOf(ScenarioPrescriptions.DERMATOLOGY_SLOT), store.pendingSlotIndex());
        assertEquals(clinic.getId(), store.slotAssignEnvelopeId());
        assertTrue(store.confirmSlotAssign());
        assertEquals(Integer.valueOf(ScenarioPrescriptions.DERMATOLOGY_SLOT), clinic.getSlotIndex());
        assertFalse(store.slotTakenByOther(internal.getId(), ScenarioPrescriptions.INTERNAL_SLOT));
        assertTrue(store.slotTakenByOther(internal.getId(), ScenarioPrescriptions.DERMATOLOGY_SLOT));
        assertFalse(store.assignSlot(internal.getId(), ScenarioPrescriptions.DERMATOLOGY_SLOT));
        assertEquals(Integer.valueOf(ScenarioPrescriptions.INTERNAL_SLOT), internal.getSlotIndex());
        assertFalse(session.isPillboxConnected());
    }

    @Test
    public void storedUnusedBedtimeMinuteDoesNotCreateClinicRounds() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        SavedEnvelope clinic = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC);
        int before = recordsForEnvelope(store, clinic.getId()).size();
        long now = ScenarioPrescriptions.startOf(ScenarioPrescriptions.REFERENCE_DAY);
        assertTrue(store.beginAlarmEdit(clinic.getId()));
        store.alarmEditMinutes().put(DoseRound.BEDTIME, ScenarioPrescriptions.minutes(23, 0));
        assertTrue(store.saveAlarmEdit(ZONE, now));
        assertEquals(Integer.valueOf(ScenarioPrescriptions.minutes(23, 0)), clinic.alarmMinutes().get(DoseRound.BEDTIME));
        for (DoseRecord record : recordsForEnvelope(store, clinic.getId())) {
            assertFalse(record.getRound() == DoseRound.BEDTIME);
        }
        assertEquals(before, recordsForEnvelope(store, clinic.getId()).size());
    }

    @Test
    public void slotAssignmentRejectsASlotAnotherEnvelopeUses() {
        YakyeonSession session = new YakyeonSession();
        MedicationStore store = ScenarioPrescriptions.knownStore(session);
        SavedEnvelope clinic = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC);
        int records = store.records().size();
        assertFalse(store.assignSlot(clinic.getId(), ScenarioPrescriptions.INTERNAL_SLOT));
        assertEquals(Integer.valueOf(ScenarioPrescriptions.CLINIC_SLOT), clinic.getSlotIndex());
        assertEquals(records, store.records().size());
        assertTrue(session.isSlotOccupied(ScenarioPrescriptions.CLINIC_SLOT));
        assertFalse(session.isPillboxConnected());
    }

    @Test
    public void cancellingSlotAssignmentKeepsThePreviousSlot() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        SavedEnvelope clinic = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC);
        int records = store.records().size();
        assertTrue(store.beginSlotAssign(clinic.getId()));
        store.choosePendingSlot(ScenarioPrescriptions.DERMATOLOGY_SLOT);
        store.cancelSlotAssign();
        assertNull(store.slotAssignEnvelopeId());
        assertEquals(Integer.valueOf(ScenarioPrescriptions.CLINIC_SLOT), clinic.getSlotIndex());
        assertEquals(records, store.records().size());
        assertEquals(3, clinic.medicines().size());
    }

    @Test
    public void confirmingSlotAssignmentStoresTheSlotWithoutConnectingWifi() {
        YakyeonSession session = new YakyeonSession();
        MedicationStore store = ScenarioPrescriptions.knownStore(session);
        SavedEnvelope clinic = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC);
        int records = store.records().size();
        assertTrue(store.beginSlotAssign(clinic.getId()));
        store.choosePendingSlot(ScenarioPrescriptions.DERMATOLOGY_SLOT);
        assertEquals(Integer.valueOf(ScenarioPrescriptions.CLINIC_SLOT), clinic.getSlotIndex());
        assertTrue(store.confirmSlotAssign());
        assertNull(store.slotAssignEnvelopeId());
        assertEquals(Integer.valueOf(ScenarioPrescriptions.DERMATOLOGY_SLOT), clinic.getSlotIndex());
        assertFalse(session.isSlotOccupied(ScenarioPrescriptions.CLINIC_SLOT));
        assertTrue(session.isSlotOccupied(ScenarioPrescriptions.DERMATOLOGY_SLOT));
        assertFalse(session.isPillboxConnected());
        assertEquals(records, store.records().size());
    }

    @Test
    public void clearingPillboxUseKeepsMedicinesAndRecords() {
        YakyeonSession session = new YakyeonSession();
        MedicationStore store = ScenarioPrescriptions.knownStore(session);
        SavedEnvelope clinic = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC);
        int records = store.records().size();
        assertTrue(store.assignSlot(clinic.getId(), null));
        assertNull(clinic.getSlotIndex());
        assertEquals(3, clinic.medicines().size());
        assertEquals(records, store.records().size());
        assertFalse(session.isSlotOccupied(ScenarioPrescriptions.CLINIC_SLOT));
        assertFalse(session.isPillboxConnected());
    }

    @Test
    public void reconnectStaysUnlinkedUntilAcceptedAndRejectKeepsDisconnected() {
        YakyeonSession session = new YakyeonSession();
        assertFalse(session.isCareLinked());
        assertFalse(session.receiveReconnectRequest());
        assertFalse(session.acceptReconnect());
        assertEquals(YakyeonSession.CareLink.NONE, session.getCareLink());

        session.markCareDisconnected();
        assertTrue(session.receiveReconnectRequest());
        assertEquals(YakyeonSession.CareLink.REQUESTED, session.getCareLink());
        assertFalse(session.isCareLinked());
        assertTrue(session.rejectReconnect());
        assertEquals(YakyeonSession.CareLink.DISCONNECTED, session.getCareLink());
        assertFalse(session.isCareLinked());

        assertTrue(session.receiveReconnectRequest());
        assertTrue(session.acceptReconnect());
        assertEquals(YakyeonSession.CareLink.LINKED, session.getCareLink());
        assertFalse(session.acceptReconnect());
        session.markCareDisconnected();
        assertEquals(YakyeonSession.CareLink.DISCONNECTED, session.getCareLink());
        assertFalse(session.isCareLinked());
    }

    @Test
    public void scenarioInstallLeavesAnInProgressDraftUntouched() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        EnvelopeDraft draft = store.beginDraft();
        draft.setHospitalName("작성중");
        assertFalse(ScenarioPrescriptions.installKnown(store));
        assertEquals("작성중", store.draft().getHospitalName());
        assertFalse(ScenarioPrescriptions.hasHospital(store, ScenarioPrescriptions.CLINIC));
    }

    @Test
    public void removeScenarioLeavesGraphCheckEnvelopes() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        assertTrue(store.installGraphChecks(1, ZONE, ScenarioPrescriptions.startOf(ScenarioPrescriptions.REFERENCE_DAY)));
        assertTrue(ScenarioPrescriptions.installKnown(store));
        assertTrue(ScenarioPrescriptions.removeScenario(store) >= 2);
        assertFalse(ScenarioPrescriptions.hasHospital(store, ScenarioPrescriptions.CLINIC));
        assertFalse(ScenarioPrescriptions.hasHospital(store, ScenarioPrescriptions.INTERNAL));
        int graph = 0;
        for (SavedEnvelope envelope : store.envelopes()) {
            if (MedicationStore.GRAPH_CHECK_HOSPITAL.equals(envelope.getHospitalName())) {
                graph++;
            }
        }
        assertEquals(1, graph);
    }

    private static DoseRecord tomorrowMorning(MedicationStore store) {
        return find(store, "약1", ScenarioPrescriptions.REFERENCE_DAY.plusDays(1), DoseRound.MORNING);
    }

    private static DoseRecord find(MedicationStore store, String name, LocalDate day, DoseRound round) {
        SavedMedicine medicine = ScenarioPrescriptions.medicine(store, name);
        assertNotNull(medicine);
        for (DoseRecord record : store.records()) {
            if (!medicine.getId().equals(record.getMedicineId()) || record.getRound() != round) {
                continue;
            }
            LocalDate recordDay = Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(ZONE).toLocalDate();
            if (day.equals(recordDay)) {
                return record;
            }
        }
        throw new AssertionError(name + " " + day + " " + round);
    }

    private static int clock(DoseRecord record) {
        return Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(ZONE).toLocalTime().toSecondOfDay() / 60;
    }

    private static List<DoseRecord> forMedicine(MedicationStore store, String name) {
        SavedMedicine medicine = ScenarioPrescriptions.medicine(store, name);
        List<DoseRecord> found = new ArrayList<>();
        for (DoseRecord record : store.records()) {
            if (medicine.getId().equals(record.getMedicineId())) {
                found.add(record);
            }
        }
        return found;
    }

    private static List<DoseRecord> recordsForEnvelope(MedicationStore store, String envelopeId) {
        List<DoseRecord> found = new ArrayList<>();
        for (DoseRecord record : store.records()) {
            if (envelopeId.equals(record.getEnvelopeId())) {
                found.add(record);
            }
        }
        return found;
    }
}
