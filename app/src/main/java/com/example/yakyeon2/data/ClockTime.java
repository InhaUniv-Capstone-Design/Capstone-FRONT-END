package com.example.yakyeon2.data;

import java.util.Locale;

/**
 * Minute-of-day clock. Twelve-hour labels are display only; comparisons use minutes.
 */
public final class ClockTime {

    private ClockTime() {
    }

    public static int normalize(int minutes) {
        int value = minutes % MedicationRules.MINUTES_PER_DAY;
        if (value < 0) {
            value += MedicationRules.MINUTES_PER_DAY;
        }
        return value;
    }

    public static int of24(int hour, int minute) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("hour and minute out of range");
        }
        return hour * 60 + minute;
    }

    /**
     * @param afternoon {@code false} is 오전, {@code true} is 오후
     * @param hour12    1 through 12. 12 오전 is midnight and 12 오후 is noon.
     */
    public static int of12(boolean afternoon, int hour12, int minute) {
        if (hour12 < 1 || hour12 > 12 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("hour and minute out of range");
        }
        int hour24;
        if (hour12 == 12) {
            hour24 = afternoon ? 12 : 0;
        } else {
            hour24 = afternoon ? hour12 + 12 : hour12;
        }
        return of24(hour24, minute);
    }

    public static int hour24(int minutes) {
        return normalize(minutes) / 60;
    }

    public static int minute(int minutes) {
        return normalize(minutes) % 60;
    }

    public static boolean isAfternoon(int minutes) {
        return hour24(minutes) >= 12;
    }

    public static int shiftHour(int minutes, int deltaHours) {
        int hour = Math.floorMod(hour24(minutes) + deltaHours, 24);
        return of24(hour, minute(minutes));
    }

    public static int shiftMinute(int minutes, int deltaMinutes) {
        int next = Math.floorMod(minute(minutes) + deltaMinutes, 60);
        return of24(hour24(minutes), next);
    }

    public static String format12(int minutes) {
        int hour = hour24(minutes);
        int shown = hour % 12;
        if (shown == 0) {
            shown = 12;
        }
        String period = isAfternoon(minutes) ? "오후" : "오전";
        return period + " " + shown + ":" + String.format(Locale.KOREAN, "%02d", minute(minutes));
    }

    public static String formatHour24(int minutes) {
        return String.valueOf(hour24(minutes));
    }

    public static String formatHm(int minutes) {
        return hour24(minutes) + ":" + String.format(Locale.KOREAN, "%02d", minute(minutes));
    }

    public static String formatMinute(int minutes) {
        return String.format(Locale.KOREAN, "%02d", minute(minutes));
    }
}
