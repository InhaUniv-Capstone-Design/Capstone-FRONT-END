package com.example.yakyeon2.data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Today's home card in the caller's time zone.
 * Upcoming rows are every untaken dose still after {@code now} and not later than
 * today's bedtime. Bedtime is the latest BEDTIME dose that day. When that dose is
 * absent, the existing latest bedtime minute in {@link MedicationRules} is the end.
 * The day is done when every dose on or before that bedtime is taken or recorded as missed.
 * A dose still waiting to be checked keeps the day open.
 * Doses from other calendar days are left out of the card.
 */
public final class HomeDoseFocus {

    public enum Outcome {
        EMPTY,
        DONE,
        ACTIVE
    }

    public static final class Entry {
        public final String place;
        public final String slot;
        public final long scheduledAtMillis;
        public final boolean taken;
        /** Recorded miss. A dose still inside the check window is not missed. */
        public final boolean missed;

        public Entry(String place, String slot, long scheduledAtMillis, boolean taken) {
            this(place, slot, scheduledAtMillis, taken, false);
        }

        public Entry(String place, String slot, long scheduledAtMillis, boolean taken, boolean missed) {
            this.place = place == null ? "" : place;
            this.slot = slot == null ? "" : slot;
            this.scheduledAtMillis = scheduledAtMillis;
            this.taken = taken;
            this.missed = missed && !taken;
        }

        public boolean bedtime() {
            return "BEDTIME".equals(slot);
        }
    }

    public static final class Board {
        public final Outcome outcome;
        public final List<Entry> overdue;
        public final List<Entry> upcoming;

        private Board(Outcome outcome, List<Entry> overdue, List<Entry> upcoming) {
            this.outcome = outcome;
            this.overdue = overdue;
            this.upcoming = upcoming;
        }
    }

    private HomeDoseFocus() {
    }

    public static long bedtimeMillis(List<Entry> doses, long now, ZoneId zone) {
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        long latest = Long.MIN_VALUE;
        boolean found = false;
        if (doses != null) {
            for (Entry entry : doses) {
                if (entry == null || !entry.bedtime()) {
                    continue;
                }
                LocalDate day = Instant.ofEpochMilli(entry.scheduledAtMillis).atZone(zone).toLocalDate();
                if (!today.equals(day)) {
                    continue;
                }
                if (!found || entry.scheduledAtMillis > latest) {
                    latest = entry.scheduledAtMillis;
                    found = true;
                }
            }
        }
        if (found) {
            return latest;
        }
        return today.atStartOfDay(zone)
                .plusMinutes(MedicationRules.BEDTIME_LATEST_MINUTE)
                .toInstant()
                .toEpochMilli();
    }

    public static Board focus(List<Entry> doses, long now, ZoneId zone) {
        List<Entry> overdue = new ArrayList<>();
        List<Entry> upcoming = new ArrayList<>();
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        long bedtime = bedtimeMillis(doses, now, zone);
        boolean any = false;
        if (doses != null) {
            for (Entry entry : doses) {
                if (entry == null) {
                    continue;
                }
                LocalDate day = Instant.ofEpochMilli(entry.scheduledAtMillis).atZone(zone).toLocalDate();
                if (!today.equals(day) || entry.scheduledAtMillis > bedtime) {
                    continue;
                }
                any = true;
                if (entry.taken || entry.missed) {
                    continue;
                }
                if (entry.scheduledAtMillis > now) {
                    upcoming.add(entry);
                } else {
                    overdue.add(entry);
                }
            }
        }
        if (!any) {
            return new Board(Outcome.EMPTY, Collections.emptyList(), Collections.emptyList());
        }
        if (overdue.isEmpty() && upcoming.isEmpty()) {
            return new Board(Outcome.DONE, Collections.emptyList(), Collections.emptyList());
        }
        Collections.sort(overdue, (left, right) -> Long.compare(left.scheduledAtMillis, right.scheduledAtMillis));
        Collections.sort(upcoming, (left, right) -> Long.compare(left.scheduledAtMillis, right.scheduledAtMillis));
        return new Board(Outcome.ACTIVE, overdue, upcoming);
    }

    /** Every dose scheduled on the calendar day of {@code now}, taken doses included. */
    public static List<Entry> scheduledToday(List<Entry> doses, long now, ZoneId zone) {
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        List<Entry> rows = new ArrayList<>();
        if (doses != null) {
            for (Entry entry : doses) {
                if (entry == null) {
                    continue;
                }
                LocalDate day = Instant.ofEpochMilli(entry.scheduledAtMillis).atZone(zone).toLocalDate();
                if (!today.equals(day)) {
                    continue;
                }
                rows.add(entry);
            }
        }
        Collections.sort(rows, (left, right) -> Long.compare(left.scheduledAtMillis, right.scheduledAtMillis));
        return rows;
    }
}
