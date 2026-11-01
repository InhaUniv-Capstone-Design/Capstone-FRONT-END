package com.example.yakyeon2.data;

public final class DoseRecord {

    private final String id;
    private final String envelopeId;
    private final String medicineId;
    private final String envelopeName;
    private final String medicineName;
    private final DoseRound round;
    private final long scheduledAtMillis;
    private DoseStatus manualStatus;
    private Long takenAtMillis;

    public DoseRecord(
            String id,
            String envelopeId,
            String medicineId,
            String envelopeName,
            String medicineName,
            DoseRound round,
            long scheduledAtMillis
    ) {
        this.id = id;
        this.envelopeId = envelopeId;
        this.medicineId = medicineId;
        this.envelopeName = envelopeName == null ? "" : envelopeName;
        this.medicineName = medicineName == null ? "" : medicineName;
        this.round = round;
        this.scheduledAtMillis = scheduledAtMillis;
    }

    public String getId() {
        return id;
    }

    public String getEnvelopeId() {
        return envelopeId;
    }

    public String getMedicineId() {
        return medicineId;
    }

    public String getEnvelopeName() {
        return envelopeName;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public DoseRound getRound() {
        return round;
    }

    public long getScheduledAtMillis() {
        return scheduledAtMillis;
    }

    public DoseStatus getManualStatus() {
        return manualStatus;
    }

    public Long getTakenAtMillis() {
        return takenAtMillis;
    }

    public void markDelayed(long takenAtMillis) {
        this.manualStatus = DoseStatus.DELAYED;
        this.takenAtMillis = takenAtMillis;
    }

    public void markCompleted(long takenAtMillis) {
        this.manualStatus = DoseStatus.COMPLETED;
        this.takenAtMillis = takenAtMillis;
    }

    void restoreManual(DoseStatus status, Long takenAtMillis) {
        this.manualStatus = status;
        this.takenAtMillis = takenAtMillis;
    }
}
