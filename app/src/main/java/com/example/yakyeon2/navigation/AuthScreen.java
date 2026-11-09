package com.example.yakyeon2.navigation;

public final class AuthScreen {

    public static final String LOGIN = "login";
    public static final String SIGNUP_TYPE = "signup-type";
    public static final String SIGNUP_FORM = "signup-form";
    public static final String ACCOUNT_RECOVERY = "account-recovery";
    public static final String ACCOUNT_RECOVERY_VERIFY = "account-recovery-verify";
    public static final String ACCOUNT_RECOVERY_RESULT = "account-recovery-result";
    public static final String ACCOUNT_RECOVERY_PASSWORD = "account-recovery-password";
    public static final String ACCOUNT_RECOVERY_DONE = "account-recovery-done";

    private AuthScreen() {
    }

    public static boolean isRecovery(String screen) {
        return ACCOUNT_RECOVERY.equals(screen)
                || ACCOUNT_RECOVERY_VERIFY.equals(screen)
                || ACCOUNT_RECOVERY_RESULT.equals(screen)
                || ACCOUNT_RECOVERY_PASSWORD.equals(screen)
                || ACCOUNT_RECOVERY_DONE.equals(screen);
    }
}
