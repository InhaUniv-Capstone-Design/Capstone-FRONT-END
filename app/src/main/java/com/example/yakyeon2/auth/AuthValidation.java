package com.example.yakyeon2.auth;

import com.example.yakyeon2.api.AuthContracts;

import java.time.LocalDate;
import java.time.ZoneId;

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

    public enum FindIdCheck {
        MISSING_NAME,
        MISSING_PHONE,
        INVALID_PHONE,
        READY
    }

    public enum ResetIdentityCheck {
        MISSING_ID,
        MISSING_PHONE,
        INVALID_ID,
        INVALID_PHONE,
        READY
    }

    public enum ResetPasswordCheck {
        MISSING_ID,
        MISSING_PHONE,
        MISSING_PASSWORD,
        MISSING_PASSWORD_CONFIRM,
        PASSWORD_MISMATCH,
        INVALID_ID,
        INVALID_PASSWORD,
        INVALID_PHONE,
        READY
    }

    public enum PasswordIssue {
        EMPTY,
        TOO_SHORT,
        TOO_LONG,
        NEED_LETTER,
        NEED_DIGIT,
        NEED_SPECIAL,
        OK
    }

    private AuthValidation() {
    }

    /** Trims the name and collapses repeated spaces. The server also ignores spaces. */
    public static String normalizePersonName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replaceAll("\\s+", " ");
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

    public static boolean isUnder14(String birthdate) {
        String iso = AuthContracts.isoDate(birthdate);
        if (iso == null) {
            return false;
        }
        LocalDate birth = LocalDate.parse(iso);
        return birth.plusYears(14).isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    public static FindIdCheck findId(String name, String phone) {
        if (normalizePersonName(name).isEmpty()) {
            return FindIdCheck.MISSING_NAME;
        }
        if (isBlank(phone)) {
            return FindIdCheck.MISSING_PHONE;
        }
        if (AuthContracts.phoneDigits(phone) == null) {
            return FindIdCheck.INVALID_PHONE;
        }
        return FindIdCheck.READY;
    }

    public static ResetIdentityCheck confirmResetIdentity(String loginId, String phone) {
        if (isBlank(loginId)) {
            return ResetIdentityCheck.MISSING_ID;
        }
        if (isBlank(phone)) {
            return ResetIdentityCheck.MISSING_PHONE;
        }
        if (!isValidId(loginId.trim())) {
            return ResetIdentityCheck.INVALID_ID;
        }
        if (AuthContracts.phoneDigits(phone) == null) {
            return ResetIdentityCheck.INVALID_PHONE;
        }
        return ResetIdentityCheck.READY;
    }

    public static ResetPasswordCheck resetPassword(
            String loginId,
            String phone,
            String password,
            String passwordConfirm
    ) {
        if (isBlank(loginId)) {
            return ResetPasswordCheck.MISSING_ID;
        }
        if (isBlank(phone)) {
            return ResetPasswordCheck.MISSING_PHONE;
        }
        if (password == null || password.isEmpty()) {
            return ResetPasswordCheck.MISSING_PASSWORD;
        }
        if (passwordConfirm == null || passwordConfirm.isEmpty()) {
            return ResetPasswordCheck.MISSING_PASSWORD_CONFIRM;
        }
        if (!password.equals(passwordConfirm)) {
            return ResetPasswordCheck.PASSWORD_MISMATCH;
        }
        if (!isValidId(loginId.trim())) {
            return ResetPasswordCheck.INVALID_ID;
        }
        if (!isValidPassword(password)) {
            return ResetPasswordCheck.INVALID_PASSWORD;
        }
        if (AuthContracts.phoneDigits(phone) == null) {
            return ResetPasswordCheck.INVALID_PHONE;
        }
        return ResetPasswordCheck.READY;
    }

    public static PasswordIssue passwordIssue(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordIssue.EMPTY;
        }
        if (password.length() < 8) {
            return PasswordIssue.TOO_SHORT;
        }
        if (password.length() > 64) {
            return PasswordIssue.TOO_LONG;
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
        if (!letter) {
            return PasswordIssue.NEED_LETTER;
        }
        if (!digit) {
            return PasswordIssue.NEED_DIGIT;
        }
        if (!special) {
            return PasswordIssue.NEED_SPECIAL;
        }
        return PasswordIssue.OK;
    }

    public static boolean canSubmitReset(String password, String confirm) {
        return passwordIssue(password) == PasswordIssue.OK && password.equals(confirm);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static boolean isValidId(String id) {
        return id.matches("[A-Za-z0-9]{6,20}");
    }

    static boolean isValidPassword(String password) {
        return passwordIssue(password) == PasswordIssue.OK;
    }
}
