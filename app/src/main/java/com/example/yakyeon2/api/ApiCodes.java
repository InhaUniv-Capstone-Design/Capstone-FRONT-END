package com.example.yakyeon2.api;

public final class ApiCodes {

    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String REFRESH_TOKEN_REUSED = "REFRESH_TOKEN_REUSED";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    public static final String CURRENT_PASSWORD_MISMATCH = "CURRENT_PASSWORD_MISMATCH";
    public static final String PASSWORD_MISMATCH = "PASSWORD_MISMATCH";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String INVALID_NOTIFICATION_SETTING = "INVALID_NOTIFICATION_SETTING";
    public static final String INVALID_PHONE_FORMAT = "INVALID_PHONE_FORMAT";
    public static final String VERIFICATION_RATE_LIMITED = "VERIFICATION_RATE_LIMITED";
    public static final String VERIFICATION_EXPIRED = "VERIFICATION_EXPIRED";
    public static final String VERIFICATION_NOT_FOUND = "VERIFICATION_NOT_FOUND";
    public static final String VERIFICATION_ALREADY_COMPLETED = "VERIFICATION_ALREADY_COMPLETED";
    public static final String VERIFICATION_ATTEMPTS_EXCEEDED = "VERIFICATION_ATTEMPTS_EXCEEDED";
    public static final String REQUIRED_TERMS_NOT_AGREED = "REQUIRED_TERMS_NOT_AGREED";
    public static final String VERIFICATION_NOT_COMPLETED = "VERIFICATION_NOT_COMPLETED";
    public static final String DUPLICATE_LOGIN_ID = "DUPLICATE_LOGIN_ID";
    public static final String DUPLICATE_PHONE = "DUPLICATE_PHONE";
    public static final String TERMS_VERSION_OUTDATED = "TERMS_VERSION_OUTDATED";
    public static final String DEVICE_NOT_FOUND = "DEVICE_NOT_FOUND";
    public static final String PATIENT_NOT_FOUND = "PATIENT_NOT_FOUND";
    public static final String SHARE_NOT_GRANTED = "SHARE_NOT_GRANTED";
    public static final String INSUFFICIENT_PERMISSION = "INSUFFICIENT_PERMISSION";
    public static final String GUARDIAN_LINK_NOT_FOUND = "GUARDIAN_LINK_NOT_FOUND";
    public static final String GUARDIAN_LINK_NOT_ACTIVE = "GUARDIAN_LINK_NOT_ACTIVE";
    public static final String CONSENT_REQUIRED = "CONSENT_REQUIRED";
    public static final String INTERACTION_NOT_FOUND = "INTERACTION_NOT_FOUND";
    public static final String PRESCRIPTION_NOT_FOUND = "PRESCRIPTION_NOT_FOUND";
    public static final String COMPARTMENT_MAPPING_NOT_FOUND = "COMPARTMENT_MAPPING_NOT_FOUND";
    public static final String DEVICE_ALREADY_REGISTERED = "DEVICE_ALREADY_REGISTERED";
    public static final String DEVICE_SERIAL_IN_USE = "DEVICE_SERIAL_IN_USE";
    public static final String COMPARTMENT_OCCUPIED = "COMPARTMENT_OCCUPIED";
    public static final String PRESCRIPTION_ALREADY_ASSIGNED = "PRESCRIPTION_ALREADY_ASSIGNED";
    public static final String PRESCRIPTION_ALREADY_ENDED = "PRESCRIPTION_ALREADY_ENDED";
    public static final String MAPPING_ALREADY_ENDED = "MAPPING_ALREADY_ENDED";
    public static final String MAPPING_NOT_PENDING_LOAD = "MAPPING_NOT_PENDING_LOAD";
    public static final String MAPPING_NOT_ACTIVE = "MAPPING_NOT_ACTIVE";
    public static final String INVALID_COMPARTMENT_NO = "INVALID_COMPARTMENT_NO";
    public static final String INVALID_MEDICATION = "INVALID_MEDICATION";
    public static final String INVALID_SCHEDULE = "INVALID_SCHEDULE";
    public static final String INVALID_TAKEN_AT = "INVALID_TAKEN_AT";
    public static final String INVALID_QUERY = "INVALID_QUERY";
    public static final String INVALID_SEARCH_QUERY = "INVALID_SEARCH_QUERY";
    public static final String INVALID_IMAGE = "INVALID_IMAGE";
    public static final String DOSE_NOT_FOUND = "DOSE_NOT_FOUND";
    public static final String DOSE_EXPIRED = "DOSE_EXPIRED";
    public static final String DOSE_NOT_DECIDED = "DOSE_NOT_DECIDED";
    public static final String DOSE_RECORD_FAILED = "DOSE_RECORD_FAILED";
    public static final String MEDICATION_NOT_FOUND = "MEDICATION_NOT_FOUND";
    public static final String MEDICATION_ALREADY_ENDED = "MEDICATION_ALREADY_ENDED";
    public static final String MEDICATION_NOT_STARTED = "MEDICATION_NOT_STARTED";
    public static final String DRUG_NOT_FOUND = "DRUG_NOT_FOUND";
    public static final String OCR_UNAVAILABLE = "OCR_UNAVAILABLE";
    public static final String SCHEDULE_CONFLICT = "SCHEDULE_CONFLICT";

    private ApiCodes() {
    }

    public static boolean endsSession(String code) {
        return REFRESH_TOKEN_REUSED.equals(code) || INVALID_REFRESH_TOKEN.equals(code);
    }
}
