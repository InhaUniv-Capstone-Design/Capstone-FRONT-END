package com.example.yakyeon2.api;

/**
 * One refresh at a time. Callers that arrive while a refresh is running wait for that
 * result. A caller whose access token was already replaced does not send another refresh.
 */
final class RefreshGate {

    interface AccessProbe {
        boolean unchanged();
    }

    static final class Outcome {
        final boolean leader;
        final ApiException error;

        private Outcome(boolean leader, ApiException error) {
            this.leader = leader;
            this.error = error;
        }

        static Outcome lead() {
            return new Outcome(true, null);
        }

        static Outcome joined(ApiException error) {
            return new Outcome(false, error);
        }
    }

    private boolean refreshing;
    private boolean rotated;
    private ApiException error;

    synchronized Outcome enter(AccessProbe probe) throws InterruptedException {
        if (refreshing) {
            while (refreshing) {
                wait();
            }
            if (rotated || !probe.unchanged()) {
                return Outcome.joined(null);
            }
            if (error != null) {
                return Outcome.joined(error);
            }
        } else if (!probe.unchanged()) {
            return Outcome.joined(null);
        }
        refreshing = true;
        error = null;
        rotated = false;
        return Outcome.lead();
    }

    synchronized void succeed() {
        refreshing = false;
        rotated = true;
        error = null;
        notifyAll();
    }

    synchronized void fail(ApiException exception) {
        refreshing = false;
        rotated = false;
        error = exception;
        notifyAll();
    }
}
