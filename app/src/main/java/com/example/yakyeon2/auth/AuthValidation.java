package com.example.yakyeon2.auth;

import com.example.yakyeon2.api.AuthContracts;

public final class AuthValidation {

    public static final int LOGIN_ID_MAX = 100;
    public static final int LOGIN_PASSWORD_MAX = 128;

    public enum LoginCheck {
        EMPTY_ID,
        EMPTY_PASSWORD,
        ID_TOO_LONG,
        PASSWORD_TOO_LONG,
        NOT_CONNECTED
    }

    public enum SignupCheck {
        MISSING_NAME,
        MISSING_BIRTHDATE,
        MISSING_PHONE,
        MISSING_ID,
        MISSING_PASSWORD,
        MISSING_PASSWORD_CONFIRM,
        PASSWORD_MISMATCH,
        INVALID_ID,
        INVALID_PASSWORD,
        INVALID_PHONE,
        INVALID_BIRTHDATE,
        NOT_CONNECTED
    }

    private AuthValidation() {
    }

    public static LoginCheck login(String id, String password) {
        if (isBlank(id)) {
            return LoginCheck.EMPTY_ID;
        }
        if (id.trim().length() > LOGIN_ID_MAX) {
            return LoginCheck.ID_TOO_LONG;
        }
        if (password == null || password.isEmpty()) {
            return LoginCheck.EMPTY_PASSWORD;
        }
        if (password.length() > LOGIN_PASSWORD_MAX) {
            return LoginCheck.PASSWORD_TOO_LONG;
        }
        return LoginCheck.NOT_CONNECTED;
    }

    public static SignupCheck signup(
            String name,
            String birthdate,
            String phone,
            String id,
            String password,
            String passwordConfirm
    ) {
        if (isBlank(name)) {
            return SignupCheck.MISSING_NAME;
        }
        if (isBlank(birthdate)) {
            return SignupCheck.MISSING_BIRTHDATE;
        }
        if (isBlank(phone)) {
            return SignupCheck.MISSING_PHONE;
        }
        if (isBlank(id)) {
            return SignupCheck.MISSING_ID;
        }
        if (password == null || password.isEmpty()) {
            return SignupCheck.MISSING_PASSWORD;
        }
        if (passwordConfirm == null || passwordConfirm.isEmpty()) {
            return SignupCheck.MISSING_PASSWORD_CONFIRM;
        }
        if (!password.equals(passwordConfirm)) {
            return SignupCheck.PASSWORD_MISMATCH;
        }
        if (!isValidId(id.trim())) {
            return SignupCheck.INVALID_ID;
        }
        if (!isValidPassword(password)) {
            return SignupCheck.INVALID_PASSWORD;
        }
        if (AuthContracts.phoneDigits(phone) == null) {
            return SignupCheck.INVALID_PHONE;
        }
        if (AuthContracts.isoDate(birthdate) == null) {
            return SignupCheck.INVALID_BIRTHDATE;
        }
        return SignupCheck.NOT_CONNECTED;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static boolean isValidId(String id) {
        return id.matches("[A-Za-z0-9]{6,20}");
    }

    static boolean isValidPassword(String password) {
        if (password.length() < 8 || password.length() > 64) {
            return false;
        }
        boolean letter = false;
        boolean digit = false;
        boolean special = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                letter = true;
            } else if (c >= '0' && c <= '9') {
                digit = true;
            } else if (!Character.isWhitespace(c)) {
                special = true;
            }
        }
        return letter && digit && special;
    }
}
