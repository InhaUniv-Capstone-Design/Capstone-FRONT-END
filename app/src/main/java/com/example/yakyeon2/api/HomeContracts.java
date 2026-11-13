package com.example.yakyeon2.api;

import com.example.yakyeon2.data.MealTimingText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HomeContracts {

    public static final String MED_INFO = "MED_INFO";
    public static final String DOSE_RECORD = "DOSE_RECORD";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_UPCOMING = "UPCOMING";
    public static final String STATUS_ENDED = "ENDED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_DELAYED = "DELAYED";
    public static final String STATUS_MISSED = "MISSED";
    public static final String SLOT_MORNING = "MORNING";
    public static final String SLOT_LUNCH = "LUNCH";
    public static final String SLOT_DINNER = "DINNER";
    public static final String SLOT_BEDTIME = "BEDTIME";
    public static final String STATE_NONE = "NONE";
    public static final String STATE_PENDING = "PENDING";
    public static final String STATE_DONE = "DONE";
    public static final String STATE_MISSED = "MISSED";
    public static final String SOURCE_OCR = "OCR";
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final int IMAGE_MAX_BYTES = 10 * 1024 * 1024;
    public static final int HISTORY_MAX_DAYS = 90;
    public static final int AUTOCOMPLETE_LIMIT = 10;
    public static final int SEARCH_MAX_CHARS = 50;

    private static final ZoneOffset SEOUL = ZoneOffset.ofHours(9);

    private HomeContracts() {
    }

    public static final class Section<T> {
        public enum Kind {
            READY,
            WITHHELD,
            FAILED
        }

        public final Kind kind;
        public final T value;
        public final String message;

        private Section(Kind kind, T value, String message) {
            this.kind = kind;
            this.value = value;
            this.message = message;
        }

        public static <T> Section<T> ready(T value) {
            return new Section<>(Kind.READY, value, null);
        }

        public static <T> Section<T> withheld(String message) {
            return new Section<>(Kind.WITHHELD, null, message);
        }

        public static <T> Section<T> failed(String message) {
            return new Section<>(Kind.FAILED, null, message);
        }
    }

    public static final class NamedDrug {
        public final long medicationId;
        public final String drugName;

        private NamedDrug(long medicationId, String drugName) {
            this.medicationId = medicationId;
            this.drugName = drugName;
        }
    }

    public static final class DoseDrug {
        public final long medicationId;
        public final String drugName;
        public final String dosePerIntake;

        private DoseDrug(long medicationId, String drugName, String dosePerIntake) {
            this.medicationId = medicationId;
            this.drugName = drugName;
            this.dosePerIntake = dosePerIntake;
        }
    }

    public static final class SlotState {
        public final String timingSlot;
        public final String state;

        private SlotState(String timingSlot, String state) {
            this.timingSlot = timingSlot;
            this.state = state;
        }
    }

    public static final class NextDose {
        public final long scheduledDoseId;
        public final String hospitalName;
        public final String timingSlot;
        public final String scheduledAt;
        public final String status;
        public final List<NamedDrug> medications;

        private NextDose(
                long scheduledDoseId,
                String hospitalName,
                String timingSlot,
                String scheduledAt,
                String status,
                List<NamedDrug> medications
        ) {
            this.scheduledDoseId = scheduledDoseId;
            this.hospitalName = hospitalName;
            this.timingSlot = timingSlot;
            this.scheduledAt = scheduledAt;
            this.status = status;
            this.medications = medications;
        }
    }

    /**
     * One row of {@code unchecked_doses} or {@code upcoming_doses}.
     * The same timing slot stays as separate rows when the scheduled times differ.
     */
    public static final class SummaryDose {
        public final long scheduledDoseId;
        public final String hospitalName;
        public final String timingSlot;
        public final String scheduledAt;
        public final List<NamedDrug> medications;

        private SummaryDose(
                long scheduledDoseId,
                String hospitalName,
                String timingSlot,
                String scheduledAt,
                List<NamedDrug> medications
        ) {
            this.scheduledDoseId = scheduledDoseId;
            this.hospitalName = hospitalName;
            this.timingSlot = timingSlot;
            this.scheduledAt = scheduledAt;
            this.medications = medications;
        }
    }

    public static final class SlotProgress {
        public final int done;
        public final int scheduled;

        private SlotProgress(int done, int scheduled) {
            this.done = done;
            this.scheduled = scheduled;
        }
    }

    public static final class HomeSummary {
        public final String date;
        public final long total;
        public final long done;
        public final long normal;
        public final long delayed;
        public final long missed;
        public final long pending;
        public final List<SlotState> slots;
        public final NextDose nextDose;
        public final String serverTime;
        public final List<SummaryDose> uncheckedDoses;
        public final List<SummaryDose> upcomingDoses;
        public final Boolean allDone;
        /** True when the response includes the newer schedule lists or {@code all_done}. */
        public final boolean serverBoard;

        private HomeSummary(
                String date,
                long total,
                long done,
                long normal,
                long delayed,
                long missed,
                long pending,
                List<SlotState> slots,
                NextDose nextDose,
                String serverTime,
                List<SummaryDose> uncheckedDoses,
                List<SummaryDose> upcomingDoses,
                Boolean allDone,
                boolean serverBoard
        ) {
            this.date = date;
            this.total = total;
            this.done = done;
            this.normal = normal;
            this.delayed = delayed;
            this.missed = missed;
            this.pending = pending;
            this.slots = slots;
            this.nextDose = nextDose;
            this.serverTime = serverTime;
            this.uncheckedDoses = uncheckedDoses;
            this.upcomingDoses = upcomingDoses;
            this.allDone = allDone;
            this.serverBoard = serverBoard;
        }
    }

    public static final class Dose {
        public final long scheduledDoseId;
        public final long prescriptionId;
        public final String hospitalName;
        public final String timingSlot;
        public final String scheduledAt;
        public final String normalUntil;
        public final String closeAt;
        public final String status;
        public final String takenAt;
        public final String decisionSource;
        public final boolean corrected;
        public final List<DoseDrug> medications;

        private Dose(
                long scheduledDoseId,
                long prescriptionId,
                String hospitalName,
                String timingSlot,
                String scheduledAt,
                String normalUntil,
                String closeAt,
                String status,
                String takenAt,
                String decisionSource,
                boolean corrected,
                List<DoseDrug> medications
        ) {
            this.scheduledDoseId = scheduledDoseId;
            this.prescriptionId = prescriptionId;
            this.hospitalName = hospitalName;
            this.timingSlot = timingSlot;
            this.scheduledAt = scheduledAt;
            this.normalUntil = normalUntil;
            this.closeAt = closeAt;
            this.status = status;
            this.takenAt = takenAt;
            this.decisionSource = decisionSource;
            this.corrected = corrected;
            this.medications = medications;
        }
    }

    public static final class Medication {
        public final long medicationId;
        public final long prescriptionId;
        public final String hospitalName;
        public final Long drugId;
        public final String drugName;
        public final String manufacturer;
        public final String matchStatus;
        public final String dosePerIntake;
        public final boolean prn;
        public final long timesPerDay;
        public final List<String> timingSlots;
        public final String mealRelation;
        public final Long mealOffsetMin;
        public final long totalDays;
        public final String startDate;
        public final String endDate;
        public final String endedOn;
        public final String status;

        private Medication(
                long medicationId,
                long prescriptionId,
                String hospitalName,
                Long drugId,
                String drugName,
                String manufacturer,
                String matchStatus,
                String dosePerIntake,
                boolean prn,
                long timesPerDay,
                List<String> timingSlots,
                String mealRelation,
                Long mealOffsetMin,
                long totalDays,
                String startDate,
                String endDate,
                String endedOn,
                String status
        ) {
            this.medicationId = medicationId;
            this.prescriptionId = prescriptionId;
            this.hospitalName = hospitalName;
            this.drugId = drugId;
            this.drugName = drugName;
            this.manufacturer = manufacturer;
            this.matchStatus = matchStatus;
            this.dosePerIntake = dosePerIntake;
            this.prn = prn;
            this.timesPerDay = timesPerDay;
            this.timingSlots = timingSlots;
            this.mealRelation = mealRelation;
            this.mealOffsetMin = mealOffsetMin;
            this.totalDays = totalDays;
            this.startDate = startDate;
            this.endDate = endDate;
            this.endedOn = endedOn;
            this.status = status;
        }
    }

    public static final class Prescription {
        public final long prescriptionId;
        public final String hospitalName;
        public final String pharmacyName;
        public final String prescribedDate;
        public final List<Medication> medications;

        private Prescription(
                long prescriptionId,
                String hospitalName,
                String pharmacyName,
                String prescribedDate,
                List<Medication> medications
        ) {
            this.prescriptionId = prescriptionId;
            this.hospitalName = hospitalName;
            this.pharmacyName = pharmacyName;
            this.prescribedDate = prescribedDate;
            this.medications = medications;
        }
    }

    public static final class DrugSuggestion {
        public final long drugId;
        public final String productName;
        public final String manufacturer;

        private DrugSuggestion(long drugId, String productName, String manufacturer) {
            this.drugId = drugId;
            this.productName = productName;
            this.manufacturer = manufacturer;
        }
    }

    public static final class RecognizedMedication {
        public final String drugName;
        public final Long matchedDrugId;
        public final List<DrugSuggestion> candidates;
        public final String dosePerIntake;
        public final Long timesPerDay;
        public final List<String> timingSlots;
        public final Long totalDays;
        public final boolean prn;
        public final String mealRelation;
        public final Long mealOffsetMin;

        private RecognizedMedication(
                String drugName,
                Long matchedDrugId,
                List<DrugSuggestion> candidates,
                String dosePerIntake,
                Long timesPerDay,
                List<String> timingSlots,
                Long totalDays,
                boolean prn,
                String mealRelation,
                Long mealOffsetMin
        ) {
            this.drugName = drugName;
            this.matchedDrugId = matchedDrugId;
            this.candidates = candidates;
            this.dosePerIntake = dosePerIntake;
            this.timesPerDay = timesPerDay;
            this.timingSlots = timingSlots;
            this.totalDays = totalDays;
            this.prn = prn;
            this.mealRelation = mealRelation;
            this.mealOffsetMin = mealOffsetMin;
        }
    }

    public static final class OcrDraft {
        public final String hospitalName;
        public final String pharmacyName;
        public final String prescribedDate;
        public final List<RecognizedMedication> recognized;

        private OcrDraft(
                String hospitalName,
                String pharmacyName,
                String prescribedDate,
                List<RecognizedMedication> recognized
        ) {
            this.hospitalName = hospitalName;
            this.pharmacyName = pharmacyName;
            this.prescribedDate = prescribedDate;
            this.recognized = recognized;
        }
    }

    public static final class CreatedPrescription {
        public final long prescriptionId;
        public final List<Long> medicationIds;

        private CreatedPrescription(long prescriptionId, List<Long> medicationIds) {
            this.prescriptionId = prescriptionId;
            this.medicationIds = medicationIds;
        }
    }

    public static final class ScheduleSetting {
        public final String timingSlot;
        public final String doseTime;
        public final long normalWindowMin;
        public final long closeAfterMin;

        private ScheduleSetting(String timingSlot, String doseTime, long normalWindowMin, long closeAfterMin) {
            this.timingSlot = timingSlot;
            this.doseTime = doseTime;
            this.normalWindowMin = normalWindowMin;
            this.closeAfterMin = closeAfterMin;
        }
    }

    public static final class ScheduleSettings {
        public final long prescriptionId;
        public final List<ScheduleSetting> settings;

        private ScheduleSettings(long prescriptionId, List<ScheduleSetting> settings) {
            this.prescriptionId = prescriptionId;
            this.settings = settings;
        }
    }

    public static final class EndedPrescription {
        public final long prescriptionId;
        public final String endedAt;
        public final long endedMedicationCount;

        private EndedPrescription(long prescriptionId, String endedAt, long endedMedicationCount) {
            this.prescriptionId = prescriptionId;
            this.endedAt = endedAt;
            this.endedMedicationCount = endedMedicationCount;
        }
    }

    public static String medicationsPath(long patientId, String status) {
        String path = "/patients/" + patientId + "/medications";
        if (status == null || status.isEmpty()) {
            return path;
        }
        return path + "?status=" + status;
    }

    public static String prescriptionsPath(long patientId) {
        return "/patients/" + patientId + "/prescriptions";
    }

    public static String dosesPath(long patientId, LocalDate date) {
        return "/patients/" + patientId + "/doses?date=" + date;
    }

    public static String historyPath(long patientId, LocalDate from, LocalDate to, String status) {
        StringBuilder path = new StringBuilder("/patients/").append(patientId).append("/dose-history");
        boolean any = false;
        if (from != null && to != null) {
            path.append("?from=").append(from).append("&to=").append(to);
            any = true;
        }
        if (status != null && !status.isEmpty()) {
            path.append(any ? "&" : "?").append("status=").append(status);
        }
        return path.toString();
    }

    public static String autocompletePath(String query, int limit) throws ApiException {
        return "/medications/autocomplete?q=" + encode(query) + "&limit=" + limit;
    }

    public static String ocrPath(long patientId) {
        return "/prescriptions/ocr?patient_id=" + patientId;
    }

    public static boolean imageAllowed(byte[] bytes, String mime) {
        if (bytes == null || bytes.length == 0 || bytes.length > IMAGE_MAX_BYTES) {
            return false;
        }
        boolean jpeg = "image/jpeg".equals(mime) || "image/jpg".equals(mime);
        boolean png = "image/png".equals(mime);
        if (jpeg || png) {
            return true;
        }
        if (mime != null && !mime.isEmpty()) {
            return false;
        }
        return looksLikeJpeg(bytes) || looksLikePng(bytes);
    }

    public static boolean looksLikeJpeg(byte[] bytes) {
        return bytes != null && bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    public static boolean looksLikePng(byte[] bytes) {
        return bytes != null && bytes.length >= 4
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47;
    }

    public static Integer historySpanDays(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1L;
        if (days < 1L || days > HISTORY_MAX_DAYS) {
            return null;
        }
        return (int) days;
    }

    public static String takenAt(long epochMillis) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), SEOUL)
                .withNano(0)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public enum DoseFollowUp {
        NONE,
        ASK,
        RECORDED
    }

    public static DoseFollowUp followUp(Dose dose, Instant now) {
        if (dose == null || now == null) {
            return DoseFollowUp.NONE;
        }
        if (dose.takenAt != null && !dose.takenAt.isEmpty()) {
            return DoseFollowUp.RECORDED;
        }
        OffsetDateTime scheduled = parseOffset(dose.scheduledAt);
        if (scheduled == null) {
            return DoseFollowUp.NONE;
        }
        Instant start = scheduled.toInstant();
        if (now.isBefore(start)) {
            return DoseFollowUp.NONE;
        }
        OffsetDateTime close = parseOffset(dose.closeAt);
        if (close == null) {
            return DoseFollowUp.NONE;
        }
        if (now.isAfter(close.toInstant())) {
            return DoseFollowUp.NONE;
        }
        return DoseFollowUp.ASK;
    }

    public static String scheduleStatus(Dose dose, Instant now) {
        if (dose == null) {
            return STATUS_PENDING;
        }
        if (followUp(dose, now) == DoseFollowUp.ASK) {
            return STATUS_MISSED;
        }
        return dose.status;
    }

    public static Dose catchUpDose(List<Dose> doses, Instant now) {
        if (doses == null || now == null) {
            return null;
        }
        Dose chosen = null;
        Instant chosenAt = null;
        for (Dose dose : doses) {
            if (followUp(dose, now) != DoseFollowUp.ASK) {
                continue;
            }
            OffsetDateTime scheduled = parseOffset(dose.scheduledAt);
            Instant at = scheduled == null ? null : scheduled.toInstant();
            if (chosen == null || (at != null && (chosenAt == null || at.isBefore(chosenAt)))) {
                chosen = dose;
                chosenAt = at;
            }
        }
        return chosen;
    }

    public static String clock(String iso) {
        OffsetDateTime parsed = parseOffset(iso);
        if (parsed == null) {
            return iso == null ? "" : iso;
        }
        return String.format("%02d:%02d", parsed.getHour(), parsed.getMinute());
    }

    public static Long epochMillis(String iso) {
        OffsetDateTime parsed = parseOffset(iso);
        return parsed == null ? null : parsed.toInstant().toEpochMilli();
    }

    public static LocalDate seoulDate(String iso) {
        OffsetDateTime parsed = parseOffset(iso);
        if (parsed == null) {
            return null;
        }
        return parsed.toLocalDate();
    }

    public static JSONObject takenBody(String takenAt) throws ApiException {
        if (takenAt == null || takenAt.isEmpty()) {
            return null;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("taken_at", takenAt);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return body;
    }

    public static JSONObject scheduleBody(List<String> slots, List<String> times) throws ApiException {
        if (slots == null || times == null || slots.size() != times.size() || slots.isEmpty() || slots.size() > 4) {
            throw ApiException.local();
        }
        JSONArray settings = new JSONArray();
        try {
            int previousOrder = -1;
            int previousMinute = -1;
            for (int index = 0; index < slots.size(); index++) {
                String slot = slots.get(index);
                int order = slotOrder(slot);
                if (order < 0 || order <= previousOrder) {
                    throw ApiException.local();
                }
                String time = times.get(index);
                int minute = clockMinute(time);
                if (minute < 0 || minute > latestMinute(slot)) {
                    throw ApiException.local();
                }
                if (previousMinute >= 0 && minute - previousMinute < 4 * 60) {
                    throw ApiException.local();
                }
                JSONObject item = new JSONObject();
                item.put("timing_slot", slot);
                item.put("dose_time", time);
                settings.put(item);
                previousOrder = order;
                previousMinute = minute;
            }
            JSONObject body = new JSONObject();
            body.put("settings", settings);
            return body;
        } catch (JSONException exception) {
            throw ApiException.local();
        }
    }

    public static JSONObject medicationBody(
            Long drugId,
            String inputName,
            String dosePerIntake,
            boolean prn,
            List<String> timingSlots,
            String mealRelation,
            Integer mealOffsetMin,
            int totalDays,
            String startDate
    ) throws ApiException {
        List<String> orderedSlots = orderedSlots(timingSlots);
        if (!prn && (orderedSlots.isEmpty() || orderedSlots.size() > 4)) {
            throw ApiException.local();
        }
        if (prn && !orderedSlots.isEmpty()) {
            throw ApiException.local();
        }
        if ((drugId == null || drugId <= 0L) && (inputName == null || inputName.trim().isEmpty())) {
            throw ApiException.local();
        }
        if (inputName != null && inputName.length() > 500) {
            throw ApiException.local();
        }
        if (totalDays < 1 || totalDays > 365 || startDate == null || !startDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw ApiException.local();
        }
        JSONObject item = new JSONObject();
        try {
            if (drugId != null && drugId > 0L) {
                item.put("drug_id", drugId);
            }
            if (inputName != null && !inputName.trim().isEmpty()) {
                item.put("input_name", inputName.trim());
            }
            if (dosePerIntake != null && !dosePerIntake.isEmpty()) {
                item.put("dose_per_intake", Double.parseDouble(dosePerIntake));
            }
            if (prn) {
                item.put("is_prn", true);
                item.put("timing_slots", new JSONArray());
            } else {
                item.put("times_per_day", orderedSlots.size());
                JSONArray slots = new JSONArray();
                for (String slot : orderedSlots) {
                    slots.put(slot);
                }
                item.put("timing_slots", slots);
            }
            if (mealRelation != null && !mealRelation.isEmpty()) {
                item.put("meal_relation", mealRelation);
                if (mealOffsetMin != null) {
                    item.put("meal_offset_min", mealOffsetMin);
                }
            }
            item.put("total_days", totalDays);
            item.put("start_date", startDate);
        } catch (JSONException | NumberFormatException exception) {
            throw ApiException.local();
        }
        return item;
    }

    public static Long medicationTermsVersion(JSONArray terms) throws ApiException {
        return singleTermsVersion(terms, SettingsContracts.MEDICATION);
    }

    public static Long deviceTermsVersion(JSONArray terms) throws ApiException {
        return singleTermsVersion(terms, SettingsContracts.DEVICE);
    }

    /**
     * The current version id for one terms type.
     * Returns null when that type is absent or listed more than once.
     * A malformed item is a response error, not a missing version.
     */
    private static Long singleTermsVersion(JSONArray terms, String termsType) throws ApiException {
        if (terms == null || termsType == null) {
            throw ApiException.response(200);
        }
        Long found = null;
        for (int index = 0; index < terms.length(); index++) {
            JSONObject item = terms.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            String type = JsonReads.text(item, "terms_type");
            if (!termsType.equals(type)) {
                continue;
            }
            long id = JsonReads.positiveId(item, "terms_version_id");
            if (found != null) {
                return null;
            }
            found = id;
        }
        return found;
    }

    public static HomeSummary readHomeSummary(JSONObject response) throws ApiException {
        JsonReads.object(response);
        JSONArray slots = requiredArray(response, "slots");
        List<SlotState> parsed = new ArrayList<>();
        for (int index = 0; index < slots.length(); index++) {
            JSONObject item = slots.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            String slot = JsonReads.text(item, "timing_slot");
            String state = JsonReads.text(item, "state");
            if (!knownSummarySlot(slot) || !knownSlotState(state)) {
                throw ApiException.response(200);
            }
            parsed.add(new SlotState(slot, state));
        }
        NextDose next = null;
        if (response.has("next_dose") && !response.isNull("next_dose")) {
            JSONObject node = response.optJSONObject("next_dose");
            if (node == null) {
                throw ApiException.response(200);
            }
            String status = JsonReads.text(node, "status");
            if (!STATUS_PENDING.equals(status)) {
                throw ApiException.response(200);
            }
            String timing = JsonReads.text(node, "timing_slot");
            if (!knownSummarySlot(timing)) {
                throw ApiException.response(200);
            }
            next = new NextDose(
                    JsonReads.positiveId(node, "scheduled_dose_id"),
                    JsonReads.optionalText(node, "hospital_name"),
                    timing,
                    requireOffset(node, "scheduled_at"),
                    status,
                    readNamedDrugs(requiredArray(node, "medications"))
            );
        }
        boolean hasUnchecked = response.has("unchecked_doses");
        boolean hasUpcoming = response.has("upcoming_doses");
        boolean hasAllDone = response.has("all_done");
        boolean hasServerTime = response.has("server_time");
        List<SummaryDose> unchecked = hasUnchecked
                ? readSummaryDoses(optionalDoseArray(response, "unchecked_doses"))
                : Collections.emptyList();
        List<SummaryDose> upcoming = hasUpcoming
                ? readSummaryDoses(optionalDoseArray(response, "upcoming_doses"))
                : Collections.emptyList();
        return new HomeSummary(
                requireDate(response, "date"),
                JsonReads.number(response, "total"),
                JsonReads.number(response, "done"),
                JsonReads.number(response, "normal"),
                JsonReads.number(response, "delayed"),
                JsonReads.number(response, "missed"),
                JsonReads.number(response, "pending"),
                parsed,
                next,
                hasServerTime ? optionalOffset(response, "server_time") : null,
                unchecked,
                upcoming,
                hasAllDone ? JsonReads.optionalBool(response, "all_done") : null,
                hasUnchecked || hasUpcoming || hasAllDone
        );
    }

    /**
     * Denominator is every slot whose state is not {@code NONE}.
     * Numerator is every slot whose state is {@code DONE}.
     */
    public static SlotProgress slotProgress(List<SlotState> slots) {
        int scheduled = 0;
        int done = 0;
        if (slots != null) {
            for (SlotState slot : slots) {
                if (slot == null || STATE_NONE.equals(slot.state)) {
                    continue;
                }
                scheduled++;
                if (STATE_DONE.equals(slot.state)) {
                    done++;
                }
            }
        }
        return new SlotProgress(done, scheduled);
    }

    /**
     * Whole minutes from {@code serverTime} to {@code scheduledAt}.
     * Both values must carry an offset such as {@code +09:00}.
     */
    public static Long minutesUntil(String scheduledAt, String serverTime) {
        OffsetDateTime scheduled = parseOffset(scheduledAt);
        OffsetDateTime server = parseOffset(serverTime);
        if (scheduled == null || server == null) {
            return null;
        }
        return ChronoUnit.MINUTES.between(server, scheduled);
    }

    public static List<Dose> readDoses(JSONArray array) throws ApiException {
        if (array == null) {
            throw ApiException.response(200);
        }
        List<Dose> doses = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            doses.add(readDose(item));
        }
        return doses;
    }

    public static Dose readDose(JSONObject item) throws ApiException {
        JsonReads.object(item);
        String status = JsonReads.text(item, "status");
        String source = JsonReads.optionalText(item, "decision_source");
        String slot = JsonReads.text(item, "timing_slot");
        if (!knownDoseStatus(status) || !knownSummarySlot(slot) || !knownDecision(source)) {
            throw ApiException.response(200);
        }
        return new Dose(
                JsonReads.positiveId(item, "scheduled_dose_id"),
                JsonReads.positiveId(item, "prescription_id"),
                JsonReads.optionalText(item, "hospital_name"),
                slot,
                requireOffset(item, "scheduled_at"),
                requireOffset(item, "normal_until"),
                requireOffset(item, "close_at"),
                status,
                optionalOffset(item, "taken_at"),
                source,
                JsonReads.bool(item, "corrected"),
                readDoseDrugs(requiredArray(item, "medications"))
        );
    }

    public static List<Dose> pendingSorted(List<Dose> doses) {
        List<Dose> pending = new ArrayList<>();
        if (doses == null) {
            return pending;
        }
        for (Dose dose : doses) {
            if (dose != null && STATUS_PENDING.equals(dose.status)) {
                pending.add(dose);
            }
        }
        Collections.sort(pending, (left, right) -> left.scheduledAt.compareTo(right.scheduledAt));
        return pending;
    }

    public static List<Medication> readMedications(JSONArray array) throws ApiException {
        if (array == null) {
            throw ApiException.response(200);
        }
        List<Medication> medications = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            medications.add(readMedication(item));
        }
        return medications;
    }

    public static Medication readMedication(JSONObject item) throws ApiException {
        JsonReads.object(item);
        String status = JsonReads.text(item, "status");
        String meal = JsonReads.optionalText(item, "meal_relation");
        if (!knownMedicationStatus(status) || !knownMeal(meal)) {
            throw ApiException.response(200);
        }
        List<String> slots = JsonReads.optionalStrings(item, "timing_slots");
        if (slots == null || !knownSlotList(slots)) {
            throw ApiException.response(200);
        }
        long timesPerDay = JsonReads.number(item, "times_per_day");
        if (timesPerDay < 0L || timesPerDay > 4L) {
            throw ApiException.response(200);
        }
        return new Medication(
                JsonReads.positiveId(item, "medication_id"),
                JsonReads.positiveId(item, "prescription_id"),
                JsonReads.optionalText(item, "hospital_name"),
                JsonReads.optionalPositiveId(item, "drug_id"),
                JsonReads.text(item, "drug_name"),
                JsonReads.optionalText(item, "manufacturer"),
                JsonReads.text(item, "match_status"),
                JsonReads.decimal(item, "dose_per_intake"),
                JsonReads.bool(item, "is_prn"),
                timesPerDay,
                slots,
                meal,
                JsonReads.optionalNumber(item, "meal_offset_min"),
                JsonReads.number(item, "total_days"),
                requireDate(item, "start_date"),
                requireDate(item, "end_date"),
                optionalDate(item, "ended_on"),
                status
        );
    }

    public static List<Prescription> readPrescriptions(JSONArray array) throws ApiException {
        if (array == null) {
            throw ApiException.response(200);
        }
        List<Prescription> prescriptions = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            prescriptions.add(new Prescription(
                    JsonReads.positiveId(item, "prescription_id"),
                    JsonReads.optionalText(item, "hospital_name"),
                    JsonReads.optionalText(item, "pharmacy_name"),
                    optionalDate(item, "prescribed_date"),
                    readMedications(requiredArray(item, "medications"))
            ));
        }
        return prescriptions;
    }

    public static List<DrugSuggestion> readSuggestions(JSONArray array) throws ApiException {
        if (array == null) {
            throw ApiException.response(200);
        }
        List<DrugSuggestion> rows = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            rows.add(new DrugSuggestion(
                    JsonReads.positiveId(item, "drug_id"),
                    JsonReads.text(item, "product_name"),
                    JsonReads.optionalText(item, "manufacturer")
            ));
        }
        return rows;
    }

    public static OcrDraft readOcr(JSONObject response) throws ApiException {
        JsonReads.object(response);
        JSONArray recognized = requiredArray(response, "recognized");
        List<RecognizedMedication> rows = new ArrayList<>();
        for (int index = 0; index < recognized.length(); index++) {
            JSONObject item = recognized.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            String meal = JsonReads.optionalText(item, "meal_relation");
            if (!knownMeal(meal) && !MealTimingText.displaysAsAfterMeal(meal)) {
                meal = null;
            }
            Boolean prn = JsonReads.optionalBool(item, "is_prn");
            rows.add(new RecognizedMedication(
                    JsonReads.text(item, "drug_name"),
                    JsonReads.optionalPositiveId(item, "matched_drug_id"),
                    ocrCandidates(item),
                    JsonReads.optionalDecimal(item, "dose_per_intake"),
                    JsonReads.optionalNumber(item, "times_per_day"),
                    ocrTimingSlots(item),
                    JsonReads.optionalNumber(item, "total_days"),
                    prn != null && prn,
                    meal,
                    ocrMealOffset(item)
            ));
        }
        return new OcrDraft(
                JsonReads.optionalText(response, "hospital_name"),
                JsonReads.optionalText(response, "pharmacy_name"),
                optionalDate(response, "prescribed_date"),
                rows
        );
    }

    public static CreatedPrescription readCreated(JSONObject response) throws ApiException {
        JsonReads.object(response);
        JSONArray ids = requiredArray(response, "medication_ids");
        List<Long> medicationIds = new ArrayList<>();
        for (int index = 0; index < ids.length(); index++) {
            Object value = ids.opt(index);
            if (!(value instanceof Number) || ((Number) value).longValue() <= 0L) {
                throw ApiException.response(200);
            }
            medicationIds.add(((Number) value).longValue());
        }
        return new CreatedPrescription(JsonReads.positiveId(response, "prescription_id"), medicationIds);
    }

    public static ScheduleSettings readSchedule(JSONObject response) throws ApiException {
        JsonReads.object(response);
        JSONArray settings = requiredArray(response, "settings");
        List<ScheduleSetting> rows = new ArrayList<>();
        for (int index = 0; index < settings.length(); index++) {
            JSONObject item = settings.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            String slot = JsonReads.text(item, "timing_slot");
            String time = JsonReads.text(item, "dose_time");
            if (!knownServerSlot(slot) || !time.matches("\\d{2}:\\d{2}")) {
                throw ApiException.response(200);
            }
            rows.add(new ScheduleSetting(
                    slot,
                    time,
                    JsonReads.number(item, "normal_window_min"),
                    JsonReads.number(item, "close_after_min")
            ));
        }
        return new ScheduleSettings(JsonReads.positiveId(response, "prescription_id"), rows);
    }

    public static EndedPrescription readEnded(JSONObject response) throws ApiException {
        JsonReads.object(response);
        return new EndedPrescription(
                JsonReads.positiveId(response, "prescription_id"),
                requireOffset(response, "ended_at"),
                JsonReads.number(response, "ended_medication_count")
        );
    }

    public static boolean containsSlot(List<SlotState> slots, String timingSlot) {
        if (slots == null) {
            return false;
        }
        for (SlotState slot : slots) {
            if (timingSlot.equals(slot.timingSlot)) {
                return true;
            }
        }
        return false;
    }

    public static String slotState(List<SlotState> slots, String timingSlot) {
        if (slots == null) {
            return null;
        }
        for (SlotState slot : slots) {
            if (timingSlot.equals(slot.timingSlot)) {
                return slot.state;
            }
        }
        return null;
    }

    /**
     * True when today has a scheduled dose in {@code slot}.
     * A completed or missed summary state stays active.
     * {@code NONE} does not. Today's dose rows count even when already taken.
     * Registered medications are used only when neither the summary nor today's doses are ready.
     * A PRN medication with no timing slot does not light a slot, and dates outside the course are ignored.
     */
    public static boolean timingSlotActive(
            String slot,
            String summaryState,
            boolean summaryReady,
            List<Dose> todayDoses,
            boolean dosesReady,
            List<Medication> medications,
            boolean medicationsReady,
            LocalDate today
    ) {
        if (slot == null) {
            return false;
        }
        if (summaryReady && summaryState != null && !STATE_NONE.equals(summaryState)) {
            return true;
        }
        if (dosesReady) {
            if (todayDoses == null) {
                return false;
            }
            for (Dose dose : todayDoses) {
                if (dose != null && slot.equals(dose.timingSlot)) {
                    return true;
                }
            }
            return false;
        }
        if (summaryReady && STATE_NONE.equals(summaryState)) {
            return false;
        }
        if (!medicationsReady || medications == null || today == null) {
            return false;
        }
        for (Medication medication : medications) {
            if (medication == null || medication.timingSlots == null || !medication.timingSlots.contains(slot)) {
                continue;
            }
            if (medication.prn && medication.timingSlots.isEmpty()) {
                continue;
            }
            if (coversToday(medication, today)) {
                return true;
            }
        }
        return false;
    }

    private static boolean coversToday(Medication medication, LocalDate today) {
        if (STATUS_ENDED.equals(medication.status) || STATUS_UPCOMING.equals(medication.status)) {
            return false;
        }
        if (medication.prn && (medication.timingSlots == null || medication.timingSlots.isEmpty())) {
            return false;
        }
        if (medication.startDate == null || medication.endDate == null) {
            return false;
        }
        try {
            LocalDate start = LocalDate.parse(medication.startDate);
            LocalDate end = LocalDate.parse(medication.endDate);
            return !today.isBefore(start) && !today.isAfter(end);
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private static JSONArray optionalDoseArray(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key)) {
            return new JSONArray();
        }
        if (!(json.opt(key) instanceof JSONArray)) {
            throw ApiException.response(200);
        }
        return json.optJSONArray(key);
    }

    private static List<SummaryDose> readSummaryDoses(JSONArray array) throws ApiException {
        List<SummaryDose> rows = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            String timing = JsonReads.text(item, "timing_slot");
            if (!knownSummarySlot(timing)) {
                throw ApiException.response(200);
            }
            List<NamedDrug> medications = item.has("medications") && !item.isNull("medications")
                    ? readNamedDrugs(requiredArray(item, "medications"))
                    : Collections.emptyList();
            rows.add(new SummaryDose(
                    JsonReads.positiveId(item, "scheduled_dose_id"),
                    JsonReads.optionalText(item, "hospital_name"),
                    timing,
                    requireOffset(item, "scheduled_at"),
                    medications
            ));
        }
        Collections.sort(rows, (left, right) -> {
            Long leftAt = epochMillis(left.scheduledAt);
            Long rightAt = epochMillis(right.scheduledAt);
            if (leftAt == null || rightAt == null) {
                return left.scheduledAt.compareTo(right.scheduledAt);
            }
            return leftAt.compareTo(rightAt);
        });
        return rows;
    }

    private static List<NamedDrug> readNamedDrugs(JSONArray array) throws ApiException {
        List<NamedDrug> rows = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            rows.add(new NamedDrug(
                    JsonReads.positiveId(item, "medication_id"),
                    JsonReads.text(item, "drug_name")
            ));
        }
        return rows;
    }

    private static List<DoseDrug> readDoseDrugs(JSONArray array) throws ApiException {
        List<DoseDrug> rows = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            rows.add(new DoseDrug(
                    JsonReads.positiveId(item, "medication_id"),
                    JsonReads.text(item, "drug_name"),
                    JsonReads.decimal(item, "dose_per_intake")
            ));
        }
        return rows;
    }

    private static JSONArray requiredArray(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key) || !(json.opt(key) instanceof JSONArray)) {
            throw ApiException.response(200);
        }
        return json.optJSONArray(key);
    }

    private static String requireDate(JSONObject json, String key) throws ApiException {
        String value = JsonReads.text(json, key);
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw ApiException.response(200);
        }
        return value;
    }

    private static String optionalDate(JSONObject json, String key) throws ApiException {
        String value = JsonReads.optionalText(json, key);
        if (value == null) {
            return null;
        }
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw ApiException.response(200);
        }
        return value;
    }

    private static String requireOffset(JSONObject json, String key) throws ApiException {
        String value = JsonReads.text(json, key);
        if (parseOffset(value) == null) {
            throw ApiException.response(200);
        }
        return value;
    }

    private static String optionalOffset(JSONObject json, String key) throws ApiException {
        String value = JsonReads.optionalText(json, key);
        if (value == null) {
            return null;
        }
        if (parseOffset(value) == null) {
            throw ApiException.response(200);
        }
        return value;
    }

    private static OffsetDateTime parseOffset(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static List<DrugSuggestion> ocrCandidates(JSONObject item) throws ApiException {
        if (item == null || !item.has("candidates") || item.isNull("candidates")) {
            return new ArrayList<>();
        }
        Object value = item.opt("candidates");
        if (!(value instanceof JSONArray)) {
            throw ApiException.response(200);
        }
        return readSuggestions((JSONArray) value);
    }

    private static List<String> ocrTimingSlots(JSONObject item) {
        if (item == null || !item.has("timing_slots") || item.isNull("timing_slots")) {
            return null;
        }
        Object value = item.opt("timing_slots");
        if (!(value instanceof JSONArray)) {
            return null;
        }
        JSONArray array = (JSONArray) value;
        List<String> slots = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            Object slot = array.opt(index);
            if (!(slot instanceof String) || !knownServerSlot((String) slot) || slots.contains(slot)) {
                continue;
            }
            slots.add((String) slot);
        }
        return slots;
    }

    private static Long ocrMealOffset(JSONObject item) {
        if (item == null || !item.has("meal_offset_min") || item.isNull("meal_offset_min")) {
            return null;
        }
        Object value = item.opt("meal_offset_min");
        if (!(value instanceof Number)) {
            return null;
        }
        return ((Number) value).longValue();
    }

    private static boolean knownServerSlot(String slot) {
        return slotOrder(slot) >= 0;
    }

    private static boolean knownSummarySlot(String slot) {
        return knownServerSlot(slot);
    }

    private static int slotOrder(String slot) {
        if (SLOT_MORNING.equals(slot)) {
            return 0;
        }
        if (SLOT_LUNCH.equals(slot)) {
            return 1;
        }
        if (SLOT_DINNER.equals(slot)) {
            return 2;
        }
        if (SLOT_BEDTIME.equals(slot)) {
            return 3;
        }
        return -1;
    }

    private static int latestMinute(String slot) {
        return SLOT_BEDTIME.equals(slot) ? 23 * 60 + 59 : 20 * 60 + 59;
    }

    private static int clockMinute(String time) {
        if (time == null || !time.matches("\\d{2}:\\d{2}")) {
            return -1;
        }
        int hour = Integer.parseInt(time.substring(0, 2));
        int minute = Integer.parseInt(time.substring(3, 5));
        if (hour > 23 || minute > 59) {
            return -1;
        }
        return hour * 60 + minute;
    }

    private static boolean knownSlotList(List<String> slots) throws ApiException {
        if (slots.size() > 4) {
            return false;
        }
        boolean[] seen = new boolean[4];
        for (String slot : slots) {
            int order = slotOrder(slot);
            if (order < 0 || seen[order]) {
                return false;
            }
            seen[order] = true;
        }
        return true;
    }

    private static List<String> orderedSlots(List<String> timingSlots) throws ApiException {
        if (timingSlots == null || !knownSlotList(timingSlots)) {
            throw ApiException.local();
        }
        List<String> ordered = new ArrayList<>();
        String[] names = {SLOT_MORNING, SLOT_LUNCH, SLOT_DINNER, SLOT_BEDTIME};
        boolean[] seen = new boolean[4];
        for (String slot : timingSlots) {
            seen[slotOrder(slot)] = true;
        }
        for (int index = 0; index < names.length; index++) {
            if (seen[index]) {
                ordered.add(names[index]);
            }
        }
        return ordered;
    }

    private static boolean knownSlotState(String state) {
        return STATE_NONE.equals(state)
                || STATE_PENDING.equals(state)
                || STATE_DONE.equals(state)
                || STATE_MISSED.equals(state);
    }

    private static boolean knownDoseStatus(String status) {
        return STATUS_PENDING.equals(status)
                || STATUS_NORMAL.equals(status)
                || STATUS_DELAYED.equals(status)
                || STATUS_MISSED.equals(status);
    }

    private static boolean knownMedicationStatus(String status) {
        return STATUS_UPCOMING.equals(status) || STATUS_ACTIVE.equals(status) || STATUS_ENDED.equals(status);
    }

    private static boolean knownMeal(String meal) {
        return meal == null
                || "BEFORE_MEAL".equals(meal)
                || "AFTER_MEAL".equals(meal)
                || "WITH_MEAL".equals(meal);
    }

    private static boolean knownDecision(String source) {
        return source == null || "DEVICE".equals(source) || "MANUAL".equals(source) || "SYSTEM".equals(source);
    }

    private static String encode(String value) throws ApiException {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw ApiException.local();
        }
    }
}
