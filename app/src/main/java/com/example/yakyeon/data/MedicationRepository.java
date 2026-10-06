package com.example.yakyeon.data;

import com.example.yakyeon.data.model.Medication;

import java.util.List;

public interface MedicationRepository {

    List<Medication> getMedications();

    List<Medication> getActiveMedications();

    List<Medication> getEndedMedications();

    int countActive();

    Medication findById(String id);

    boolean updateMedication(
            String id,
            String name,
            double doseAmount,
            String doseUnit,
            String timesPerDay,
            String doseTimes,
            String doseTiming,
            String duration
    );

    boolean deleteMedication(String id);

    boolean endMedication(String id);

    void updateExpiredMedications();

    String addMedication(
            String name,
            String dosage,
            double doseAmount,
            String doseUnit,
            String timesPerDay,
            String doseTimes,
            String doseTiming,
            String duration
    );
}
