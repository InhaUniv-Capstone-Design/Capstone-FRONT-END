package com.example.yakyeon2.data;

/**
 * Stored dose. Tablet and capsule values follow the confirmed list.
 * Pouch, mL, and g reuse that tablet number list because no other range is confirmed.
 * New names are appended so older saved names still load.
 */
public enum DoseAmount {
    TABLET_HALF,
    TABLET_1,
    TABLET_2,
    TABLET_3,
    CAPSULE_1,
    CAPSULE_2,
    POUCH_HALF,
    POUCH_1,
    POUCH_2,
    POUCH_3,
    ML_HALF,
    ML_1,
    ML_2,
    ML_3,
    G_HALF,
    G_1,
    G_2,
    G_3;

    public static final String UNIT_TABLET = "정";
    public static final String UNIT_CAPSULE = "캡슐";
    public static final String UNIT_POUCH = "포";
    public static final String UNIT_ML = "mL";
    public static final String UNIT_G = "g";

    public String quantityText() {
        switch (this) {
            case TABLET_HALF:
            case POUCH_HALF:
            case ML_HALF:
            case G_HALF:
                return "0.5";
            case TABLET_2:
            case CAPSULE_2:
            case POUCH_2:
            case ML_2:
            case G_2:
                return "2";
            case TABLET_3:
            case POUCH_3:
            case ML_3:
            case G_3:
                return "3";
            case TABLET_1:
            case CAPSULE_1:
            case POUCH_1:
            case ML_1:
            case G_1:
            default:
                return "1";
        }
    }

    public String unitText() {
        switch (this) {
            case CAPSULE_1:
            case CAPSULE_2:
                return UNIT_CAPSULE;
            case POUCH_HALF:
            case POUCH_1:
            case POUCH_2:
            case POUCH_3:
                return UNIT_POUCH;
            case ML_HALF:
            case ML_1:
            case ML_2:
            case ML_3:
                return UNIT_ML;
            case G_HALF:
            case G_1:
            case G_2:
            case G_3:
                return UNIT_G;
            case TABLET_HALF:
            case TABLET_1:
            case TABLET_2:
            case TABLET_3:
            default:
                return UNIT_TABLET;
        }
    }

    public static String[] units() {
        return new String[] {UNIT_TABLET, UNIT_CAPSULE, UNIT_POUCH, UNIT_ML, UNIT_G};
    }

    /**
     * Keypad text that matches a stored quantity. {@code 1.0} and {@code 0.50} count as 1 and 0.5.
     */
    public static String canonicalQuantity(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        java.math.BigDecimal value;
        try {
            value = new java.math.BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (value.compareTo(new java.math.BigDecimal("0.5")) == 0) {
            return "0.5";
        }
        if (value.compareTo(java.math.BigDecimal.ONE) == 0) {
            return "1";
        }
        if (value.compareTo(new java.math.BigDecimal("2")) == 0) {
            return "2";
        }
        if (value.compareTo(new java.math.BigDecimal("3")) == 0) {
            return "3";
        }
        return null;
    }

    public static String[] quantitiesFor(String unit) {
        if (UNIT_CAPSULE.equals(unit)) {
            return new String[] {"1", "2"};
        }
        return new String[] {"0.5", "1", "2", "3"};
    }

    public static DoseAmount from(String unit, String quantity) {
        if (unit == null || quantity == null) {
            return null;
        }
        if (UNIT_TABLET.equals(unit)) {
            return tablet(quantity);
        }
        if (UNIT_CAPSULE.equals(unit)) {
            if ("1".equals(quantity)) {
                return CAPSULE_1;
            }
            if ("2".equals(quantity)) {
                return CAPSULE_2;
            }
            return null;
        }
        if (UNIT_POUCH.equals(unit)) {
            return pouch(quantity);
        }
        if (UNIT_ML.equals(unit)) {
            return milliliter(quantity);
        }
        if (UNIT_G.equals(unit)) {
            return gram(quantity);
        }
        return null;
    }

    private static DoseAmount tablet(String quantity) {
        if ("0.5".equals(quantity)) {
            return TABLET_HALF;
        }
        if ("1".equals(quantity)) {
            return TABLET_1;
        }
        if ("2".equals(quantity)) {
            return TABLET_2;
        }
        if ("3".equals(quantity)) {
            return TABLET_3;
        }
        return null;
    }

    private static DoseAmount pouch(String quantity) {
        if ("0.5".equals(quantity)) {
            return POUCH_HALF;
        }
        if ("1".equals(quantity)) {
            return POUCH_1;
        }
        if ("2".equals(quantity)) {
            return POUCH_2;
        }
        if ("3".equals(quantity)) {
            return POUCH_3;
        }
        return null;
    }

    private static DoseAmount milliliter(String quantity) {
        if ("0.5".equals(quantity)) {
            return ML_HALF;
        }
        if ("1".equals(quantity)) {
            return ML_1;
        }
        if ("2".equals(quantity)) {
            return ML_2;
        }
        if ("3".equals(quantity)) {
            return ML_3;
        }
        return null;
    }

    private static DoseAmount gram(String quantity) {
        if ("0.5".equals(quantity)) {
            return G_HALF;
        }
        if ("1".equals(quantity)) {
            return G_1;
        }
        if ("2".equals(quantity)) {
            return G_2;
        }
        if ("3".equals(quantity)) {
            return G_3;
        }
        return null;
    }
}
