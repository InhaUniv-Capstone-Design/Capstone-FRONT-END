package com.example.yakyeon2.fcm;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BoxTakenRefreshTest {

    @After
    public void reset() {
        BoxTakenRefresh.resetForTest();
    }

    @Test
    public void onlyASignedInTakenPushRequestsReload() {
        assertTrue(BoxTakenRefresh.requestsReload(PushMessages.Kind.BOX_TAKEN, true));
        assertFalse(BoxTakenRefresh.requestsReload(PushMessages.Kind.BOX_TAKEN, false));
        assertFalse(BoxTakenRefresh.requestsReload(PushMessages.Kind.MISSED_DOSE, true));
        assertFalse(BoxTakenRefresh.requestsReload(PushMessages.Kind.PASSTHROUGH, true));
        assertTrue(BoxTakenRefresh.requestsReload(PushMessages.Kind.PASSTHROUGH, true, true));
        assertFalse(BoxTakenRefresh.requestsReload(PushMessages.Kind.PASSTHROUGH, false, true));
        assertFalse(BoxTakenRefresh.requestsReload(PushMessages.Kind.MISSED_DOSE, true, false));
        assertFalse(BoxTakenRefresh.requestsReload(PushMessages.Kind.DOSE_REMINDER, true, false));
    }

    @Test
    public void stoppedActivityKeepsTheSignalUntilResume() {
        BoxTakenRefresh.deliver();
        assertTrue(BoxTakenRefresh.consumePending());
        assertFalse(BoxTakenRefresh.consumePending());
    }

    @Test
    public void startedActivityRefreshesOncePerDelivery() {
        int[] calls = new int[] {0};
        BoxTakenRefresh.Host host = () -> calls[0]++;
        BoxTakenRefresh.attach(host);
        BoxTakenRefresh.deliver();
        BoxTakenRefresh.deliver();
        assertEquals(2, calls[0]);
        BoxTakenRefresh.detach(host);
        BoxTakenRefresh.deliver();
        assertEquals(2, calls[0]);
        assertTrue(BoxTakenRefresh.consumePending());
    }

    @Test
    public void attachDoesNotReplayAPendingSignal() {
        BoxTakenRefresh.deliver();
        int[] calls = new int[] {0};
        BoxTakenRefresh.attach(() -> calls[0]++);
        assertEquals(0, calls[0]);
        assertTrue(BoxTakenRefresh.consumePending());
    }
}
