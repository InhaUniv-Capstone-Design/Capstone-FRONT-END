package com.example.yakyeon2.data;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;

/**
 * Prescriptions for the 2026-10-27 scenario. Names 약1–약9 are labels, not real drugs.
 * This class does not create DUR relations or a Wi-Fi connection.
 *
 * Dose amount is not in the source table. Commit requires one, so every medicine uses
 * {@link DoseAmount#TABLET_1}. 약5 stores both {@link DoseTiming#AFTER_MEAL_30} and
 * {@link DoseTiming#BEDTIME}. Those timings do not add rounds. The four rounds come
 * from the morning, lunch, evening, and bedtime round set.
 */
public final class ScenarioPrescriptions {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static final LocalDate REFERENCE_DAY = LocalDate.of(2026, 10, 27);

    public static final String CLINIC = "한의원";
    public static final String INTERNAL = "내과";
    public static final String DERMATOLOGY = "피부과";

    public static final LocalDate CLINIC_START = LocalDate.of(2026, 10, 13);
    public static final LocalDate CLINIC_END = LocalDate.of(2026, 10, 30);
    public static final LocalDate INTERNAL_START = LocalDate.of(2026, 10, 17);
    public static final LocalDate INTERNAL_END = LocalDate.of(2026, 10, 31);
    public static final LocalDate DERMATOLOGY_START = LocalDate.of(2026, 10, 27);
    public static final LocalDate DERMATOLOGY_END = LocalDate.of(2026, 11, 2);

    /** Display slot 1. Index 0 is what {@code MedicationStore} stores. */
    public static final int CLINIC_SLOT = 0;
    public static final int INTERNAL_SLOT = 1;
    public static final int DERMATOLOGY_SLOT = 2;

    private ScenarioPrescriptions() {
    }

    public static MedicationStore knownStore() {
        return knownStore(new YakyeonSession());
    }

    public static MedicationStore knownStore(YakyeonSession session) {
        MedicationStore store = new MedicationStore(session);
        installKnown(store);
        return store;
    }

    public static boolean installKnown(MedicationStore store) {
        boolean clinic = installClinic(store);
        boolean internal = installInternal(store);
        return clinic || internal;
    }

    public static boolean installClinic(MedicationStore store) {
        if (hasHospital(store, CLINIC) || blocked(store, CLINIC_SLOT, CLINIC)) {
            return false;
        }
        EnvelopeDraft draft = store.beginDraft();
        draft.setHospitalName(CLINIC);
        draft.setSlotIndex(CLINIC_SLOT);
        draft.alarmMinutes().put(DoseRound.MORNING, minutes(9, 0));
        draft.alarmMinutes().put(DoseRound.LUNCH, minutes(13, 30));
        draft.alarmMinutes().put(DoseRound.EVENING, minutes(20, 0));
        addMedicine(store, "약1", DoseTiming.AFTER_MEAL_30, CLINIC_START, CLINIC_END, meals());
        addMedicine(store, "약2", DoseTiming.AFTER_MEAL_30, CLINIC_START, CLINIC_END, meals());
        addMedicine(store, "약3", DoseTiming.AFTER_MEAL_30, CLINIC_START, CLINIC_END, meals());
        confirmAll(draft);
        if (store.commit(ZONE, startOf(CLINIC_START)) != MedicationRules.SaveBlock.OK) {
            throw new IllegalStateException("clinic commit failed");
        }
        return true;
    }

    public static boolean installInternal(MedicationStore store) {
        if (hasHospital(store, INTERNAL) || blocked(store, INTERNAL_SLOT, INTERNAL)) {
            return false;
        }
        EnvelopeDraft draft = store.beginDraft();
        draft.setHospitalName(INTERNAL);
        draft.setSlotIndex(INTERNAL_SLOT);
        draft.alarmMinutes().put(DoseRound.MORNING, minutes(8, 30));
        draft.alarmMinutes().put(DoseRound.LUNCH, minutes(14, 0));
        draft.alarmMinutes().put(DoseRound.EVENING, minutes(19, 0));
        draft.alarmMinutes().put(DoseRound.BEDTIME, minutes(23, 0));
        addMedicine(store, "약4", DoseTiming.BEFORE_MEAL, INTERNAL_START, INTERNAL_END, meals());
        EnumSet<DoseRound> four = meals();
        four.add(DoseRound.BEDTIME);
        addMedicine(store, "약5", DoseTiming.AFTER_MEAL_30, INTERNAL_START, INTERNAL_END, four)
                .timings()
                .add(DoseTiming.BEDTIME);
        addMedicine(
                store,
                "약6",
                DoseTiming.AFTER_MEAL_30,
                INTERNAL_START,
                INTERNAL_END,
                EnumSet.of(DoseRound.MORNING, DoseRound.EVENING)
        );
        confirmAll(draft);
        if (store.commit(ZONE, startOf(INTERNAL_START)) != MedicationRules.SaveBlock.OK) {
            throw new IllegalStateException("internal commit failed");
        }
        return true;
    }

    public static boolean installDermatology(MedicationStore store) {
        if (hasHospital(store, DERMATOLOGY) || blocked(store, DERMATOLOGY_SLOT, DERMATOLOGY)) {
            return false;
        }
        EnvelopeDraft draft = store.beginDraft();
        draft.setHospitalName(DERMATOLOGY);
        draft.setSlotIndex(DERMATOLOGY_SLOT);
        draft.alarmMinutes().put(DoseRound.MORNING, minutes(7, 30));
        draft.alarmMinutes().put(DoseRound.LUNCH, minutes(12, 30));
        draft.alarmMinutes().put(DoseRound.EVENING, minutes(19, 0));
        addMedicine(store, "약7", DoseTiming.AFTER_MEAL_30, DERMATOLOGY_START, DERMATOLOGY_END, meals());
        addMedicine(store, "약8", DoseTiming.AFTER_MEAL_30, DERMATOLOGY_START, DERMATOLOGY_END, meals());
        addMedicine(
                store,
                "약9",
                DoseTiming.AFTER_MEAL_30,
                DERMATOLOGY_START,
                DERMATOLOGY_END,
                EnumSet.of(DoseRound.EVENING)
        );
        confirmAll(draft);
        if (store.commit(ZONE, startOf(DERMATOLOGY_START)) != MedicationRules.SaveBlock.OK) {
            throw new IllegalStateException("dermatology commit failed");
        }
        return true;
    }

    public static int removeScenario(MedicationStore store) {
        int removed = 0;
        boolean found = true;
        while (found) {
            found = false;
            for (String hospital : new String[] {CLINIC, INTERNAL, DERMATOLOGY}) {
                SavedEnvelope envelope = envelope(store, hospital);
                if (envelope != null && store.removeEnvelope(envelope.getId())) {
                    removed++;
                    found = true;
                }
            }
        }
        return removed;
    }

    public static boolean hasHospital(MedicationStore store, String hospital) {
        return envelope(store, hospital) != null;
    }

    public static SavedEnvelope envelope(MedicationStore store, String hospital) {
        for (SavedEnvelope envelope : store.envelopes()) {
            if (hospital.equals(envelope.getHospitalName())) {
                return envelope;
            }
        }
        return null;
    }

    public static SavedMedicine medicine(MedicationStore store, String name) {
        for (SavedEnvelope envelope : store.envelopes()) {
            for (SavedMedicine medicine : envelope.medicines()) {
                if (name.equals(medicine.getName())) {
                    return medicine;
                }
            }
        }
        return null;
    }

    public static long startOf(LocalDate day) {
        return day.atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    public static int minutes(int hour, int minute) {
        return hour * 60 + minute;
    }

    private static boolean blocked(MedicationStore store, int slot, String hospital) {
        if (draftInProgress(store)) {
            return true;
        }
        for (SavedEnvelope envelope : store.envelopes()) {
            if (envelope.isStopped() || hospital.equals(envelope.getHospitalName())) {
                continue;
            }
            Integer taken = envelope.getSlotIndex();
            if (taken != null && taken == slot) {
                return true;
            }
        }
        return false;
    }

    private static boolean draftInProgress(MedicationStore store) {
        EnvelopeDraft draft = store.draft();
        if (draft == null) {
            return false;
        }
        if (draft.isDevSample() || draft.isFixturePractice()) {
            return true;
        }
        if (draft.getHospitalName() != null && !draft.getHospitalName().trim().isEmpty()) {
            return true;
        }
        if (!draft.medicines().isEmpty() || !draft.alarmMinutes().isEmpty() || draft.getSlotIndex() != null) {
            return true;
        }
        return draft.isCollectionChecked() || draft.isNoticeChecked();
    }

    private static EnumSet<DoseRound> meals() {
        return EnumSet.of(DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING);
    }

    private static MedicineDraft addMedicine(
            MedicationStore store,
            String name,
            DoseTiming timing,
            LocalDate start,
            LocalDate end,
            EnumSet<DoseRound> rounds
    ) {
        MedicineDraft medicine = store.addMedicine(start);
        medicine.setName(name);
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.setTiming(timing);
        medicine.setEnd(end);
        medicine.rounds().addAll(rounds);
        return medicine;
    }

    private static void confirmAll(EnvelopeDraft draft) {
        for (MedicineDraft medicine : draft.medicines()) {
            medicine.setConfirmed(true);
        }
    }
}
