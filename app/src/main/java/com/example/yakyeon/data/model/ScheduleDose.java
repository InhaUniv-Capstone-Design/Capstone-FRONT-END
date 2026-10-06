package com.example.yakyeon.data.model;

public final class ScheduleDose {

    public static final String PLANNED = "복용 예정";
    public static final String DONE = "복용 완료";
    public static final String MISSED = "미복용";

    private final String medicationName;
    private final String doseLabel;
    private final String clock;
    private final String timeLabel;
    private final String status;

    public ScheduleDose(
            String medicationName,
            String doseLabel,
            String clock,
            String timeLabel,
            String status
    ) {
        this.medicationName = medicationName;
        this.doseLabel = doseLabel;
        this.clock = clock;
        this.timeLabel = timeLabel;
        this.status = status;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public String getDoseLabel() {
        return doseLabel;
    }

    public String getClock() {
        return clock;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public String getStatus() {
        return status;
    }
}
