package com.example.yakyeon2.navigation;

public final class MedScreen {

    public static final String LIST = "med-list";
    public static final String CONSENT = "med-consent";
    public static final String ENVELOPE = "med-envelope";
    public static final String EDIT = "med-edit";
    public static final String SLOT = "med-slot";
    public static final String ALARM = "med-alarm";
    public static final String SCHEDULE = "med-schedule";
    public static final String HISTORY = "med-history";

    private MedScreen() {
    }

    public static boolean isMedication(String key) {
        return LIST.equals(key)
                || CONSENT.equals(key)
                || ENVELOPE.equals(key)
                || EDIT.equals(key)
                || SLOT.equals(key)
                || ALARM.equals(key)
                || SCHEDULE.equals(key)
                || HISTORY.equals(key);
    }
}
