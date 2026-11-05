package com.example.yakyeon2.api;

import com.example.yakyeon2.data.DoseRound;
import com.example.yakyeon2.data.DoseTiming;
import com.example.yakyeon2.data.EnvelopeDraft;
import com.example.yakyeon2.data.MedicineDraft;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HomeRegistration {

    public enum Block {
        NONE,
        INCOMPLETE
    }

    public static final class Prepared {
        public final Block block;
        public final JSONObject prescription;
        public final JSONObject schedule;
        public final Integer compartmentNo;

        private Prepared(Block block, JSONObject prescription, JSONObject schedule, Integer compartmentNo) {
            this.block = block;
            this.prescription = prescription;
            this.schedule = schedule;
            this.compartmentNo = compartmentNo;
        }
    }

    private HomeRegistration() {
    }

    public static Prepared prepare(EnvelopeDraft draft) {
        if (draft == null || draft.getHospitalName() == null || draft.getHospitalName().trim().isEmpty()) {
            return blocked(Block.INCOMPLETE);
        }
        if (draft.medicines().isEmpty()) {
            return blocked(Block.INCOMPLETE);
        }
        try {
            JSONArray medications = new JSONArray();
            LocalDate earliest = null;
            List<String> scheduleSlots = new ArrayList<>();
            List<String> scheduleTimes = new ArrayList<>();
            for (MedicineDraft medicine : draft.medicines()) {
                if (!medicine.isConfirmed() || medicine.getName() == null || medicine.getName().trim().isEmpty()) {
                    return blocked(Block.INCOMPLETE);
                }
                if (medicine.getName().trim().length() > 500) {
                    return blocked(Block.INCOMPLETE);
                }
                if (medicine.getStart() == null || medicine.getEnd() == null || medicine.getEnd().isBefore(medicine.getStart())) {
                    return blocked(Block.INCOMPLETE);
                }
                long days = ChronoUnit.DAYS.between(medicine.getStart(), medicine.getEnd()) + 1L;
                if (days < 1L || days > 365L) {
                    return blocked(Block.INCOMPLETE);
                }
                if (earliest == null || medicine.getStart().isBefore(earliest)) {
                    earliest = medicine.getStart();
                }
                List<String> slots = new ArrayList<>();
                if (!medicine.isPrn()) {
                    if (medicine.rounds().contains(DoseRound.MORNING)) {
                        slots.add(HomeContracts.SLOT_MORNING);
                    }
                    if (medicine.rounds().contains(DoseRound.LUNCH)) {
                        slots.add(HomeContracts.SLOT_LUNCH);
                    }
                    if (medicine.rounds().contains(DoseRound.EVENING)) {
                        slots.add(HomeContracts.SLOT_DINNER);
                    }
                    if (medicine.rounds().contains(DoseRound.BEDTIME)) {
                        slots.add(HomeContracts.SLOT_BEDTIME);
                    }
                    if (slots.isEmpty() || slots.size() > 4) {
                        return blocked(Block.INCOMPLETE);
                    }
                    for (String slot : slots) {
                        if (!scheduleSlots.contains(slot)) {
                            Integer minutes = minutesFor(draft, slot);
                            if (minutes == null) {
                                return blocked(Block.INCOMPLETE);
                            }
                            scheduleSlots.add(slot);
                            scheduleTimes.add(clock(minutes));
                        }
                    }
                }
                String dose = medicine.getDoseText();
                if (dose == null && medicine.getAmount() != null) {
                    dose = medicine.getAmount().quantityText();
                }
                String meal = medicine.getMealRelation();
                Integer offset = medicine.getMealOffsetMin();
                if (meal == null) {
                    boolean before = medicine.timings().contains(DoseTiming.BEFORE_MEAL);
                    boolean after = medicine.timings().contains(DoseTiming.AFTER_MEAL_30);
                    if (before && !after) {
                        meal = "BEFORE_MEAL";
                        offset = 0;
                    } else if (after && !before) {
                        meal = "AFTER_MEAL";
                        offset = 30;
                    }
                }
                medications.put(HomeContracts.medicationBody(
                        medicine.getDrugId(),
                        medicine.getName(),
                        dose,
                        medicine.isPrn(),
                        slots,
                        meal,
                        offset,
                        (int) days,
                        medicine.getStart().toString()
                ));
            }
            String prescribed = draft.getPrescribedDate();
            if (prescribed == null || prescribed.isEmpty()) {
                // Estimate from the earliest start date. This is not a date read off a prescription.
                // An OCR prescribed date already stored on the draft is kept above.
                prescribed = earliest.toString();
            }
            JSONObject prescription = new JSONObject();
            prescription.put("hospital_name", draft.getHospitalName().trim());
            if (draft.getPharmacyName() == null || draft.getPharmacyName().trim().isEmpty()) {
                prescription.put("pharmacy_name", JSONObject.NULL);
            } else {
                prescription.put("pharmacy_name", draft.getPharmacyName().trim());
            }
            prescription.put("prescribed_date", prescribed);
            prescription.put("source", draft.isFromOcr() ? HomeContracts.SOURCE_OCR : HomeContracts.SOURCE_MANUAL);
            prescription.put("medications", medications);
            Integer compartment = null;
            if (draft.getSlotIndex() != null) {
                int number = draft.getSlotIndex() + 1;
                if (number < 1 || number > 3) {
                    return blocked(Block.INCOMPLETE);
                }
                compartment = number;
            }
            return new Prepared(
                    Block.NONE,
                    prescription,
                    scheduleSlots.isEmpty() ? null : HomeContracts.scheduleBody(scheduleSlots, scheduleTimes),
                    compartment
            );
        } catch (ApiException | JSONException exception) {
            return blocked(Block.INCOMPLETE);
        }
    }

    private static Prepared blocked(Block block) {
        return new Prepared(block, null, null, null);
    }

    private static Integer minutesFor(EnvelopeDraft draft, String slot) {
        DoseRound round;
        if (HomeContracts.SLOT_MORNING.equals(slot)) {
            round = DoseRound.MORNING;
        } else if (HomeContracts.SLOT_LUNCH.equals(slot)) {
            round = DoseRound.LUNCH;
        } else if (HomeContracts.SLOT_DINNER.equals(slot)) {
            round = DoseRound.EVENING;
        } else if (HomeContracts.SLOT_BEDTIME.equals(slot)) {
            round = DoseRound.BEDTIME;
        } else {
            return null;
        }
        return draft.alarmMinutes().get(round);
    }

    private static String clock(int minutes) {
        int hour = Math.floorMod(minutes, 24 * 60) / 60;
        int minute = Math.floorMod(minutes, 60);
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }
}
