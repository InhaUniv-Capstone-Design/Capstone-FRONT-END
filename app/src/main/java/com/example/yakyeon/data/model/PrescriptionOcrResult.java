package com.example.yakyeon.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PrescriptionOcrResult {

    private static final Pattern STRENGTH = Pattern.compile("(\\d+(?:\\.\\d+)?mg)");

    private final String name;
    private final String dosage;
    private final double doseAmount;
    private final String doseUnit;
    private final String timesPerDay;
    private final String doseTimes;
    private final String doseTiming;
    private final String duration;
    private final boolean needsReview;
    private final String reviewNote;
    private final List<String> nameCandidates;

    public PrescriptionOcrResult(
            String name,
            String dosage,
            double doseAmount,
            String doseUnit,
            String timesPerDay,
            String doseTimes,
            String doseTiming,
            String duration,
            boolean needsReview,
            String reviewNote,
            List<String> nameCandidates
    ) {
        this.name = name;
        this.dosage = dosage == null ? "" : dosage;
        this.doseAmount = doseAmount;
        this.doseUnit = doseUnit == null ? "" : doseUnit;
        this.timesPerDay = timesPerDay == null ? "" : timesPerDay;
        this.doseTimes = doseTimes == null ? "" : doseTimes;
        this.doseTiming = doseTiming == null ? "" : doseTiming;
        this.duration = duration == null ? "" : duration;
        this.needsReview = needsReview;
        this.reviewNote = reviewNote == null ? "" : reviewNote;
        this.nameCandidates = nameCandidates == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(nameCandidates));
    }

    public String getName() {
        return name;
    }

    public String getDosage() {
        return dosage;
    }

    public double getDoseAmount() {
        return doseAmount;
    }

    public String getDoseUnit() {
        return doseUnit;
    }

    public String formatDose() {
        return Medication.formatAmount(doseAmount) + doseUnit;
    }

    public String formatSchedule() {
        return formatDose() + " · " + timesPerDay + " · " + doseTiming;
    }

    public PrescriptionOcrResult withEdits(
            String name,
            double doseAmount,
            String doseUnit,
            String timesPerDay,
            String doseTimes,
            String doseTiming,
            String duration
    ) {
        String nextName = name == null ? this.name : name;
        return new PrescriptionOcrResult(
                nextName,
                dosageFor(nextName, this.dosage),
                doseAmount,
                doseUnit == null ? "" : doseUnit,
                timesPerDay,
                doseTimes,
                doseTiming,
                duration,
                needsReview,
                reviewNote,
                nameCandidates
        );
    }

    private static String dosageFor(String name, String fallback) {
        Matcher matcher = STRENGTH.matcher(name == null ? "" : name);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return fallback;
    }

    public String getTimesPerDay() {
        return timesPerDay;
    }

    public String getDoseTimes() {
        return doseTimes;
    }

    public String getDoseTiming() {
        return doseTiming;
    }

    public String getDuration() {
        return duration;
    }

    public boolean needsReview() {
        return needsReview;
    }
}
