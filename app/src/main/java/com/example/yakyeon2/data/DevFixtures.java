package com.example.yakyeon2.data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * In-memory examples for debug builds. Callers must not invoke this from release UI.
 * Saved files omit these envelopes.
 */
public final class DevFixtures {

    public enum Scenario {
        MULTIPLE_ENVELOPES,
        HOME_REMAINING,
        SCHEDULE_STATUSES,
        ALARM_DRAFT,
        SLOTS_PARTIAL,
        SLOTS_FULL
    }

    private DevFixtures() {
    }

    public static void reset(MedicationStore store) {
        if (store == null) {
            return;
        }
        store.setDevGraphRelationScenario(false);
        store.clearFixtures();
    }

    public static boolean apply(MedicationStore store, Scenario scenario, ZoneId zone, long now) {
        if (store == null || scenario == null || zone == null) {
            return false;
        }
        store.clearFixtures();
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        switch (scenario) {
            case MULTIPLE_ENVELOPES:
                return addPair(store, zone, today, startOfDay(today, zone), meals(8 * 60, 13 * 60, 18 * 60), false);
            case HOME_REMAINING:
                return addPair(
                        store,
                        zone,
                        today,
                        startOfDay(today, zone),
                        remainingAlarms(zone, now),
                        false
                );
            case SCHEDULE_STATUSES:
                return addScheduleStatuses(store, zone, today, now);
            case ALARM_DRAFT:
                return prepareAlarmDraft(store, today);
            case SLOTS_PARTIAL:
                return addSlots(store, zone, today, 2);
            case SLOTS_FULL:
                return addSlots(store, zone, today, 3);
            default:
                return false;
        }
    }

    private static boolean addPair(
            MedicationStore store,
            ZoneId zone,
            LocalDate today,
            long registeredAt,
            Map<DoseRound, Integer> alarms,
            boolean withBedtime
    ) {
        addEnvelope(store, zone, "개발용 예시 갑", null, alarms, registeredAt, today, withBedtime, "개발용 예시 약 1");
        addEnvelope(store, zone, "개발용 예시 을", null, alarms, registeredAt, today, withBedtime, "개발용 예시 약 2");
        return true;
    }

    private static boolean addScheduleStatuses(MedicationStore store, ZoneId zone, LocalDate today, long now) {
        int minute = Instant.ofEpochMilli(now).atZone(zone).getHour() * 60
                + Instant.ofEpochMilli(now).atZone(zone).getMinute();
        int missedAt = Math.max(0, minute - 1);
        int upcoming = Math.min(MedicationRules.MINUTES_PER_DAY - 1, minute + 60);
        Map<DoseRound, Integer> alarms = new EnumMap<>(DoseRound.class);
        alarms.put(DoseRound.MORNING, 8 * 60);
        alarms.put(DoseRound.LUNCH, 12 * 60);
        alarms.put(DoseRound.EVENING, missedAt);
        alarms.put(DoseRound.BEDTIME, upcoming);
        SavedEnvelope envelope = addEnvelope(
                store,
                zone,
                "개발용 예시 갑",
                null,
                alarms,
                startOfDay(today, zone),
                today,
                true,
                "개발용 예시 약 1"
        );
        DoseRecord morning = find(store, envelope.getId(), today, DoseRound.MORNING, zone);
        DoseRecord lunch = find(store, envelope.getId(), today, DoseRound.LUNCH, zone);
        if (morning != null) {
            morning.restoreManual(DoseStatus.COMPLETED, morning.getScheduledAtMillis());
        }
        if (lunch != null) {
            lunch.restoreManual(DoseStatus.DELAYED, lunch.getScheduledAtMillis() + 60_000L);
        }
        return morning != null && lunch != null && now >= 0;
    }

    private static boolean addSlots(MedicationStore store, ZoneId zone, LocalDate today, int needed) {
        if (store.freeSlotCount() < needed) {
            return false;
        }
        Map<DoseRound, Integer> alarms = meals(8 * 60, 13 * 60, 18 * 60);
        int placed = 0;
        for (int index = 0; index < store.sessionSlotCount() && placed < needed; index++) {
            if (store.sessionSlotOccupied(index)) {
                continue;
            }
            addEnvelope(
                    store,
                    zone,
                    "개발용 예시 칸 " + (index + 1),
                    index,
                    alarms,
                    startOfDay(today, zone),
                    today,
                    false,
                    "개발용 예시 약 " + (index + 1)
            );
            placed++;
        }
        if (placed != needed) {
            store.clearFixtures();
            return false;
        }
        store.markFixturePillbox();
        return true;
    }

    private static boolean prepareAlarmDraft(MedicationStore store, LocalDate today) {
        EnvelopeDraft existing = store.draft();
        if (existing != null && !existing.medicines().isEmpty() && !existing.isFixturePractice()) {
            return false;
        }
        EnvelopeDraft draft = store.beginDraft();
        draft.setFixturePractice(true);
        draft.setHospitalName("개발용 예시");
        draft.alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        draft.alarmMinutes().put(DoseRound.LUNCH, 13 * 60);
        draft.alarmMinutes().put(DoseRound.EVENING, 18 * 60);
        MedicineDraft medicine = store.addMedicine(today);
        medicine.setName("개발용 예시 약 1");
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.rounds().add(DoseRound.LUNCH);
        medicine.rounds().add(DoseRound.EVENING);
        medicine.setEnd(today.plusDays(6));
        medicine.setConfirmed(true);
        return true;
    }

    private static SavedEnvelope addEnvelope(
            MedicationStore store,
            ZoneId zone,
            String hospital,
            Integer slot,
            Map<DoseRound, Integer> alarms,
            long registeredAt,
            LocalDate today,
            boolean bedtime,
            String medicineName
    ) {
        SavedEnvelope envelope = new SavedEnvelope(store.allocateId("fixture-"), hospital, slot, alarms);
        envelope.setRegisteredAtMillis(registeredAt);
        EnumSet<DoseRound> rounds = EnumSet.of(DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING);
        if (bedtime) {
            rounds.add(DoseRound.BEDTIME);
        }
        MedicineDraft draft = new MedicineDraft();
        draft.setName(medicineName);
        draft.setAmount(DoseAmount.TABLET_1);
        draft.setTiming(DoseTiming.AFTER_MEAL_30);
        draft.rounds().addAll(rounds);
        draft.setStart(today);
        draft.setEnd(today);
        draft.setConfirmed(true);
        envelope.medicines().add(new SavedMedicine(store.allocateId("fixture-med-"), draft));
        store.rememberSample(envelope, zone);
        return envelope;
    }

    private static Map<DoseRound, Integer> remainingAlarms(ZoneId zone, long now) {
        ZonedDateTime moment = Instant.ofEpochMilli(now).atZone(zone);
        int minute = moment.getHour() * 60 + moment.getMinute();
        int lunch = 12 * 60 + 10;
        int evening = 19 * 60 + 10;
        if (minute >= evening) {
            lunch = Math.min(minute + 30, MedicationRules.MINUTES_PER_DAY - 1);
            int later = lunch + MedicationRules.FOUR_HOURS_MINUTES;
            evening = later < MedicationRules.MINUTES_PER_DAY ? later : lunch;
        }
        return meals(8 * 60, lunch, evening);
    }

    private static Map<DoseRound, Integer> meals(int morning, int lunch, int evening) {
        Map<DoseRound, Integer> alarms = new EnumMap<>(DoseRound.class);
        alarms.put(DoseRound.MORNING, morning);
        alarms.put(DoseRound.LUNCH, lunch);
        alarms.put(DoseRound.EVENING, evening);
        return alarms;
    }

    private static long startOfDay(LocalDate day, ZoneId zone) {
        return day.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    private static DoseRecord find(
            MedicationStore store,
            String envelopeId,
            LocalDate day,
            DoseRound round,
            ZoneId zone
    ) {
        for (DoseRecord record : store.records()) {
            if (!envelopeId.equals(record.getEnvelopeId()) || record.getRound() != round) {
                continue;
            }
            ZonedDateTime time = Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(zone);
            if (time.toLocalDate().equals(day)) {
                return record;
            }
        }
        return null;
    }
}
