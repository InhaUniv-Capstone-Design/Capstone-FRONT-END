package com.example.yakyeon2.api;

public final class ApiFailure {

    public enum Kind {
        NETWORK,
        AUTHENTICATION,
        VALIDATION,
        SESSION,
        SERVER,
        RESPONSE,
        OTHER
    }

    private ApiFailure() {
    }

    public static Kind kind(int status, String code) {
        if (ApiCodes.endsSession(code)) {
            return Kind.SESSION;
        }
        if (ApiCodes.INVALID_CREDENTIALS.equals(code)
                || ApiCodes.CURRENT_PASSWORD_MISMATCH.equals(code)
                || ApiCodes.UNAUTHENTICATED.equals(code)) {
            return Kind.AUTHENTICATION;
        }
        if (ApiCodes.VALIDATION_ERROR.equals(code)
                || ApiCodes.PASSWORD_MISMATCH.equals(code)
                || ApiCodes.INVALID_NOTIFICATION_SETTING.equals(code)) {
            return Kind.VALIDATION;
        }
        if (status >= 500) {
            return Kind.SERVER;
        }
        return Kind.OTHER;
    }
}
