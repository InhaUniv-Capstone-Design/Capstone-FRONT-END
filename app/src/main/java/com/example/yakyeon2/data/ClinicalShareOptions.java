package com.example.yakyeon2.data;

import java.time.LocalDate;

public final class ClinicalShareOptions {

    public static final int DAYS_7 = 7;
    public static final int DAYS_30 = 30;
    public static final int DAYS_90 = 90;

    private int periodDays = DAYS_30;
    private boolean includeRecords = true;
    private boolean includeSpan = true;
    private boolean includeDur = true;
    private boolean includeAdherence = true;
    private boolean includeCurrent = true;
    private boolean includeFinished = true;
    private Long startEpoch;
    private Long endEpoch;

    public int getPeriodDays() {
        if (startEpoch != null && endEpoch != null) {
            return (int) (endEpoch - startEpoch + 1);
        }
        return periodDays;
    }

    public boolean setPeriodDays(int days) {
        if (days != DAYS_7 && days != DAYS_30 && days != DAYS_90) {
            return false;
        }
        periodDays = days;
        startEpoch = null;
        endEpoch = null;
        return true;
    }

    public boolean hasCustomRange() {
        return startEpoch != null && endEpoch != null;
    }

    public boolean setPeriodRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return false;
        }
        long count = end.toEpochDay() - start.toEpochDay() + 1;
        if (count < 1 || count > 3660) {
            return false;
        }
        startEpoch = start.toEpochDay();
        endEpoch = end.toEpochDay();
        periodDays = (int) count;
        return true;
    }

    public LocalDate resolveStart(LocalDate today) {
        if (hasCustomRange()) {
            return LocalDate.ofEpochDay(startEpoch);
        }
        LocalDate end = today == null ? LocalDate.now() : today;
        return end.minusDays(Math.max(periodDays, 1) - 1L);
    }

    public LocalDate resolveEnd(LocalDate today) {
        if (hasCustomRange()) {
            return LocalDate.ofEpochDay(endEpoch);
        }
        return today == null ? LocalDate.now() : today;
    }

    public boolean isIncludeRecords() {
        return includeRecords;
    }

    public void setIncludeRecords(boolean includeRecords) {
        this.includeRecords = includeRecords;
    }

    public boolean isIncludeSpan() {
        return includeSpan;
    }

    public void setIncludeSpan(boolean includeSpan) {
        this.includeSpan = includeSpan;
    }

    public boolean isIncludeDur() {
        return includeDur;
    }

    public void setIncludeDur(boolean includeDur) {
        this.includeDur = includeDur;
    }

    public boolean isIncludeAdherence() {
        return includeAdherence;
    }

    public void setIncludeAdherence(boolean includeAdherence) {
        this.includeAdherence = includeAdherence;
    }

    public boolean isIncludeCurrent() {
        return includeCurrent;
    }

    public void setIncludeCurrent(boolean includeCurrent) {
        this.includeCurrent = includeCurrent;
    }

    public boolean isIncludeFinished() {
        return includeFinished;
    }

    public void setIncludeFinished(boolean includeFinished) {
        this.includeFinished = includeFinished;
    }

    public boolean includesCurrentMedicines() {
        return includeCurrent;
    }

    public String exportState() {
        String head = hasCustomRange()
                ? startEpoch + "~" + endEpoch
                : Integer.toString(periodDays);
        return head + "," + flags();
    }

    public boolean restoreState(String raw) {
        if (raw == null) {
            return false;
        }
        int comma = raw.indexOf(',');
        if (comma < 1) {
            return false;
        }
        String head = raw.substring(0, comma);
        String flags = raw.substring(comma + 1);
        if (!validFlags(flags)) {
            return false;
        }
        if (head.indexOf('~') >= 0) {
            int tilde = head.indexOf('~');
            long start;
            long end;
            try {
                start = Long.parseLong(head.substring(0, tilde));
                end = Long.parseLong(head.substring(tilde + 1));
            } catch (NumberFormatException exception) {
                return false;
            }
            if (!setPeriodRange(LocalDate.ofEpochDay(start), LocalDate.ofEpochDay(end))) {
                return false;
            }
        } else {
            int days;
            try {
                days = Integer.parseInt(head);
            } catch (NumberFormatException exception) {
                return false;
            }
            if (!setPeriodDays(days)) {
                return false;
            }
        }
        applyFlags(flags);
        return true;
    }

    private String flags() {
        return bit(includeRecords) + bit(includeSpan) + bit(includeDur) + bit(includeAdherence)
                + bit(includeCurrent) + bit(includeFinished);
    }

    private void applyFlags(String flags) {
        includeRecords = flags.charAt(0) == '1';
        includeSpan = flags.charAt(1) == '1';
        includeDur = flags.charAt(2) == '1';
        includeAdherence = flags.charAt(3) == '1';
        if (flags.length() == 6) {
            includeCurrent = flags.charAt(4) == '1';
            includeFinished = flags.charAt(5) == '1';
        } else {
            includeCurrent = true;
            includeFinished = true;
        }
    }

    private static boolean validFlags(String flags) {
        if (flags.length() != 4 && flags.length() != 6) {
            return false;
        }
        for (int index = 0; index < flags.length(); index++) {
            char mark = flags.charAt(index);
            if (mark != '0' && mark != '1') {
                return false;
            }
        }
        return true;
    }

    private static String bit(boolean value) {
        return value ? "1" : "0";
    }
}
