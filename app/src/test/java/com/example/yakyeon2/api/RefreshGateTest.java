package com.example.yakyeon2.api;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RefreshGateTest {

    @Test
    public void concurrentCallerWaitsForTheSingleRefresh() throws Exception {
        RefreshGate gate = new RefreshGate();
        AtomicInteger leads = new AtomicInteger();
        AtomicBoolean accessUnchanged = new AtomicBoolean(true);
        CountDownLatch leading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread leader = new Thread(() -> {
            try {
                RefreshGate.Outcome outcome = gate.enter(accessUnchanged::get);
                if (outcome.leader) {
                    leads.incrementAndGet();
                    leading.countDown();
                    assertTrue(release.await(2, TimeUnit.SECONDS));
                    accessUnchanged.set(false);
                    gate.succeed();
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        leader.start();
        assertTrue(leading.await(2, TimeUnit.SECONDS));
        java.util.concurrent.atomic.AtomicReference<RefreshGate.Outcome> follower = new java.util.concurrent.atomic.AtomicReference<>();
        Thread followerThread = new Thread(() -> {
            try {
                follower.set(gate.enter(accessUnchanged::get));
            } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
            }
        });
        followerThread.start();
        Thread.sleep(50);
        release.countDown();
        leader.join(2000);
        followerThread.join(2000);
        assertEquals(1, leads.get());
        assertFalse(follower.get().leader);
        assertNull(follower.get().error);
    }

    @Test
    public void replacedAccessTokenDoesNotStartAnotherRefresh() throws Exception {
        RefreshGate gate = new RefreshGate();
        assertTrue(gate.enter(() -> true).leader);
        gate.succeed();
        RefreshGate.Outcome later = gate.enter(() -> false);
        assertFalse(later.leader);
        assertNull(later.error);
    }

    @Test
    public void waitingCallerReceivesTheSameFailureAndALaterCallCanRetry() throws Exception {
        RefreshGate gate = new RefreshGate();
        CountDownLatch leading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread leader = new Thread(() -> {
            try {
                RefreshGate.Outcome outcome = gate.enter(() -> true);
                if (outcome.leader) {
                    leading.countDown();
                    assertTrue(release.await(2, TimeUnit.SECONDS));
                    gate.fail(ApiException.http(500, null, ""));
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        leader.start();
        assertTrue(leading.await(2, TimeUnit.SECONDS));
        Thread followerThread = new Thread(() -> {
            try {
                RefreshGate.Outcome outcome = gate.enter(() -> true);
                if (outcome.leader || outcome.error == null || outcome.error.status() != 500) {
                    throw new AssertionError("follower did not share the server failure");
                }
            } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
            }
        });
        followerThread.start();
        Thread.sleep(50);
        release.countDown();
        leader.join(2000);
        followerThread.join(2000);
        RefreshGate.Outcome retry = gate.enter(() -> true);
        assertTrue(retry.leader);
        gate.succeed();
    }

    @Test
    public void responseParseFailureDoesNotEndTheSession() {
        assertFalse(ApiException.response(200).endsSession());
        assertFalse(ApiException.disconnected().endsSession());
        assertFalse(ApiException.http(503, null, "").endsSession());
        assertTrue(ApiException.http(401, ApiCodes.INVALID_REFRESH_TOKEN, "").endsSession());
        assertTrue(ApiException.http(401, ApiCodes.REFRESH_TOKEN_REUSED, "").endsSession());
    }
}
