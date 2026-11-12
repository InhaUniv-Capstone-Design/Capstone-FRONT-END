package com.example.yakyeon2.data;

/**
 * Display rules for meal timing. Stored values are not rewritten here.
 */
public final class MealTimingText {

    public static final String AFTER_LABEL = "식후";

    private MealTimingText() {
    }

    public static boolean displaysAsAfterMeal(String stored) {
        if (stored == null) {
            return false;
        }
        String trimmed = stored.trim();
        if ("AFTER_MEAL".equals(trimmed)) {
            return true;
        }
        String compact = trimmed.replace(" ", "").replace("\u00A0", "");
        return "식후".equals(compact)
                || "식후30분".equals(compact)
                || "식사직후".equals(compact)
                || "식사후".equals(compact);
    }

    /**
     * Label for a stored phrase. After-meal variants display as {@link #AFTER_LABEL}.
     * Other phrases, including {@code 식전 30분}, are returned unchanged.
     */
    public static String displayPhrase(String stored) {
        if (stored == null) {
            return null;
        }
        if (displaysAsAfterMeal(stored)) {
            return AFTER_LABEL;
        }
        return stored;
    }
}
