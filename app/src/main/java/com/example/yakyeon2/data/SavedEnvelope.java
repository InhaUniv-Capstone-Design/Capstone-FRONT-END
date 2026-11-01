package com.example.yakyeon2.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SavedEnvelope {

    private final String id;
    private final String hospitalName;
    private Integer slotIndex;
    private final EnumMap<DoseRound, Integer> alarmMinutes = new EnumMap<>(DoseRound.class);
    private final List<SavedMedicine> medicines = new ArrayList<>();
    private boolean stopped;
    private long registeredAtMillis;

    public SavedEnvelope(String id, String hospitalName, Integer slotIndex, Map<DoseRound, Integer> alarmMinutes) {
        this.id = id;
        this.hospitalName = hospitalName == null ? "" : hospitalName;
        this.slotIndex = slotIndex;
        if (alarmMinutes != null) {
            this.alarmMinutes.putAll(alarmMinutes);
        }
    }

    public String getId() {
        return id;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public Integer getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(Integer slotIndex) {
        this.slotIndex = slotIndex;
    }

    public Map<DoseRound, Integer> alarmMinutes() {
        return Collections.unmodifiableMap(alarmMinutes);
    }

    public void replaceAlarmMinutes(Map<DoseRound, Integer> next) {
        alarmMinutes.clear();
        if (next != null) {
            alarmMinutes.putAll(next);
        }
    }

    public List<SavedMedicine> medicines() {
        return medicines;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public long getRegisteredAtMillis() {
        return registeredAtMillis;
    }

    public void setRegisteredAtMillis(long registeredAtMillis) {
        this.registeredAtMillis = registeredAtMillis;
    }
}
