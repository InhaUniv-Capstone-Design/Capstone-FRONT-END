package com.example.yakyeon.data.model;

public final class ReminderRecord {

    public static final String FILTER_ALL = "전체";
    public static final String STATUS_ON_TIME = "정상";
    public static final String STATUS_USER_COMPLETED = "정상*";
    public static final String STATUS_DELAYED = "지연";
    public static final String STATUS_MISSED = "누락";

    private final String id;
    private final String dateLabel;
    private final String clock;
    private final String medicationName;
    private final String status;

    public ReminderRecord(
            String id,
            String dateLabel,
            String clock,
            String medicationName,
            String status
    ) {
        this.id = id;
        this.dateLabel = dateLabel;
        this.clock = clock;
        this.medicationName = medicationName;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public String getClock() {
        return clock;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public String getStatus() {
        return status;
    }

    public boolean isCompleted() {
        return STATUS_ON_TIME.equals(status) || STATUS_USER_COMPLETED.equals(status);
    }

    public boolean isUserCompleted() {
        return STATUS_USER_COMPLETED.equals(status);
    }

    public ReminderRecord withStatus(String status) {
        return new ReminderRecord(id, dateLabel, clock, medicationName, status);
    }
}
