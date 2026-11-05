package com.example.yakyeon2.api;

import com.example.yakyeon2.data.AccountRoles;
import com.example.yakyeon2.data.LegalMarks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class SettingsContracts {

    public static final String TOS = "TOS";
    public static final String PRIVACY = "PRIVACY";
    public static final String SENSITIVE_HEALTH = "SENSITIVE_HEALTH";
    public static final String SERVICE_NOTICE = "SERVICE_NOTICE";
    public static final String MEDICATION = "MEDICATION";
    public static final String DEVICE = "DEVICE";
    public static final String BUZZER_ENABLED = "buzzer_enabled";
    public static final String LED_ENABLED = "led_enabled";
    public static final String SELF = "SELF";
    public static final String GUARDIAN = "GUARDIAN";
    public static final String ACTIVE = "ACTIVE";

    private SettingsContracts() {
    }

    public enum Resolution {
        ONE,
        NONE,
        SEVERAL
    }

    public static final class ResolvedPatient {
        public final Resolution resolution;
        public final long patientId;

        private ResolvedPatient(Resolution resolution, long patientId) {
            this.resolution = resolution;
            this.patientId = patientId;
        }
    }

    public static final class ManagedPatient {
        public final long patientId;
        public final String name;
        public final String birthDate;
        public final boolean hasAccount;
        public final String myRole;
        public final Long linkId;
        public final String permissionScope;
        public final String relationship;
        public final List<String> sharedItems;
        public final String phoneMasked;

        private ManagedPatient(
                long patientId,
                String name,
                String birthDate,
                boolean hasAccount,
                String myRole,
                Long linkId,
                String permissionScope,
                String relationship,
                List<String> sharedItems,
                String phoneMasked
        ) {
            this.patientId = patientId;
            this.name = name;
            this.birthDate = birthDate;
            this.hasAccount = hasAccount;
            this.myRole = myRole;
            this.linkId = linkId;
            this.permissionScope = permissionScope;
            this.relationship = relationship;
            this.sharedItems = sharedItems;
            this.phoneMasked = phoneMasked;
        }
    }

    public static final class GuardianLink {
        public final long linkId;
        public final String guardianName;
        public final String guardianLoginId;
        public final String status;
        public final String permissionScope;
        public final String relationship;
        public final List<String> sharedItems;

        private GuardianLink(
                long linkId,
                String guardianName,
                String guardianLoginId,
                String status,
                String permissionScope,
                String relationship,
                List<String> sharedItems
        ) {
            this.linkId = linkId;
            this.guardianName = guardianName;
            this.guardianLoginId = guardianLoginId;
            this.status = status;
            this.permissionScope = permissionScope;
            this.relationship = relationship;
            this.sharedItems = sharedItems;
        }
    }

    public static final class AgreementRecord {
        public final String termsType;
        public final long termsVersionId;
        public final String version;
        public final boolean agreed;
        public final String changedAt;

        private AgreementRecord(
                String termsType,
                long termsVersionId,
                String version,
                boolean agreed,
                String changedAt
        ) {
            this.termsType = termsType;
            this.termsVersionId = termsVersionId;
            this.version = version;
            this.agreed = agreed;
            this.changedAt = changedAt;
        }
    }

    public static final class DeviceSnapshot {
        public final long deviceId;
        public final String serialNo;
        public final boolean online;
        public final Boolean buzzerEnabled;
        public final Boolean ledEnabled;
        public final String scheduleSyncedAt;
        /** Null until the server adds {@code schedule_in_sync}. Null is not treated as true. */
        public final Boolean scheduleInSync;

        private DeviceSnapshot(
                long deviceId,
                String serialNo,
                boolean online,
                Boolean buzzerEnabled,
                Boolean ledEnabled,
                String scheduleSyncedAt,
                Boolean scheduleInSync
        ) {
            this.deviceId = deviceId;
            this.serialNo = serialNo;
            this.online = online;
            this.buzzerEnabled = buzzerEnabled;
            this.ledEnabled = ledEnabled;
            this.scheduleSyncedAt = scheduleSyncedAt;
            this.scheduleInSync = scheduleInSync;
        }
    }

    public static final class ShareDocument {
        public final String patientName;
        public final String birthDate;
        public final String periodFrom;
        public final String periodTo;
        public final List<String> records;
        public final List<ShareMedicine> medications;
        public final List<String> interactions;
        public final String interactionDisclaimer;
        public final String adherence;

        private ShareDocument(
                String patientName,
                String birthDate,
                String periodFrom,
                String periodTo,
                List<String> records,
                List<ShareMedicine> medications,
                List<String> interactions,
                String interactionDisclaimer,
                String adherence
        ) {
            this.patientName = patientName;
            this.birthDate = birthDate;
            this.periodFrom = periodFrom;
            this.periodTo = periodTo;
            this.records = records;
            this.medications = medications;
            this.interactions = interactions;
            this.interactionDisclaimer = interactionDisclaimer;
            this.adherence = adherence;
        }

        public String text() {
            StringBuilder text = new StringBuilder();
            line(text, "이름", patientName);
            line(text, "생년월일", birthDate);
            line(text, "기간", join(periodFrom, periodTo));
            section(text, "복약 기록", records);
            section(text, "약", medicineNames(medications));
            section(text, "병용금기", interactions);
            line(text, "안내", interactionDisclaimer);
            line(text, "복용률", adherence);
            return text.toString().trim();
        }
    }

    public static final class ShareMedicine {
        public final String name;
        public final String detail;
        public final String startDate;
        public final String endDate;
        public final boolean ended;
        public final boolean statusKnown;

        private ShareMedicine(
                String name,
                String detail,
                String startDate,
                String endDate,
                boolean ended,
                boolean statusKnown
        ) {
            this.name = name;
            this.detail = detail == null ? "" : detail;
            this.startDate = startDate;
            this.endDate = endDate;
            this.ended = ended;
            this.statusKnown = statusKnown;
        }
    }

    public static List<ManagedPatient> readManagedPatients(JSONArray array) throws ApiException {
        if (array == null) {
            throw ApiException.response(200);
        }
        List<ManagedPatient> rows = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            String role = JsonReads.text(item, "my_role");
            if (!SELF.equals(role) && !GUARDIAN.equals(role)) {
                throw ApiException.response(200);
            }
            String birth = JsonReads.text(item, "birth_date");
            if (!birth.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw ApiException.response(200);
            }
            rows.add(new ManagedPatient(
                    JsonReads.positiveId(item, "patient_id"),
                    JsonReads.text(item, "name"),
                    birth,
                    JsonReads.bool(item, "has_account"),
                    role,
                    JsonReads.optionalPositiveId(item, "link_id"),
                    JsonReads.optionalText(item, "permission_scope"),
                    JsonReads.optionalText(item, "relationship"),
                    JsonReads.optionalStrings(item, "shared_items"),
                    JsonReads.optionalText(item, "phone_masked")
            ));
        }
        return rows;
    }

    public static ResolvedPatient resolveSelf(List<ManagedPatient> rows) {
        return resolveRole(rows, SELF);
    }

    public static ResolvedPatient resolveForSettings(List<ManagedPatient> rows, String accountType) {
        if (AccountRoles.PATIENT.equals(accountType)) {
            return resolveRole(rows, SELF);
        }
        if (AccountRoles.GUARDIAN.equals(accountType)) {
            return resolveFirst(rows, GUARDIAN);
        }
        return new ResolvedPatient(Resolution.NONE, 0L);
    }

    public static List<GuardianLink> readGuardianLinks(JSONArray array) throws ApiException {
        if (array == null) {
            throw ApiException.response(200);
        }
        List<GuardianLink> rows = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            rows.add(new GuardianLink(
                    JsonReads.positiveId(item, "link_id"),
                    JsonReads.text(item, "guardian_name"),
                    JsonReads.text(item, "guardian_login_id"),
                    JsonReads.text(item, "status"),
                    JsonReads.text(item, "permission_scope"),
                    JsonReads.optionalText(item, "relationship"),
                    JsonReads.optionalStrings(item, "shared_items")
            ));
        }
        return rows;
    }

    public static List<AgreementRecord> readAgreements(JSONArray array) throws ApiException {
        if (array == null) {
            throw ApiException.response(200);
        }
        List<AgreementRecord> rows = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            rows.add(new AgreementRecord(
                    JsonReads.text(item, "terms_type"),
                    JsonReads.positiveId(item, "terms_version_id"),
                    JsonReads.text(item, "version"),
                    JsonReads.bool(item, "agreed"),
                    JsonReads.text(item, "changed_at")
            ));
        }
        return rows;
    }

    public static LegalMarks marksFrom(List<AgreementRecord> records) {
        LegalMarks marks = new LegalMarks();
        for (AgreementRecord record : records) {
            boolean withdrawn = !record.agreed;
            if (TOS.equals(record.termsType)) {
                marks.setTerms(withdrawn);
            } else if (PRIVACY.equals(record.termsType)) {
                marks.setPrivacy(withdrawn);
            } else if (SENSITIVE_HEALTH.equals(record.termsType)) {
                marks.setSensitive(withdrawn);
            } else if (MEDICATION.equals(record.termsType)) {
                marks.setDrugCollection(withdrawn);
            } else if (DEVICE.equals(record.termsType)) {
                marks.setDevice(withdrawn);
            }
        }
        return marks;
    }

    public static Long versionId(List<AgreementRecord> records, String termsType) {
        Long found = null;
        for (AgreementRecord record : records) {
            if (termsType.equals(record.termsType)) {
                if (found != null) {
                    return null;
                }
                found = record.termsVersionId;
            }
        }
        return found;
    }

    public static JSONObject agreementBody(long termsVersionId, boolean agreed) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("terms_version_id", termsVersionId);
            body.put("agreed", agreed);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return body;
    }

    public static DeviceSnapshot readDevice(JSONObject response) throws ApiException {
        JsonReads.object(response);
        JsonReads.text(response, "status");
        JsonReads.number(response, "max_compartments");
        JsonReads.text(response, "registered_at");
        JsonReads.text(response, "last_seen_at");
        JsonReads.text(response, "firmware_version");
        JsonReads.bool(response, "ir_sensor_ok");
        JsonReads.bool(response, "button_ok");
        return new DeviceSnapshot(
                JsonReads.positiveId(response, "device_id"),
                JsonReads.text(response, "serial_no"),
                JsonReads.bool(response, "is_online"),
                JsonReads.optionalBool(response, BUZZER_ENABLED),
                JsonReads.optionalBool(response, LED_ENABLED),
                JsonReads.optionalText(response, "schedule_synced_at"),
                JsonReads.optionalBool(response, "schedule_in_sync")
        );
    }

    public static JSONObject deviceSettingBody(String field, boolean enabled) throws ApiException {
        if (!BUZZER_ENABLED.equals(field) && !LED_ENABLED.equals(field)) {
            throw ApiException.local();
        }
        JSONObject body = new JSONObject();
        try {
            body.put(field, enabled);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return body;
    }

    public static long readScheduleVersion(JSONObject response) throws ApiException {
        long version = JsonReads.number(JsonReads.object(response), "schedule_version");
        if (version < 0L) {
            throw ApiException.response(200);
        }
        return version;
    }

    public static boolean shareRangeAllowed(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            return false;
        }
        long days = to.toEpochDay() - from.toEpochDay() + 1L;
        return days >= 1L && days <= 90L;
    }

    public static String sharePath(
            long patientId,
            LocalDate from,
            LocalDate to,
            boolean includeRecords,
            boolean includePeriod,
            boolean includeInteractions,
            boolean includeAdherence
    ) {
        return "/patients/" + patientId + "/share-summary?from=" + from
                + "&to=" + to
                + "&include_records=" + includeRecords
                + "&include_period=" + includePeriod
                + "&include_interactions=" + includeInteractions
                + "&include_adherence=" + includeAdherence;
    }

    public static ShareDocument readShare(JSONObject response) throws ApiException {
        JsonReads.object(response);
        String patientName = null;
        String birthDate = null;
        JSONObject patient = response.optJSONObject("patient");
        if (response.has("patient") && !response.isNull("patient")) {
            if (patient == null) {
                throw ApiException.response(200);
            }
            patientName = JsonReads.optionalText(patient, "name");
            birthDate = JsonReads.optionalText(patient, "birth_date");
        }
        String periodFrom = null;
        String periodTo = null;
        JSONObject period = response.optJSONObject("period");
        if (response.has("period") && !response.isNull("period")) {
            if (period == null) {
                throw ApiException.response(200);
            }
            periodFrom = JsonReads.optionalText(period, "from");
            periodTo = JsonReads.optionalText(period, "to");
        }
        String disclaimer = null;
        List<String> interactions = null;
        if (response.has("interactions") && !response.isNull("interactions")) {
            JSONObject block = response.optJSONObject("interactions");
            if (block == null) {
                throw ApiException.response(200);
            }
            disclaimer = JsonReads.optionalText(block, "disclaimer");
            interactions = readInteractionLines(block.optJSONArray("items"), block.has("items") && !block.isNull("items"));
        }
        String adherence = null;
        if (response.has("adherence") && !response.isNull("adherence")) {
            JSONObject block = response.optJSONObject("adherence");
            if (block == null) {
                throw ApiException.response(200);
            }
            adherence = adherenceText(block);
        }
        return new ShareDocument(
                patientName,
                birthDate,
                periodFrom,
                periodTo,
                readRecords(response),
                readMedications(response),
                interactions,
                disclaimer,
                adherence
        );
    }

    private static ResolvedPatient resolveFirst(List<ManagedPatient> rows, String role) {
        if (rows == null) {
            return new ResolvedPatient(Resolution.NONE, 0L);
        }
        for (ManagedPatient row : rows) {
            if (role.equals(row.myRole)) {
                return new ResolvedPatient(Resolution.ONE, row.patientId);
            }
        }
        return new ResolvedPatient(Resolution.NONE, 0L);
    }

    private static ResolvedPatient resolveRole(List<ManagedPatient> rows, String role) {
        long id = 0L;
        int count = 0;
        for (ManagedPatient row : rows) {
            if (role.equals(row.myRole)) {
                count++;
                id = row.patientId;
            }
        }
        if (count == 1) {
            return new ResolvedPatient(Resolution.ONE, id);
        }
        if (count == 0) {
            return new ResolvedPatient(Resolution.NONE, 0L);
        }
        return new ResolvedPatient(Resolution.SEVERAL, 0L);
    }

    private static List<String> readRecords(JSONObject response) throws ApiException {
        if (!response.has("records") || response.isNull("records")) {
            return null;
        }
        JSONArray array = response.optJSONArray("records");
        if (array == null) {
            throw ApiException.response(200);
        }
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            String when = JsonReads.optionalText(item, "scheduled_at");
            String status = JsonReads.optionalText(item, "status");
            lines.add(join(when, status));
        }
        return lines;
    }

    public static List<ShareMedicine> shareMedicines(List<HomeContracts.Medication> medications) {
        List<ShareMedicine> lines = new ArrayList<>();
        if (medications == null) {
            return lines;
        }
        for (HomeContracts.Medication medication : medications) {
            if (medication == null || medication.drugName == null || medication.drugName.trim().isEmpty()) {
                continue;
            }
            boolean ended = HomeContracts.STATUS_ENDED.equals(medication.status);
            String end = medication.endDate;
            if (ended && medication.endedOn != null && !medication.endedOn.trim().isEmpty()) {
                end = medication.endedOn.trim();
            }
            lines.add(new ShareMedicine(
                    medication.drugName.trim(),
                    catalogDetail(medication),
                    medication.startDate,
                    end,
                    ended,
                    true
            ));
        }
        return lines;
    }

    private static String catalogDetail(HomeContracts.Medication medication) {
        StringBuilder detail = new StringBuilder();
        if (medication.dosePerIntake != null && !medication.dosePerIntake.isEmpty()) {
            detail.append("1회 ").append(medication.dosePerIntake);
        }
        if (medication.timingSlots != null && !medication.timingSlots.isEmpty()) {
            if (detail.length() > 0) {
                detail.append(" · ");
            }
            for (int index = 0; index < medication.timingSlots.size(); index++) {
                if (index > 0) {
                    detail.append(", ");
                }
                detail.append(slotLabel(medication.timingSlots.get(index)));
            }
        }
        return detail.toString();
    }

    private static List<String> medicineNames(List<ShareMedicine> medications) {
        if (medications == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (ShareMedicine medicine : medications) {
            if (medicine == null || medicine.name == null || medicine.name.isEmpty()) {
                continue;
            }
            names.add(medicine.name);
        }
        return names;
    }

    private static List<ShareMedicine> readMedications(JSONObject response) throws ApiException {
        if (!response.has("medications") || response.isNull("medications")) {
            return null;
        }
        JSONArray array = response.optJSONArray("medications");
        if (array == null) {
            throw ApiException.response(200);
        }
        List<ShareMedicine> lines = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            String name = firstName(item);
            String status = JsonReads.optionalText(item, "status");
            boolean known = status != null && !status.trim().isEmpty();
            boolean ended = known && "ENDED".equalsIgnoreCase(status.trim());
            lines.add(new ShareMedicine(
                    blank(name),
                    medicineDetail(item),
                    JsonReads.optionalText(item, "start_date"),
                    JsonReads.optionalText(item, "end_date"),
                    ended,
                    known
            ));
        }
        return lines;
    }

    private static String firstName(JSONObject item) throws ApiException {
        String drug = JsonReads.optionalText(item, "drug_name");
        if (drug != null && !drug.trim().isEmpty()) {
            return drug.trim();
        }
        String product = JsonReads.optionalText(item, "product_name");
        if (product != null && !product.trim().isEmpty()) {
            return product.trim();
        }
        String input = JsonReads.optionalText(item, "input_name");
        if (input != null && !input.trim().isEmpty()) {
            return input.trim();
        }
        return JsonReads.optionalText(item, "name");
    }

    private static String medicineDetail(JSONObject item) throws ApiException {
        StringBuilder detail = new StringBuilder();
        String dose = JsonReads.optionalDecimal(item, "dose_per_intake");
        if (dose != null && !dose.isEmpty()) {
            detail.append("1회 ").append(dose);
        }
        List<String> slots = JsonReads.optionalStrings(item, "timing_slots");
        if (slots != null && !slots.isEmpty()) {
            if (detail.length() > 0) {
                detail.append(" · ");
            }
            for (int index = 0; index < slots.size(); index++) {
                if (index > 0) {
                    detail.append(", ");
                }
                detail.append(slotLabel(slots.get(index)));
            }
        }
        return detail.toString();
    }

    private static String slotLabel(String slot) {
        if (slot == null) {
            return "";
        }
        if ("MORNING".equals(slot)) {
            return "아침";
        }
        if ("LUNCH".equals(slot)) {
            return "점심";
        }
        if ("DINNER".equals(slot)) {
            return "저녁";
        }
        if ("BEDTIME".equals(slot)) {
            return "취침";
        }
        return slot;
    }

    private static List<String> readInteractionLines(JSONArray array, boolean present) throws ApiException {
        if (!present) {
            return null;
        }
        if (array == null) {
            throw ApiException.response(200);
        }
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            String left = JsonReads.optionalText(item, "medication_a_name");
            String right = JsonReads.optionalText(item, "medication_b_name");
            lines.add(join(left, right));
        }
        return lines;
    }

    private static String adherenceText(JSONObject block) throws ApiException {
        if (!block.has("total") && !block.has("done") && !block.has("rate")) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        if (block.has("done") && !block.isNull("done")) {
            text.append(JsonReads.number(block, "done"));
        }
        if (block.has("total") && !block.isNull("total")) {
            if (text.length() > 0) {
                text.append('/');
            }
            text.append(JsonReads.number(block, "total"));
        }
        if (block.has("rate") && !block.isNull("rate")) {
            if (!(block.opt("rate") instanceof Number)) {
                throw ApiException.response(200);
            }
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(block.opt("rate"));
        }
        return text.toString();
    }

    private static void line(StringBuilder text, String label, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (text.length() > 0) {
            text.append('\n');
        }
        text.append(label).append(": ").append(value);
    }

    private static void section(StringBuilder text, String label, List<String> lines) {
        if (lines == null) {
            return;
        }
        if (text.length() > 0) {
            text.append('\n');
        }
        text.append(label);
        if (lines.isEmpty()) {
            text.append(": 없음");
            return;
        }
        for (String line : lines) {
            text.append("\n- ").append(line);
        }
    }

    private static String join(String left, String right) {
        if (left == null || left.isEmpty()) {
            return blank(right);
        }
        if (right == null || right.isEmpty()) {
            return left;
        }
        return left + " ~ " + right;
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }
}
