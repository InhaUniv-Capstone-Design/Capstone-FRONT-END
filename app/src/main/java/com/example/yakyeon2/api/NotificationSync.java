package com.example.yakyeon2.api;

import com.example.yakyeon2.data.NotificationPreferences;

import java.util.ArrayList;
import java.util.List;

public final class NotificationSync {

    public static final String DOSE_REMINDER = "DOSE_REMINDER";
    public static final String MISSED = "MISSED";
    public static final String END_SOON = "END_SOON";
    public static final String DEVICE = "DEVICE";
    public static final String GUARDIAN_OPEN = "GUARDIAN_OPEN";
    public static final String GUARDIAN_TAKEN = "GUARDIAN_TAKEN";
    public static final String GUARDIAN_MISSED = "GUARDIAN_MISSED";
    public static final String GUARDIAN_DEVICE = "GUARDIAN_DEVICE";

    public static String[] keys() {
        String[] copy = new String[KEYS.length];
        System.arraycopy(KEYS, 0, copy, 0, KEYS.length);
        return copy;
    }

    public static int indexOf(String key) {
        for (int index = 0; index < KEYS.length; index++) {
            if (KEYS[index].equals(key)) {
                return index;
            }
        }
        return -1;
    }

    private static final String[] KEYS = {
            DOSE_REMINDER,
            MISSED,
            END_SOON,
            DEVICE,
            GUARDIAN_OPEN,
            GUARDIAN_TAKEN,
            GUARDIAN_MISSED,
            GUARDIAN_DEVICE
    };

    private NotificationSync() {
    }

    public static final class Item {
        public final String key;
        public final boolean enabled;

        public Item(String key, boolean enabled) {
            this.key = key;
            this.enabled = enabled;
        }
    }

    public static boolean[] capture(NotificationPreferences preferences) {
        return new boolean[]{
                preferences.isDoseTime(),
                preferences.isMissedDose(),
                preferences.isEndingSoon(),
                preferences.isPillboxStatus(),
                preferences.isBoxOpened(),
                preferences.isDoseButton(),
                preferences.isMissedByDevice(),
                preferences.isDeviceFault()
        };
    }

    public static List<Item> diff(boolean[] before, NotificationPreferences after) {
        List<Item> changes = new ArrayList<>();
        if (before == null || before.length != KEYS.length) {
            return changes;
        }
        boolean[] now = capture(after);
        for (int index = 0; index < KEYS.length; index++) {
            if (before[index] != now[index]) {
                changes.add(new Item(KEYS[index], now[index]));
            }
        }
        return changes;
    }

    public static void apply(List<Item> items, NotificationPreferences preferences) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Boolean dose = null;
        Boolean missed = null;
        Boolean ending = null;
        Boolean device = null;
        Boolean open = null;
        Boolean taken = null;
        Boolean missedDevice = null;
        Boolean fault = null;
        for (Item item : items) {
            if (item == null || item.key == null) {
                continue;
            }
            switch (item.key) {
                case DOSE_REMINDER:
                    dose = item.enabled;
                    break;
                case MISSED:
                    missed = item.enabled;
                    break;
                case END_SOON:
                    ending = item.enabled;
                    break;
                case DEVICE:
                    device = item.enabled;
                    break;
                case GUARDIAN_OPEN:
                    open = item.enabled;
                    break;
                case GUARDIAN_TAKEN:
                    taken = item.enabled;
                    break;
                case GUARDIAN_MISSED:
                    missedDevice = item.enabled;
                    break;
                case GUARDIAN_DEVICE:
                    fault = item.enabled;
                    break;
                default:
                    break;
            }
        }
        boolean[] current = capture(preferences);
        preferences.applyServerFlags(
                dose != null ? dose : current[0],
                missed != null ? missed : current[1],
                ending != null ? ending : current[2],
                device != null ? device : current[3],
                open != null ? open : current[4],
                taken != null ? taken : current[5],
                missedDevice != null ? missedDevice : current[6],
                fault != null ? fault : current[7]
        );
    }
}
