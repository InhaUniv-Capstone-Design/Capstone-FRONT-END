package com.example.yakyeon.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PillboxSession {

    private final String displayName;
    private final String connectionDetail;
    private final String batteryLabel;
    private final String syncLabel;
    private final boolean notificationsEnabled;
    private final boolean simulated;
    private final List<PillboxSlot> slots;

    public PillboxSession(
            String displayName,
            String connectionDetail,
            String batteryLabel,
            String syncLabel,
            boolean notificationsEnabled,
            boolean simulated,
            List<PillboxSlot> slots
    ) {
        this.displayName = displayName;
        this.connectionDetail = connectionDetail;
        this.batteryLabel = batteryLabel;
        this.syncLabel = syncLabel;
        this.notificationsEnabled = notificationsEnabled;
        this.simulated = simulated;
        this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getConnectionDetail() {
        return connectionDetail;
    }

    public String getBatteryLabel() {
        return batteryLabel;
    }

    public String getSyncLabel() {
        return syncLabel;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public List<PillboxSlot> getSlots() {
        return slots;
    }
}
