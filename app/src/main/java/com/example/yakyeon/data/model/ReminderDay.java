package com.example.yakyeon.data.model;

import java.util.List;

public final class ReminderDay {

    private final String dateLabel;
    private final List<ReminderRecord> records;

    public ReminderDay(String dateLabel, List<ReminderRecord> records) {
        this.dateLabel = dateLabel;
        this.records = records;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public List<ReminderRecord> getRecords() {
        return records;
    }
}
