package com.example.yakyeon2.fcm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Classifies an FCM data payload into one of the eight Yakyeon notice kinds.
 * Sending stays on the server. This only chooses the copy and skips a missed
 * notice when the payload already says the dose was taken.
 */
public final class PushMessages {

    public enum Kind {
        DOSE_REMINDER,
        MISSED_DOSE,
        END_SOON,
        BOX_TAKEN,
        BOX_MISSED,
        DEVICE_FAULT,
        PATIENT_LINKED,
        PASSTHROUGH
    }

    public static final class Incoming {
        public final Kind kind;
        public final String hospitalName;
        public final Integer daysLeft;
        public final boolean suppressBecauseTaken;
        public final String dedupeKey;

        private Incoming(
                Kind kind,
                String hospitalName,
                Integer daysLeft,
                boolean suppressBecauseTaken,
                String dedupeKey
        ) {
            this.kind = kind;
            this.hospitalName = hospitalName;
            this.daysLeft = daysLeft;
            this.suppressBecauseTaken = suppressBecauseTaken;
            this.dedupeKey = dedupeKey;
        }
    }

    private PushMessages() {
    }

    public static Incoming classify(Map<String, String> data) {
        String type = upper(first(data, "type", "notification_type", "event", "kind"));
        if (type.isEmpty()) {
            type = upper(first(data, "setting_key"));
        }
        Kind kind = kindOf(type);
        Integer days = daysOf(type, first(data, "days_left", "days_before", "remaining_days"));
        if (kind == Kind.END_SOON && days == null) {
            days = 3;
        }
        boolean taken = alreadyTaken(data);
        boolean missed = kind == Kind.MISSED_DOSE || kind == Kind.BOX_MISSED;
        String hospital = first(data, "hospital_name", "hospital");
        String key = dedupeKey(data, kind);
        return new Incoming(kind, hospital, days, missed && taken, key);
    }

    /**
     * @return false when this key was already accepted
     */
    public static boolean accept(List<String> recent, String key) {
        if (key == null || key.isEmpty()) {
            return true;
        }
        if (recent == null) {
            return true;
        }
        if (recent.contains(key)) {
            return false;
        }
        recent.add(key);
        while (recent.size() > 64) {
            recent.remove(0);
        }
        return true;
    }

    public static List<String> copyRecent(List<String> recent) {
        return recent == null ? new ArrayList<>() : new ArrayList<>(recent);
    }

    private static Kind kindOf(String type) {
        if (type.isEmpty() || "DEVICE".equals(type) || "GUARDIAN_OPEN".equals(type) || "IR_TRIGGER".equals(type)) {
            return Kind.PASSTHROUGH;
        }
        if ("DOSE_REMINDER".equals(type) || "DOSE_DUE".equals(type)) {
            return Kind.DOSE_REMINDER;
        }
        if ("MISSED".equals(type) || "DOSE_MISSED".equals(type)) {
            return Kind.MISSED_DOSE;
        }
        if (type.startsWith("END_SOON") || "END_3".equals(type) || "END_1".equals(type)
                || "ENDING_3".equals(type) || "ENDING_1".equals(type)) {
            return Kind.END_SOON;
        }
        if ("BUTTON_PRESS".equals(type) || "BOX_TAKEN".equals(type) || "DEVICE_TAKEN".equals(type)
                || "PILLBOX_TAKEN".equals(type) || "GUARDIAN_TAKEN".equals(type)) {
            return Kind.BOX_TAKEN;
        }
        if ("BOX_MISSED".equals(type) || "DEVICE_MISSED".equals(type) || "PILLBOX_MISSED".equals(type)
                || "GUARDIAN_MISSED".equals(type)) {
            return Kind.BOX_MISSED;
        }
        if ("DEVICE_FAULT".equals(type) || "DEVICE_ERROR".equals(type) || "DEVICE_ABNORMAL".equals(type)
                || "PILLBOX_FAULT".equals(type) || "GUARDIAN_DEVICE".equals(type)) {
            return Kind.DEVICE_FAULT;
        }
        if ("PATIENT_LINKED".equals(type) || "GUARDIAN_LINKED".equals(type)
                || "CARE_LINKED".equals(type) || "PATIENT_PERMISSION".equals(type)) {
            return Kind.PATIENT_LINKED;
        }
        return Kind.PASSTHROUGH;
    }

    private static Integer daysOf(String type, String raw) {
        Integer parsed = null;
        if (!raw.isEmpty()) {
            try {
                int value = Integer.parseInt(raw);
                if (value == 1 || value == 3) {
                    parsed = value;
                }
            } catch (NumberFormatException ignored) {
                parsed = null;
            }
        }
        if (parsed != null) {
            return parsed;
        }
        if (type.endsWith("_1") || "END_1".equals(type) || "ENDING_1".equals(type)) {
            return 1;
        }
        if (type.endsWith("_3") || "END_3".equals(type) || "ENDING_3".equals(type)) {
            return 3;
        }
        return null;
    }

    private static boolean alreadyTaken(Map<String, String> data) {
        String taken = upper(first(data, "taken", "already_taken"));
        if ("TRUE".equals(taken) || "1".equals(taken) || "Y".equals(taken)) {
            return true;
        }
        if (!first(data, "taken_at").isEmpty()) {
            return true;
        }
        String status = upper(first(data, "status"));
        return "NORMAL".equals(status) || "DELAYED".equals(status)
                || "COMPLETED".equals(status) || "TAKEN".equals(status);
    }

    private static String dedupeKey(Map<String, String> data, Kind kind) {
        String id = first(data, "notification_id");
        if (!id.isEmpty()) {
            return "id:" + id;
        }
        if (kind == Kind.PASSTHROUGH) {
            return "";
        }
        String dose = first(data, "scheduled_dose_id", "dose_id");
        if (dose.isEmpty()) {
            return "";
        }
        return kind.name() + ":" + dose;
    }

    private static String first(Map<String, String> data, String... keys) {
        if (data == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            String value = data.get(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.US);
    }
}
