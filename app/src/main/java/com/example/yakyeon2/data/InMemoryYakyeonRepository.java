package com.example.yakyeon2.data;

public final class InMemoryYakyeonRepository implements YakyeonRepository {

    private final YakyeonSession session = new YakyeonSession();
    private final SignupDraft signupDraft = new SignupDraft();
    private final MedicationStore medications = new MedicationStore(session);
    private final MedicineNameCatalog medicineNames = new MedicineNameCatalog();

    @Override
    public YakyeonSession session() {
        return session;
    }

    @Override
    public SignupDraft signupDraft() {
        return signupDraft;
    }

    @Override
    public MedicationStore medications() {
        return medications;
    }

    @Override
    public MedicineNameCatalog medicineNames() {
        return medicineNames;
    }
}
