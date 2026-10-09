package com.example.yakyeon.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PillboxSlot {

    public enum Status {
        NORMAL,
        OPEN_ERROR
    }

    private final int number;
    private final String prescriptionName;
    private final String pharmacyLabel;
    private final Status status;
    private final String errorMessage;
    private final List<PillboxMedicine> medicines;

    public PillboxSlot(
            int number,
            String prescriptionName,
            String pharmacyLabel,
            Status status,
            String errorMessage,
            List<PillboxMedicine> medicines
    ) {
        this.number = number;
        this.prescriptionName = prescriptionName;
        this.pharmacyLabel = pharmacyLabel;
        this.status = status;
        this.errorMessage = errorMessage;
        this.medicines = Collections.unmodifiableList(new ArrayList<>(medicines));
    }

    public int getNumber() {
        return number;
    }

    public String getPrescriptionName() {
        return prescriptionName;
    }

    public String getPharmacyLabel() {
        return pharmacyLabel;
    }

    public Status getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<PillboxMedicine> getMedicines() {
        return medicines;
    }

    public boolean hasUnsetAlarm() {
        for (PillboxMedicine medicine : medicines) {
            if (medicine.hasUnsetAlarm()) {
                return true;
            }
        }
        return false;
    }
}
