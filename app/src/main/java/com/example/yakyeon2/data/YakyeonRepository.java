package com.example.yakyeon2.data;

public interface YakyeonRepository {

    YakyeonSession session();

    SignupDraft signupDraft();

    MedicationStore medications();

    MedicineNameCatalog medicineNames();
}
