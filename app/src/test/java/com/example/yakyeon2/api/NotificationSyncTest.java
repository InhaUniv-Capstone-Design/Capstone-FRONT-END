package com.example.yakyeon2.api;

import com.example.yakyeon2.data.NotificationPreferences;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotificationSyncTest {

    @Test
    public void pillboxOffSendsDeviceAndGuardianKeys() {
        NotificationPreferences preferences = new NotificationPreferences();
        boolean[] before = NotificationSync.capture(preferences);
        preferences.setPillboxStatus(false);
        List<NotificationSync.Item> changes = NotificationSync.diff(before, preferences);
        assertEquals(5, changes.size());
        assertTrue(has(changes, NotificationSync.DEVICE, false));
        assertTrue(has(changes, NotificationSync.GUARDIAN_OPEN, false));
        assertTrue(has(changes, NotificationSync.GUARDIAN_TAKEN, false));
        assertTrue(has(changes, NotificationSync.GUARDIAN_MISSED, false));
        assertTrue(has(changes, NotificationSync.GUARDIAN_DEVICE, false));
    }

    @Test
    public void applyUsesServerKeysAndIgnoresUnknown() {
        NotificationPreferences preferences = new NotificationPreferences();
        NotificationSync.apply(Arrays.asList(
                new NotificationSync.Item(NotificationSync.DOSE_REMINDER, false),
                new NotificationSync.Item("NOT_A_KEY", true),
                new NotificationSync.Item(NotificationSync.DEVICE, false)
        ), preferences);
        assertFalse(preferences.isDoseTime());
        assertTrue(preferences.isMissedDose());
        assertFalse(preferences.isPillboxStatus());
    }

    @Test
    public void emptyApplyLeavesDefaults() {
        NotificationPreferences preferences = new NotificationPreferences();
        NotificationSync.apply(Collections.emptyList(), preferences);
        assertTrue(preferences.isDoseTime());
        assertTrue(preferences.isDeviceFault());
    }

    private static boolean has(List<NotificationSync.Item> changes, String key, boolean enabled) {
        for (NotificationSync.Item change : changes) {
            if (key.equals(change.key) && change.enabled == enabled) {
                return true;
            }
        }
        return false;
    }
}
