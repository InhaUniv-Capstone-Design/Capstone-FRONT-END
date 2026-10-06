package com.example.yakyeon.data.model;

public final class Medication {

    public enum Classification {
        ACTIVE,
        ENDED
    }

    public enum Status {
        ACTIVE,
        COMPLETED
    }

    public enum Form {
        TABLET,
        PILL
    }

    private final String id;
    private final String name;
    private final String dosage;
    private final String frequency;
    private final Classification classification;
    private final Form form;
    private final String category;
    private final String drugClass;
    private final String usage;
    private final String sideEffects;
    private final String precautions;
    private final double doseAmount;
    private final String doseUnit;
    private final String timesPerDay;
    private final String doseTimes;
    private final String doseTiming;
    private final String duration;

    public Medication(
            String id,
            String name,
            String dosage,
            String frequency,
            Classification classification,
            Form form,
            String category,
            String drugClass,
            String usage,
            String sideEffects,
            String precautions,
            double doseAmount,
            String doseUnit,
            String timesPerDay,
            String doseTimes,
            String doseTiming,
            String duration
    ) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.classification = classification;
        this.form = form;
        this.category = category;
        this.drugClass = drugClass;
        this.usage = usage;
        this.sideEffects = sideEffects;
        this.precautions = precautions;
        this.doseAmount = doseAmount;
        this.doseUnit = doseUnit == null ? "" : doseUnit;
        this.timesPerDay = timesPerDay;
        this.doseTimes = doseTimes;
        this.doseTiming = doseTiming;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDosage() {
        return dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public Classification getClassification() {
        return classification;
    }

    public Status getStatus() {
        return classification == Classification.ENDED ? Status.COMPLETED : Status.ACTIVE;
    }

    public Form getForm() {
        return form;
    }

    public String getCategory() {
        return category;
    }

    public String getDrugClass() {
        return drugClass;
    }

    public String getUsage() {
        return usage;
    }

    public String getSideEffects() {
        return sideEffects;
    }

    public String getPrecautions() {
        return precautions;
    }

    public double getDoseAmount() {
        return doseAmount;
    }

    public String getDoseUnit() {
        return doseUnit;
    }

    public String formatDose() {
        return formatAmount(doseAmount) + doseUnit;
    }

    public static String formatAmount(double amount) {
        if (Double.compare(amount, Math.rint(amount)) == 0) {
            return Long.toString(Math.round(amount));
        }
        return Double.toString(amount);
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

    public Medication withSchedule(
            String name,
            double doseAmount,
            String doseUnit,
            String timesPerDay,
            String doseTimes,
            String doseTiming,
            String duration,
            String frequency,
            String usage
    ) {
        return new Medication(
                id,
                name,
                dosage,
                frequency,
                classification,
                form,
                category,
                drugClass,
                usage,
                sideEffects,
                precautions,
                doseAmount,
                doseUnit,
                timesPerDay,
                doseTimes,
                doseTiming,
                duration
        );
    }

    public Medication ended(String endedFrequency) {
        return new Medication(
                id,
                name,
                dosage,
                endedFrequency,
                Classification.ENDED,
                form,
                category,
                drugClass,
                usage,
                sideEffects,
                precautions,
                doseAmount,
                doseUnit,
                timesPerDay,
                doseTimes,
                doseTiming,
                duration
        );
    }
}
