package com.example.yakyeon2.fcm;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PillboxSensorRefreshTest {

    @After
    public void reset() {
        PillboxSensorRefresh.resetForTest();
    }

    @Test
    public void stoppedActivityKeepsTheSignalUntilResume() {
        PillboxSensorRefresh.deliver();
        assertTrue(PillboxSensorRefresh.consumePending());
        assertFalse(PillboxSensorRefresh.consumePending());
    }

    @Test
    public void startedActivityRefreshesOncePerDelivery() {
        int[] calls = new int[] {0};
        PillboxSensorRefresh.Host host = () -> calls[0]++;
        PillboxSensorRefresh.attach(host);
        PillboxSensorRefresh.deliver();
        PillboxSensorRefresh.deliver();
        assertEquals(2, calls[0]);
        PillboxSensorRefresh.detach(host);
        PillboxSensorRefresh.deliver();
        assertEquals(2, calls[0]);
        assertTrue(PillboxSensorRefresh.consumePending());
    }

    @Test
    public void attachDoesNotReplayAPendingSignal() {
        PillboxSensorRefresh.deliver();
        int[] calls = new int[] {0};
        PillboxSensorRefresh.attach(() -> calls[0]++);
        assertEquals(0, calls[0]);
        assertTrue(PillboxSensorRefresh.consumePending());
    }
}
