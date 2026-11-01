package com.example.yakyeon2.data;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ScenarioPrescriptionsTest {

    private static final ZoneId ZONE = ScenarioPrescriptions.ZONE;
    private static final LocalDate DAY = ScenarioPrescriptions.REFERENCE_DAY;

    @Test
    public void knownRegistrationHasSixMedicinesAndNoDermatology() {
        YakyeonSession session = new YakyeonSession();
        MedicationStore store = ScenarioPrescriptions.knownStore(session);

        assertEquals(2, store.envelopes().size());
        assertEquals(6, store.activeMedicineCount());
        assertNames(store, "약1", "약2", "약3", "약4", "약5", "약6");
        assertNull(ScenarioPrescriptions.medicine(store, "약7"));
        assertNull(ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.DERMATOLOGY));

        SavedEnvelope clinic = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC);
        assertEquals(Integer.valueOf(ScenarioPrescriptions.CLINIC_SLOT), clinic.getSlotIndex());
        assertEquals(Integer.valueOf(ScenarioPrescriptions.minutes(9, 0)), clinic.alarmMinutes().get(DoseRound.MORNING));
        assertEquals(Integer.valueOf(ScenarioPrescriptions.minutes(13, 30)), clinic.alarmMinutes().get(DoseRound.LUNCH));
        assertEquals(Integer.valueOf(ScenarioPrescriptions.minutes(20, 0)), clinic.alarmMinutes().get(DoseRound.EVENING));
        assertFalse(clinic.alarmMinutes().containsKey(DoseRound.BEDTIME));
        assertPeriod(clinic, ScenarioPrescriptions.CLINIC_START, ScenarioPrescriptions.CLINIC_END);

        SavedEnvelope internal = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.INTERNAL);
        assertEquals(Integer.valueOf(ScenarioPrescriptions.INTERNAL_SLOT), internal.getSlotIndex());
        assertEquals(Integer.valueOf(ScenarioPrescriptions.minutes(8, 30)), internal.alarmMinutes().get(DoseRound.MORNING));
        assertEquals(Integer.valueOf(ScenarioPrescriptions.minutes(14, 0)), internal.alarmMinutes().get(DoseRound.LUNCH));
        assertEquals(Integer.valueOf(ScenarioPrescriptions.minutes(19, 0)), internal.alarmMinutes().get(DoseRound.EVENING));
        assertEquals(Integer.valueOf(ScenarioPrescriptions.minutes(23, 0)), internal.alarmMinutes().get(DoseRound.BEDTIME));
        assertPeriod(internal, ScenarioPrescriptions.INTERNAL_START, ScenarioPrescriptions.INTERNAL_END);
        assertEquals(DoseTiming.BEFORE_MEAL, ScenarioPrescriptions.medicine(store, "약4").getTiming());
        SavedMedicine medicineFive = ScenarioPrescriptions.medicine(store, "약5");
        assertTrue(medicineFive.timings().contains(DoseTiming.AFTER_MEAL_30));
        assertTrue(medicineFive.timings().contains(DoseTiming.BEDTIME));
        assertEquals(4, medicineFive.rounds().size());

        assertFalse(store.records().isEmpty());
        assertTrue(session.isSlotOccupied(ScenarioPrescriptions.CLINIC_SLOT));
        assertTrue(session.isSlotOccupied(ScenarioPrescriptions.INTERNAL_SLOT));
        assertFalse(session.isSlotOccupied(ScenarioPrescriptions.DERMATOLOGY_SLOT));
        assertFalse(session.isPillboxConnected());
    }

    @Test
    public void dermatologyRegistrationAddsThreeMedicinesWithoutChangingTheFirstSix() {
        YakyeonSession session = new YakyeonSession();
        MedicationStore store = ScenarioPrescriptions.knownStore(session);
        String clinicId = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC).getId();
        String internalId = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.INTERNAL).getId();
        int clinicRecords = recordsForEnvelope(store, clinicId).size();
        int internalRecords = recordsForEnvelope(store, internalId).size();
        Integer clinicMorning = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC)
                .alarmMinutes().get(DoseRound.MORNING);

        assertTrue(ScenarioPrescriptions.installDermatology(store));

        assertEquals(9, store.activeMedicineCount());
        assertEquals(3, store.envelopes().size());
        assertNames(store, "약1", "약2", "약3", "약4", "약5", "약6", "약7", "약8", "약9");
        SavedEnvelope dermatology = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.DERMATOLOGY);
        assertEquals(Integer.valueOf(ScenarioPrescriptions.DERMATOLOGY_SLOT), dermatology.getSlotIndex());
        assertEquals(Integer.valueOf(ScenarioPrescriptions.minutes(7, 30)), dermatology.alarmMinutes().get(DoseRound.MORNING));
        assertEquals(Integer.valueOf(ScenarioPrescriptions.minutes(12, 30)), dermatology.alarmMinutes().get(DoseRound.LUNCH));
        assertEquals(Integer.valueOf(ScenarioPrescriptions.minutes(19, 0)), dermatology.alarmMinutes().get(DoseRound.EVENING));
        assertPeriod(dermatology, ScenarioPrescriptions.DERMATOLOGY_START, ScenarioPrescriptions.DERMATOLOGY_END);

        assertEquals(clinicId, ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC).getId());
        assertEquals(internalId, ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.INTERNAL).getId());
        assertEquals(clinicMorning, ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC)
                .alarmMinutes().get(DoseRound.MORNING));
        assertEquals(clinicRecords, recordsForEnvelope(store, clinicId).size());
        assertEquals(internalRecords, recordsForEnvelope(store, internalId).size());
        assertEquals(Integer.valueOf(ScenarioPrescriptions.CLINIC_SLOT),
                ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC).getSlotIndex());
        assertEquals(Integer.valueOf(ScenarioPrescriptions.INTERNAL_SLOT),
                ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.INTERNAL).getSlotIndex());
        assertTrue(session.isSlotOccupied(ScenarioPrescriptions.DERMATOLOGY_SLOT));
        assertFalse(session.isPillboxConnected());
    }

    @Test
    public void repeatingScenarioRegistrationDoesNotDuplicate() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        assertFalse(ScenarioPrescriptions.installClinic(store));
        assertFalse(ScenarioPrescriptions.installInternal(store));
        assertEquals(6, store.activeMedicineCount());

        assertTrue(ScenarioPrescriptions.installDermatology(store));
        String dermatologyId = ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.DERMATOLOGY).getId();
        assertFalse(ScenarioPrescriptions.installDermatology(store));
        assertEquals(dermatologyId, ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.DERMATOLOGY).getId());
        assertEquals(9, store.activeMedicineCount());
        assertEquals(1, hospitalCount(store, ScenarioPrescriptions.DERMATOLOGY));
    }

    @Test
    public void referenceDayRoundsMatchEachMedicinesOwnSchedule() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        ScenarioPrescriptions.installDermatology(store);

        assertRounds(store, "약1", DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING);
        assertRounds(store, "약2", DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING);
        assertRounds(store, "약3", DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING);
        assertRounds(store, "약4", DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING);
        assertRounds(store, "약5", DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING, DoseRound.BEDTIME);
        assertRounds(store, "약6", DoseRound.MORNING, DoseRound.EVENING);
        assertRounds(store, "약7", DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING);
        assertRounds(store, "약8", DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING);
        assertRounds(store, "약9", DoseRound.EVENING);

        assertEquals(ScenarioPrescriptions.minutes(9, 0), clockMinutes(find(store, "약1", DAY, DoseRound.MORNING)));
        assertEquals(ScenarioPrescriptions.minutes(13, 30), clockMinutes(find(store, "약1", DAY, DoseRound.LUNCH)));
        assertEquals(ScenarioPrescriptions.minutes(23, 0), clockMinutes(find(store, "약5", DAY, DoseRound.BEDTIME)));
        assertEquals(ScenarioPrescriptions.minutes(19, 0), clockMinutes(find(store, "약9", DAY, DoseRound.EVENING)));
        assertNull(find(store, "약6", DAY, DoseRound.LUNCH));
        assertNull(find(store, "약6", DAY, DoseRound.BEDTIME));
        assertNull(find(store, "약9", DAY, DoseRound.MORNING));
        assertNull(find(store, "약9", DAY, DoseRound.LUNCH));
    }

    @Test
    public void statusUsesTheScheduledInstantAndTheThreeHourManualWindow() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        DoseRecord morning = find(store, "약1", DAY, DoseRound.MORNING);
        long scheduled = morning.getScheduledAtMillis();

        assertEquals(DoseStatus.SCHEDULED, MedicationRules.displayStatus(morning, scheduled - 1));
        assertFalse(MedicationRules.canRevise(morning, scheduled - 1));

        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(morning, scheduled));
        assertTrue(MedicationRules.canRevise(morning, scheduled));
        assertTrue(MedicationRules.canRevise(morning, scheduled + MedicationRules.THREE_HOURS_MS));
        assertFalse(MedicationRules.canRevise(morning, scheduled + MedicationRules.THREE_HOURS_MS + 1));

        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(morning, scheduled + 60_000L));
        assertTrue(store.recordManual(morning, scheduled + 60_000L, scheduled + 60_000L));
        assertEquals(DoseStatus.DELAYED, morning.getManualStatus());
        assertEquals(DoseStatus.DELAYED, MedicationRules.displayStatus(morning, scheduled + MedicationRules.THREE_HOURS_MS + 1));
        assertFalse(store.recordManual(morning, scheduled + 120_000L, scheduled + 120_000L));
        assertEquals(DoseStatus.DELAYED, morning.getManualStatus());
        assertEquals(Long.valueOf(scheduled + 60_000L), morning.getTakenAtMillis());
    }

    @Test
    public void manualEntryDoesNotCreateCompletedAndDoesNotChangeOtherRounds() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        DoseRecord morning = find(store, "약1", DAY, DoseRound.MORNING);
        DoseRecord lunch = find(store, "약1", DAY, DoseRound.LUNCH);
        DoseRecord otherMedicine = find(store, "약2", DAY, DoseRound.MORNING);
        long now = morning.getScheduledAtMillis() + 60_000L;

        assertTrue(store.recordManual(morning, now, now));
        assertEquals(DoseStatus.DELAYED, MedicationRules.displayStatus(morning, now));
        assertEquals(DoseStatus.SCHEDULED, MedicationRules.displayStatus(lunch, now));
        assertEquals(DoseStatus.MISSED, MedicationRules.displayStatus(otherMedicine, now));
        assertNull(lunch.getManualStatus());
        assertNull(otherMedicine.getManualStatus());
        assertEquals(DoseStatus.DELAYED, morning.getManualStatus());
    }

    @Test
    public void recordsStayOnTheirCalendarDate() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        DoseRecord today = find(store, "약1", DAY, DoseRound.MORNING);
        LocalDate next = DAY.plusDays(1);
        DoseRecord tomorrow = find(store, "약1", next, DoseRound.MORNING);

        assertTrue(today != null);
        assertTrue(tomorrow != null);
        assertFalse(today.getId().equals(tomorrow.getId()));
        assertEquals(DAY, localDate(today));
        assertEquals(next, localDate(tomorrow));
        assertEquals(0, countOnDate(store, "약1", DAY.minusYears(1)));
    }

    @Test
    public void roundsExistOnStartAndEndDaysAndNotOutsideThePeriod() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        ScenarioPrescriptions.installDermatology(store);

        assertEquals(0, countOnDate(store, "약1", ScenarioPrescriptions.CLINIC_START.minusDays(1)));
        assertTrue(countOnDate(store, "약1", ScenarioPrescriptions.CLINIC_START) > 0);
        assertTrue(countOnDate(store, "약1", DAY) > 0);
        assertTrue(countOnDate(store, "약1", ScenarioPrescriptions.CLINIC_END) > 0);
        assertEquals(0, countOnDate(store, "약1", ScenarioPrescriptions.CLINIC_END.plusDays(1)));

        assertEquals(0, countOnDate(store, "약4", ScenarioPrescriptions.INTERNAL_START.minusDays(1)));
        assertTrue(countOnDate(store, "약4", ScenarioPrescriptions.INTERNAL_START) > 0);
        assertTrue(countOnDate(store, "약5", ScenarioPrescriptions.INTERNAL_END) > 0);
        assertEquals(0, countOnDate(store, "약6", ScenarioPrescriptions.INTERNAL_END.plusDays(1)));

        assertEquals(0, countOnDate(store, "약7", ScenarioPrescriptions.DERMATOLOGY_START.minusDays(1)));
        assertTrue(countOnDate(store, "약7", ScenarioPrescriptions.DERMATOLOGY_START) > 0);
        assertTrue(countOnDate(store, "약9", ScenarioPrescriptions.DERMATOLOGY_END) > 0);
        assertEquals(0, countOnDate(store, "약9", ScenarioPrescriptions.DERMATOLOGY_END.plusDays(1)));
    }

    @Test
    public void registrationLaterOnTheStartDayOmitsEarlierRounds() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        EnvelopeDraft draft = store.beginDraft();
        draft.setHospitalName(ScenarioPrescriptions.DERMATOLOGY);
        draft.setSlotIndex(ScenarioPrescriptions.DERMATOLOGY_SLOT);
        draft.alarmMinutes().put(DoseRound.MORNING, ScenarioPrescriptions.minutes(7, 30));
        draft.alarmMinutes().put(DoseRound.LUNCH, ScenarioPrescriptions.minutes(12, 30));
        draft.alarmMinutes().put(DoseRound.EVENING, ScenarioPrescriptions.minutes(19, 0));
        MedicineDraft medicine = store.addMedicine(ScenarioPrescriptions.DERMATOLOGY_START);
        medicine.setName("약7-늦은등록");
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        medicine.setEnd(ScenarioPrescriptions.DERMATOLOGY_END);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.rounds().add(DoseRound.LUNCH);
        medicine.rounds().add(DoseRound.EVENING);
        medicine.setConfirmed(true);
        long registeredAt = DAY.atTime(8, 0).atZone(ZONE).toInstant().toEpochMilli();
        assertEquals(MedicationRules.SaveBlock.OK, store.commit(ZONE, registeredAt));

        assertNull(find(store, "약7-늦은등록", DAY, DoseRound.MORNING));
        assertTrue(find(store, "약7-늦은등록", DAY, DoseRound.LUNCH) != null);
        assertTrue(find(store, "약7-늦은등록", DAY.plusDays(1), DoseRound.MORNING) != null);
    }

    @Test
    public void applySavedEditKeepsManualRecordsAndReplacesUnmarkedOnes() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        DoseRecord delayed = find(store, "약1", DAY, DoseRound.MORNING);
        DoseRecord unmarkedPast = find(store, "약1", DAY.minusDays(1), DoseRound.MORNING);
        DoseRecord otherMedicine = find(store, "약2", DAY, DoseRound.MORNING);
        long now = delayed.getScheduledAtMillis() + 60_000L;
        assertTrue(store.recordManual(delayed, now, now));
        String delayedId = delayed.getId();
        String pastId = unmarkedPast.getId();

        SavedMedicine medicine = ScenarioPrescriptions.medicine(store, "약1");
        MedicineDraft edited = copy(medicine);
        store.editSaved(
                ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC).getId(),
                medicine.getId()
        );
        assertTrue(store.applySavedEdit(edited, ZONE));

        DoseRecord kept = recordById(store, delayedId);
        DoseRecord replaced = recordById(store, pastId);
        assertSame(delayed, kept);
        assertEquals(DoseStatus.DELAYED, kept.getManualStatus());
        assertNotSame(unmarkedPast, replaced);
        assertNull(replaced.getManualStatus());
        assertSame(otherMedicine, find(store, "약2", DAY, DoseRound.MORNING));
        assertEquals(1, ids(store, delayedId));
        assertEquals(1, ids(store, pastId));
        assertEquals(EnumSet.of(DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING), roundsOn(store, "약1", DAY));
    }

    private static void assertNames(MedicationStore store, String... names) {
        Set<String> found = new HashSet<>();
        for (SavedEnvelope envelope : store.envelopes()) {
            for (SavedMedicine medicine : envelope.medicines()) {
                found.add(medicine.getName());
            }
        }
        assertEquals(names.length, found.size());
        for (String name : names) {
            assertTrue(found.contains(name));
        }
    }

    private static void assertPeriod(SavedEnvelope envelope, LocalDate start, LocalDate end) {
        for (SavedMedicine medicine : envelope.medicines()) {
            assertEquals(start, medicine.getStart());
            assertEquals(end, medicine.getEnd());
        }
    }

    private static void assertRounds(MedicationStore store, String name, DoseRound... rounds) {
        assertEquals(EnumSet.copyOf(java.util.Arrays.asList(rounds)), roundsOn(store, name, DAY));
    }

    private static EnumSet<DoseRound> roundsOn(MedicationStore store, String name, LocalDate day) {
        EnumSet<DoseRound> rounds = EnumSet.noneOf(DoseRound.class);
        for (DoseRecord record : store.records()) {
            if (name.equals(record.getMedicineName()) && day.equals(localDate(record))) {
                rounds.add(record.getRound());
            }
        }
        return rounds;
    }

    private static DoseRecord find(MedicationStore store, String name, LocalDate day, DoseRound round) {
        for (DoseRecord record : store.records()) {
            if (name.equals(record.getMedicineName()) && round == record.getRound() && day.equals(localDate(record))) {
                return record;
            }
        }
        return null;
    }

    private static int countOnDate(MedicationStore store, String name, LocalDate day) {
        int count = 0;
        for (DoseRecord record : store.records()) {
            if (name.equals(record.getMedicineName()) && day.equals(localDate(record))) {
                count++;
            }
        }
        return count;
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

    private static DoseRecord recordById(MedicationStore store, String id) {
        for (DoseRecord record : store.records()) {
            if (id.equals(record.getId())) {
                return record;
            }
        }
        return null;
    }

    private static int ids(MedicationStore store, String id) {
        int count = 0;
        for (DoseRecord record : store.records()) {
            if (id.equals(record.getId())) {
                count++;
            }
        }
        return count;
    }

    private static int hospitalCount(MedicationStore store, String hospital) {
        int count = 0;
        for (SavedEnvelope envelope : store.envelopes()) {
            if (hospital.equals(envelope.getHospitalName())) {
                count++;
            }
        }
        return count;
    }

    private static LocalDate localDate(DoseRecord record) {
        return Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(ZONE).toLocalDate();
    }

    private static int clockMinutes(DoseRecord record) {
        return Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(ZONE).toLocalTime().toSecondOfDay() / 60;
    }

    private static MedicineDraft copy(SavedMedicine medicine) {
        MedicineDraft edited = new MedicineDraft();
        edited.setName(medicine.getName());
        edited.setAmount(medicine.getAmount());
        edited.setTiming(medicine.getTiming());
        edited.setStart(medicine.getStart());
        edited.setEnd(medicine.getEnd());
        edited.rounds().addAll(medicine.rounds());
        edited.setConfirmed(true);
        return edited;
    }
}
