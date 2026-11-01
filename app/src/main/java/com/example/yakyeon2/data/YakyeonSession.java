package com.example.yakyeon2.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class YakyeonSession {

    public static final int PILLBOX_SLOT_COUNT = 3;

    private AccountType accountType;
    private final boolean[] slotsOccupied = new boolean[PILLBOX_SLOT_COUNT];
    private boolean pillboxConnected;
    private CareLink careLink = CareLink.NONE;
    private final List<String> medicationIds = new ArrayList<>();
    private final NotificationPreferences notifications = new NotificationPreferences();

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public int slotCount() {
        return PILLBOX_SLOT_COUNT;
    }

    public int occupiedSlotCount() {
        int count = 0;
        for (boolean occupied : slotsOccupied) {
            if (occupied) {
                count++;
            }
        }
        return count;
    }

    public boolean isSlotOccupied(int index) {
        return slotsOccupied[index];
    }

    public void setSlotOccupied(int index, boolean occupied) {
        slotsOccupied[index] = occupied;
    }

    public boolean isPillboxConnected() {
        return pillboxConnected;
    }

    public void setPillboxConnected(boolean pillboxConnected) {
        this.pillboxConnected = pillboxConnected;
    }

    public CareLink getCareLink() {
        return careLink;
    }

    public void setCareLink(CareLink careLink) {
        this.careLink = careLink == null ? CareLink.NONE : careLink;
    }

    public boolean isCareLinked() {
        return careLink == CareLink.LINKED;
    }

    public void markCareDisconnected() {
        careLink = CareLink.DISCONNECTED;
    }

    public boolean receiveReconnectRequest() {
        if (careLink != CareLink.DISCONNECTED) {
            return false;
        }
        careLink = CareLink.REQUESTED;
        return true;
    }

    public boolean acceptReconnect() {
        if (careLink != CareLink.REQUESTED) {
            return false;
        }
        careLink = CareLink.LINKED;
        return true;
    }

    public boolean rejectReconnect() {
        if (careLink != CareLink.REQUESTED) {
            return false;
        }
        careLink = CareLink.DISCONNECTED;
        return true;
    }

    public List<String> medicationIds() {
        return Collections.unmodifiableList(medicationIds);
    }

    public void replaceMedicationIds(List<String> ids) {
        medicationIds.clear();
        if (ids != null) {
            medicationIds.addAll(ids);
        }
    }

    public NotificationPreferences notifications() {
        return notifications;
    }

    public enum CareLink {
        NONE,
        DISCONNECTED,
        REQUESTED,
        LINKED
    }
}
