package com.example.yakyeon.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PillboxMedicine {

    private final String id;
    private final String name;
    private final String frequency;
    private final boolean asNeeded;
    private final List<String> alarmTimes;

    public PillboxMedicine(
            String id,
            String name,
            String frequency,
            boolean asNeeded,
            List<String> alarmTimes
    ) {
        this.id = id;
        this.name = name;
        this.frequency = frequency;
        this.asNeeded = asNeeded;
        this.alarmTimes = Collections.unmodifiableList(new ArrayList<>(alarmTimes));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFrequency() {
        return frequency;
    }

    public boolean isAsNeeded() {
        return asNeeded;
    }

    public List<String> getAlarmTimes() {
        return alarmTimes;
    }

    public boolean hasUnsetAlarm() {
        for (String time : alarmTimes) {
            if (time == null || time.length() == 0) {
                return true;
            }
        }
        return false;
    }
}
