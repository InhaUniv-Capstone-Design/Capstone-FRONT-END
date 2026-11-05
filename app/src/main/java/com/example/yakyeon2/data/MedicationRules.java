package com.example.yakyeon2.data;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MedicationRules {

    public static final long THREE_HOURS_MS = 3L * 60L * 60L * 1000L;
    public static final int FOUR_HOURS_MINUTES = 4 * 60;
    public static final int MEAL_LATEST_MINUTE = 20 * 60 + 59;
    public static final int BEDTIME_LATEST_MINUTE = 23 * 60 + 59;
    public static final int HISTORY_DEFAULT_DAYS = 14;
    public static final int HISTORY_MAX_DAYS = 365;
    public static final int MINUTES_PER_DAY = 24 * 60;

    public enum TimeCheck {
        OK,
        MISSING,
        TOO_CLOSE
    }

    public enum SaveBlock {
        OK,
        MISSING_REQUIRED,
        NOT_CONFIRMED,
        MISSING_TIME,
        TOO_CLOSE
    }

    public static final class HistoryPage {
        public final boolean hasRecords;
        public final List<DoseRecord> visible;

        public HistoryPage(boolean hasRecords, List<DoseRecord> visible) {
            this.hasRecords = hasRecords;
            this.visible = visible;
        }
    }

    private MedicationRules() {
    }

    public static DoseStatus displayStatus(DoseRecord record, long now) {
        DoseStatus manual = record.getManualStatus();
        if (manual == DoseStatus.DELAYED || manual == DoseStatus.COMPLETED) {
            return manual;
        }
        if (now < record.getScheduledAtMillis()) {
            return DoseStatus.SCHEDULED;
        }
        return DoseStatus.MISSED;
    }

    public enum ReviseBlock {
        ALLOWED,
        NOT_YET,
        TOO_LATE,
        MANUAL
    }

    public static ReviseBlock reviseBlock(DoseRecord record, long now) {
        if (record.getManualStatus() != null) {
            return ReviseBlock.MANUAL;
        }
        long scheduled = record.getScheduledAtMillis();
        if (now < scheduled) {
            return ReviseBlock.NOT_YET;
        }
        if (now > scheduled + THREE_HOURS_MS) {
            return ReviseBlock.TOO_LATE;
        }
        return ReviseBlock.ALLOWED;
    }

    public static boolean canRevise(DoseRecord record, long now) {
        return reviseBlock(record, now) == ReviseBlock.ALLOWED;
    }

    public static boolean recordManual(DoseRecord record, long now, long takenAtMillis) {
        if (!canRevise(record, now)) {
            return false;
        }
        record.markDelayed(takenAtMillis);
        return record.getManualStatus() == DoseStatus.DELAYED;
    }

    /**
     * Hardware intake before the scheduled instant. Lid-open alone does not call this.
     * The dose becomes completed at {@code takenAtMillis}.
     */
    public static boolean recordHardwareEarly(DoseRecord record, long takenAtMillis) {
        if (record == null || record.getManualStatus() != null) {
            return false;
        }
        if (takenAtMillis >= record.getScheduledAtMillis()) {
            return false;
        }
        record.markCompleted(takenAtMillis);
        return record.getManualStatus() == DoseStatus.COMPLETED;
    }

    public static int circularGapMinutes(int first, int second) {
        int diff = Math.abs(first - second) % MINUTES_PER_DAY;
        return Math.min(diff, MINUTES_PER_DAY - diff);
    }

    /**
     * Wheel label for one round. A missing minute is written only when {@code writeIfMissing}
     * is true, which is the rounds this envelope actually takes. Showing an unused round,
     * such as bedtime on a three-meal envelope, must not store 22:00 by itself.
     */
    public static int displayedAlarmMinute(
            Map<DoseRound, Integer> minutes,
            DoseRound round,
            boolean writeIfMissing
    ) {
        Integer current = minutes == null ? null : minutes.get(round);
        if (current != null && current >= 0 && current < MINUTES_PER_DAY) {
            return current;
        }
        int seeded;
        switch (round) {
            case LUNCH:
                seeded = 13 * 60;
                break;
            case EVENING:
                seeded = 18 * 60;
                break;
            case BEDTIME:
                seeded = 22 * 60;
                break;
            case MORNING:
            default:
                seeded = 8 * 60;
                break;
        }
        if (writeIfMissing && minutes != null) {
            minutes.put(round, seeded);
        }
        return seeded;
    }

    public static TimeCheck checkAlarmTimes(Set<DoseRound> required, Map<DoseRound, Integer> minutes) {
        if (required == null || required.isEmpty()) {
            return TimeCheck.OK;
        }
        if (minutes == null) {
            return TimeCheck.MISSING;
        }
        Integer previous = null;
        for (DoseRound round : DoseRound.values()) {
            if (!required.contains(round)) {
                continue;
            }
            Integer value = minutes.get(round);
            if (value == null || value < 0 || value >= MINUTES_PER_DAY) {
                return TimeCheck.MISSING;
            }
            int latest = round == DoseRound.BEDTIME ? BEDTIME_LATEST_MINUTE : MEAL_LATEST_MINUTE;
            if (value > latest) {
                return TimeCheck.TOO_CLOSE;
            }
            if (previous != null && value - previous < FOUR_HOURS_MINUTES) {
                return TimeCheck.TOO_CLOSE;
            }
            previous = value;
        }
        return TimeCheck.OK;
    }

    public static Set<DoseRound> selectedRounds(EnvelopeDraft draft) {
        EnumSet<DoseRound> rounds = EnumSet.noneOf(DoseRound.class);
        if (draft == null) {
            return rounds;
        }
        for (MedicineDraft medicine : draft.medicines()) {
            rounds.addAll(medicine.rounds());
        }
        return rounds;
    }

    public static Set<DoseRound> selectedRounds(SavedEnvelope envelope) {
        EnumSet<DoseRound> rounds = EnumSet.noneOf(DoseRound.class);
        if (envelope == null) {
            return rounds;
        }
        for (SavedMedicine medicine : envelope.medicines()) {
            rounds.addAll(medicine.rounds());
        }
        return rounds;
    }

    public static SaveBlock validate(EnvelopeDraft draft) {
        if (draft == null || draft.medicines().isEmpty()) {
            return SaveBlock.MISSING_REQUIRED;
        }
        for (MedicineDraft medicine : draft.medicines()) {
            if (!hasRequiredFields(medicine)) {
                return SaveBlock.MISSING_REQUIRED;
            }
        }
        for (MedicineDraft medicine : draft.medicines()) {
            if (!medicine.isConfirmed()) {
                return SaveBlock.NOT_CONFIRMED;
            }
        }
        TimeCheck times = checkAlarmTimes(selectedRounds(draft), draft.alarmMinutes());
        if (times == TimeCheck.MISSING) {
            return SaveBlock.MISSING_TIME;
        }
        if (times == TimeCheck.TOO_CLOSE) {
            return SaveBlock.TOO_CLOSE;
        }
        return SaveBlock.OK;
    }

    public static boolean hasRequiredFields(MedicineDraft medicine) {
        if (medicine == null) {
            return false;
        }
        String name = medicine.getName();
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        if (!medicine.hasDose() || medicine.timings().isEmpty()) {
            return false;
        }
        if (medicine.rounds().isEmpty() || medicine.getStart() == null || medicine.getEnd() == null) {
            return false;
        }
        return !medicine.getEnd().isBefore(medicine.getStart());
    }

    public static boolean rangeAllowed(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        return days >= 1 && days <= HISTORY_MAX_DAYS;
    }

    public static LocalDate defaultHistoryStart(LocalDate today) {
        return today.minusDays(HISTORY_DEFAULT_DAYS - 1L);
    }

    public static HistoryPage history(
            List<DoseRecord> records,
            HistoryFilter filter,
            long startInclusive,
            long endExclusive,
            long now
    ) {
        List<DoseRecord> recorded = new ArrayList<>();
        for (DoseRecord record : records) {
            long scheduled = record.getScheduledAtMillis();
            if (scheduled < startInclusive || scheduled >= endExclusive) {
                continue;
            }
            if (displayStatus(record, now) == DoseStatus.SCHEDULED) {
                continue;
            }
            recorded.add(record);
        }
        List<DoseRecord> visible = new ArrayList<>();
        for (DoseRecord record : recorded) {
            DoseStatus status = displayStatus(record, now);
            if (filter == HistoryFilter.ALL
                    || (filter == HistoryFilter.COMPLETED && status == DoseStatus.COMPLETED)
                    || (filter == HistoryFilter.DELAYED && status == DoseStatus.DELAYED)
                    || (filter == HistoryFilter.MISSED && status == DoseStatus.MISSED)) {
                visible.add(record);
            }
        }
        return new HistoryPage(!recorded.isEmpty(), visible);
    }

    public static List<DoseRecord> onDate(List<DoseRecord> records, LocalDate date, ZoneId zone) {
        long start = date.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        List<DoseRecord> matched = new ArrayList<>();
        for (DoseRecord record : records) {
            long scheduled = record.getScheduledAtMillis();
            if (scheduled >= start && scheduled < end) {
                matched.add(record);
            }
        }
        return matched;
    }

    public static List<DoseRecord> remainingToday(List<DoseRecord> records, long now, ZoneId zone) {
        LocalDate today = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(now), zone);
        List<DoseRecord> remaining = new ArrayList<>();
        for (DoseRecord record : onDate(records, today, zone)) {
            if (displayStatus(record, now) == DoseStatus.SCHEDULED && record.getScheduledAtMillis() >= now) {
                remaining.add(record);
            }
        }
        return remaining;
    }

    /**
     * One visible row per envelope, round, and scheduled instant.
     * Stored records stay one per medicine.
     */
    public static List<DoseRecord> onePerEnvelopeRound(List<DoseRecord> records) {
        return onePerEnvelopeRound(records, Long.MIN_VALUE);
    }

    /**
     * Same grouping as {@link #onePerEnvelopeRound(List)}. When {@code now} is a real
     * clock, a still-scheduled record is kept ahead of a finished sibling.
     */
    public static List<DoseRecord> onePerEnvelopeRound(List<DoseRecord> records, long now) {
        List<DoseRecord> grouped = new ArrayList<>();
        java.util.Map<String, Integer> indexByKey = new java.util.LinkedHashMap<>();
        for (DoseRecord record : records) {
            String key = record.getEnvelopeId()
                    + "\u001f"
                    + record.getRound().name()
                    + "\u001f"
                    + record.getScheduledAtMillis();
            Integer existingIndex = indexByKey.get(key);
            if (existingIndex == null) {
                indexByKey.put(key, grouped.size());
                grouped.add(record);
                continue;
            }
            if (now == Long.MIN_VALUE) {
                continue;
            }
            DoseRecord existing = grouped.get(existingIndex);
            if (displayStatus(existing, now) != DoseStatus.SCHEDULED
                    && displayStatus(record, now) == DoseStatus.SCHEDULED) {
                grouped.set(existingIndex, record);
            }
        }
        return grouped;
    }

    public static LocalDate weekStart(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1L);
    }

    public static boolean sameWeek(LocalDate left, LocalDate right) {
        if (left == null || right == null) {
            return false;
        }
        return weekStart(left).equals(weekStart(right));
    }

    /**
     * On the registration's local date, a round is created only when its scheduled
     * instant is at or after registration. An equal time is kept. Other days are unchanged.
     */
    public static boolean includeScheduledRound(
            LocalDate day,
            long scheduledAtMillis,
            long registeredAtMillis,
            ZoneId zone
    ) {
        LocalDate registeredDay = java.time.Instant.ofEpochMilli(registeredAtMillis).atZone(zone).toLocalDate();
        if (!day.equals(registeredDay)) {
            return true;
        }
        return scheduledAtMillis >= registeredAtMillis;
    }

    public static List<DoseRecord> expand(SavedEnvelope envelope, SavedMedicine medicine, ZoneId zone) {
        List<DoseRecord> created = new ArrayList<>();
        if (medicine.getStart() == null || medicine.getEnd() == null) {
            return created;
        }
        LocalDate day = medicine.getStart();
        while (!day.isAfter(medicine.getEnd())) {
            for (DoseRound round : DoseRound.values()) {
                if (!medicine.rounds().contains(round)) {
                    continue;
                }
                Integer minute = envelope.alarmMinutes().get(round);
                if (minute == null) {
                    continue;
                }
                long scheduled = day.atStartOfDay(zone).plusMinutes(minute).toInstant().toEpochMilli();
                if (!includeScheduledRound(day, scheduled, envelope.getRegisteredAtMillis(), zone)) {
                    continue;
                }
                created.add(new DoseRecord(
                        envelope.getId() + ":" + medicine.getId() + ":" + day + ":" + round.name(),
                        envelope.getId(),
                        medicine.getId(),
                        envelope.getHospitalName(),
                        medicine.getName(),
                        round,
                        scheduled
                ));
            }
            day = day.plusDays(1);
        }
        return created;
    }
}
