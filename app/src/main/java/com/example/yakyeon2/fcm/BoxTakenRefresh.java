package com.example.yakyeon2.fcm;

/**
 * Tells the started activity that a pillbox dose was confirmed.
 * The messaging service does not construct or call {@code MainActivity}.
 * A stopped activity leaves the signal pending; resume reads the server itself.
 */
public final class BoxTakenRefresh {

    public interface Host {
        void onBoxTaken();
    }

    private static Host host;
    private static boolean pending;

    private BoxTakenRefresh() {
    }

    public static boolean requestsReload(PushMessages.Kind kind, boolean signedIn) {
        return requestsReload(kind, signedIn, false);
    }

    public static boolean requestsReload(PushMessages.Kind kind, boolean signedIn, boolean intakeCopy) {
        return signedIn && (kind == PushMessages.Kind.BOX_TAKEN || intakeCopy);
    }

    /**
     * Runs on the main thread. The service posts this after it has accepted the push.
     */
    public static void deliver() {
        Host current;
        synchronized (BoxTakenRefresh.class) {
            current = host;
            if (current == null) {
                pending = true;
                trace("deliver listener=false pending=true");
                return;
            }
        }
        trace("deliver listener=true");
        current.onBoxTaken();
    }

    public static void attach(Host next) {
        synchronized (BoxTakenRefresh.class) {
            host = next;
        }
    }

    public static void detach(Host current) {
        synchronized (BoxTakenRefresh.class) {
            if (host == current) {
                host = null;
            }
        }
    }

    public static boolean consumePending() {
        synchronized (BoxTakenRefresh.class) {
            boolean was = pending;
            pending = false;
            return was;
        }
    }

    private static void trace(String message) {
        try {
            android.util.Log.i("BoxTaken", message);
        } catch (RuntimeException ignored) {
            // JVM unit tests do not implement android.util.Log.
        }
    }

    static void resetForTest() {
        synchronized (BoxTakenRefresh.class) {
            host = null;
            pending = false;
        }
    }
}
