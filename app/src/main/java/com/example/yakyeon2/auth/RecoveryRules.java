package com.example.yakyeon2.auth;

import com.example.yakyeon2.api.ApiCodes;

public final class RecoveryRules {

    /** Short pause after a rate-limit response so the same request is not sent again immediately. */
    public static final long RATE_LIMIT_COOLDOWN_MS = 60_000L;

    public enum Notice {
        ACCOUNT_MISSING,
        VERIFICATION_RESTART,
        RATE_LIMIT,
        PASSWORD_MISMATCH,
        VALIDATION,
        OTHER
    }

    private RecoveryRules() {
    }

    /**
     * {@code ACCOUNT_NOT_FOUND} keeps the phone proof so the name or id can be edited
     * and sent again. {@code VERIFICATION_NOT_COMPLETED} discards it.
     */
    public static boolean keepsProof(String code) {
        return !ApiCodes.VERIFICATION_NOT_COMPLETED.equals(code);
    }

    public static Notice noticeFor(String code) {
        if (ApiCodes.ACCOUNT_NOT_FOUND.equals(code)) {
            return Notice.ACCOUNT_MISSING;
        }
        if (ApiCodes.VERIFICATION_NOT_COMPLETED.equals(code)) {
            return Notice.VERIFICATION_RESTART;
        }
        if (ApiCodes.RATE_LIMITED.equals(code) || ApiCodes.VERIFICATION_RATE_LIMITED.equals(code)) {
            return Notice.RATE_LIMIT;
        }
        if (ApiCodes.PASSWORD_MISMATCH.equals(code)) {
            return Notice.PASSWORD_MISMATCH;
        }
        if (ApiCodes.VALIDATION_ERROR.equals(code)) {
            return Notice.VALIDATION;
        }
        return Notice.OTHER;
    }

    public static boolean blocksRetry(long notBeforeMillis, long nowMillis) {
        return notBeforeMillis > nowMillis;
    }
}
