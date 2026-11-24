package com.example.yakyeon2.fcm;

/**
 * Tells the started activity that an IR pouch signal arrived.
 * The messaging service does not construct or call {@code MainActivity}.
 * A stopped activity leaves the signal pending; resume reads the pillbox itself.
 */
public final class PillboxSensorRefresh {

    public interface Host {
        void onPillboxSensor();
    }

    private static Host host;
    private static boolean pending;

    private PillboxSensorRefresh() {
    }

    /**
     * Runs on the main thread. The service posts this after it has accepted the push.
     */
    public static void deliver() {
        Host current;
        synchronized (PillboxSensorRefresh.class) {
            current = host;
            if (current == null) {
                pending = true;
                trace("deliver listener=false pending=true");
                return;
            }
        }
        trace("deliver listener=true");
        current.onPillboxSensor();
    }

    public static void attach(Host next) {
        synchronized (PillboxSensorRefresh.class) {
            host = next;
        }
    }

    public static void detach(Host current) {
        synchronized (PillboxSensorRefresh.class) {
            if (host == current) {
                host = null;
            }
        }
    }

    public static boolean consumePending() {
        synchronized (PillboxSensorRefresh.class) {
            boolean was = pending;
            pending = false;
            return was;
        }
    }

    private static void trace(String message) {
        try {
            android.util.Log.i("PillboxSensor", message);
        } catch (RuntimeException ignored) {
            // JVM unit tests do not implement android.util.Log.
        }
    }

    static void resetForTest() {
        synchronized (PillboxSensorRefresh.class) {
            host = null;
            pending = false;
        }
    }
}
