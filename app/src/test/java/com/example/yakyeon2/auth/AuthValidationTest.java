package com.example.yakyeon2.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthValidationTest {

    private static final String VALID_ID = "user123";
    private static final String VALID_PASSWORD = "abcde12!";

    @Test
    public void loginChecksIdAndPasswordSeparatelyAndDoesNotSucceed() {
        assertEquals(AuthValidation.LoginCheck.EMPTY_ID, AuthValidation.login("  ", VALID_PASSWORD));
        assertEquals(AuthValidation.LoginCheck.EMPTY_PASSWORD, AuthValidation.login(VALID_ID, ""));
        assertEquals(AuthValidation.LoginCheck.NOT_CONNECTED, AuthValidation.login(VALID_ID, VALID_PASSWORD));
        assertEquals(AuthValidation.LoginCheck.ID_TOO_LONG, AuthValidation.login("a".repeat(101), VALID_PASSWORD));
        assertEquals(
                AuthValidation.LoginCheck.PASSWORD_TOO_LONG,
                AuthValidation.login(VALID_ID, "p".repeat(129))
        );
        assertEquals(AuthValidation.LoginCheck.NOT_CONNECTED, AuthValidation.login("a".repeat(100), "p".repeat(128)));
    }

    @Test
    public void signupBlocksMissingPasswordConfirmAndMismatch() {
        assertEquals(
                AuthValidation.SignupCheck.MISSING_PASSWORD_CONFIRM,
                signup(VALID_PASSWORD, "")
        );
        assertEquals(
                AuthValidation.SignupCheck.PASSWORD_MISMATCH,
                signup(VALID_PASSWORD, "abcde12?")
        );
    }

    @Test
    public void signupBlocksInvalidIdAndPasswordBeforeAnyAccountExists() {
        assertEquals(AuthValidation.SignupCheck.INVALID_ID, signup("ab", VALID_PASSWORD, VALID_PASSWORD));
        assertEquals(AuthValidation.SignupCheck.INVALID_PASSWORD, signup(VALID_ID, "abcdefg", "abcdefg"));
        assertEquals(
                AuthValidation.SignupCheck.NOT_CONNECTED,
                signup(VALID_ID, VALID_PASSWORD, VALID_PASSWORD)
        );
    }

    @Test
    public void requiredConsentItemsStayIndependent() {
        assertFalse(new SignupConsent(true, true, true, false).allowsSignup());
        assertFalse(new SignupConsent(false, true, true, true).allowsSignup());
        assertFalse(new SignupConsent(true, false, true, true).allowsSignup());
        assertFalse(new SignupConsent(true, true, false, true).allowsSignup());
        assertTrue(new SignupConsent(true, true, true, true).allowsSignup());
    }

    @Test
    public void signupNamesTheFirstMissingField() {
        assertEquals(
                AuthValidation.SignupCheck.MISSING_NAME,
                AuthValidation.signup(" ", "1962.04.18", "01012345678", VALID_ID, VALID_PASSWORD, VALID_PASSWORD)
        );
        assertEquals(
                AuthValidation.SignupCheck.MISSING_ID,
                AuthValidation.signup("이름", "1962.04.18", "01012345678", " ", VALID_PASSWORD, VALID_PASSWORD)
        );
    }

    @Test
    public void idAndPasswordInputTypesKeepLetterEntry() {
        assertEquals(android.text.InputType.TYPE_CLASS_TEXT, AuthInputTypes.ID);
        assertEquals(android.text.InputType.TYPE_CLASS_TEXT, AuthInputTypes.PASSWORD);
        assertEquals(0, AuthInputTypes.PASSWORD & android.text.InputType.TYPE_MASK_VARIATION);
        assertTrue(AuthInputTypes.passwordKeepsLetterInput());
    }

    @Test
    public void signupRejectsPhoneBirthAndOverlongPasswordBeforeAnyRequest() {
        assertEquals(
                AuthValidation.SignupCheck.INVALID_PHONE,
                AuthValidation.signup("이름", "1962.04.18", "1234", VALID_ID, VALID_PASSWORD, VALID_PASSWORD)
        );
        assertEquals(
                AuthValidation.SignupCheck.INVALID_BIRTHDATE,
                AuthValidation.signup("이름", "1962.13.18", "01012345678", VALID_ID, VALID_PASSWORD, VALID_PASSWORD)
        );
        String tooLong = "abcde12!" + "x".repeat(57);
        assertEquals(
                AuthValidation.SignupCheck.INVALID_PASSWORD,
                AuthValidation.signup("이름", "1962.04.18", "01012345678", VALID_ID, tooLong, tooLong)
        );
    }

    @Test
    public void findIdStopsBeforeAnyLookup() {
        assertEquals(AuthValidation.FindIdCheck.MISSING_NAME, AuthValidation.findId(" ", "01012345678"));
        assertEquals(AuthValidation.FindIdCheck.MISSING_PHONE, AuthValidation.findId("이름", " "));
        assertEquals(AuthValidation.FindIdCheck.INVALID_PHONE, AuthValidation.findId("이름", "0111234567"));
        assertEquals(AuthValidation.FindIdCheck.READY, AuthValidation.findId("이름", "01012345678"));
        assertEquals("홍 길동", AuthValidation.normalizePersonName("  홍   길동  "));
    }

    @Test
    public void resetIdentityOpensThePasswordStepOnlyWhenIdAndPhoneAreValid() {
        assertEquals(AuthValidation.ResetIdentityCheck.MISSING_ID, AuthValidation.confirmResetIdentity(" ", "01012345678"));
        assertEquals(AuthValidation.ResetIdentityCheck.MISSING_PHONE, AuthValidation.confirmResetIdentity(VALID_ID, " "));
        assertEquals(AuthValidation.ResetIdentityCheck.INVALID_ID, AuthValidation.confirmResetIdentity("ab", "01012345678"));
        assertEquals(AuthValidation.ResetIdentityCheck.INVALID_PHONE, AuthValidation.confirmResetIdentity(VALID_ID, "0111234567"));
        assertEquals(
                AuthValidation.ResetIdentityCheck.READY,
                AuthValidation.confirmResetIdentity(VALID_ID, "010-1234-5678")
        );
    }

    @Test
    public void resetPasswordStopsBeforeAnyChange() {
        assertEquals(
                AuthValidation.ResetPasswordCheck.MISSING_ID,
                AuthValidation.resetPassword(" ", "01012345678", VALID_PASSWORD, VALID_PASSWORD)
        );
        assertEquals(
                AuthValidation.ResetPasswordCheck.PASSWORD_MISMATCH,
                AuthValidation.resetPassword(VALID_ID, "01012345678", VALID_PASSWORD, "abcde12?")
        );
        assertEquals(
                AuthValidation.ResetPasswordCheck.INVALID_ID,
                AuthValidation.resetPassword("ab", "01012345678", VALID_PASSWORD, VALID_PASSWORD)
        );
        assertEquals(
                AuthValidation.ResetPasswordCheck.READY,
                AuthValidation.resetPassword(VALID_ID, "01012345678", VALID_PASSWORD, VALID_PASSWORD)
        );
    }

    @Test
    public void resetPasswordExplainsEachRuleAndStaysDisabledUntilTheyAllPass() {
        assertEquals(AuthValidation.PasswordIssue.TOO_SHORT, AuthValidation.passwordIssue("Ab1!"));
        assertEquals(AuthValidation.PasswordIssue.TOO_LONG, AuthValidation.passwordIssue("Ab1!" + "x".repeat(61)));
        assertEquals(AuthValidation.PasswordIssue.NEED_LETTER, AuthValidation.passwordIssue("1234567!"));
        assertEquals(AuthValidation.PasswordIssue.NEED_DIGIT, AuthValidation.passwordIssue("Abcdefg!"));
        assertEquals(AuthValidation.PasswordIssue.NEED_SPECIAL, AuthValidation.passwordIssue("Abcdefg1"));
        assertEquals(AuthValidation.PasswordIssue.OK, AuthValidation.passwordIssue(VALID_PASSWORD));
        assertFalse(AuthValidation.canSubmitReset(VALID_PASSWORD, "abcde12?"));
        assertFalse(AuthValidation.canSubmitReset("Ab1!", "Ab1!"));
        assertTrue(AuthValidation.canSubmitReset(VALID_PASSWORD, VALID_PASSWORD));
    }

    private static AuthValidation.SignupCheck signup(String password, String confirm) {
        return signup(VALID_ID, password, confirm);
    }

    private static AuthValidation.SignupCheck signup(String id, String password, String confirm) {
        return AuthValidation.signup("이름", "1962.04.18", "01012345678", id, password, confirm);
    }
}
