package com.example.yakyeon.data.model;

public final class DrugGraphEdge {

    private final String interactionId;
    private final String sourceMedicationId;
    private final String targetMedicationId;

    public DrugGraphEdge(String interactionId, String sourceMedicationId, String targetMedicationId) {
        this.interactionId = interactionId;
        this.sourceMedicationId = sourceMedicationId;
        this.targetMedicationId = targetMedicationId;
    }

    public String getInteractionId() {
        return interactionId;
    }

    public String getSourceMedicationId() {
        return sourceMedicationId;
    }

    public String getTargetMedicationId() {
        return targetMedicationId;
    }
}
