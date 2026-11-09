package com.example.yakyeon2.auth;

import com.example.yakyeon2.data.AccountType;
import com.example.yakyeon2.data.SignupDraft;
import com.example.yakyeon2.navigation.AppNavigator;
import com.example.yakyeon2.navigation.AuthScreen;

public final class AuthFlow {

    public enum TypeStep {
        NEED_TYPE,
        CAREGIVER_NOTICE,
        FORM
    }

    public enum RecoveryTab {
        ID,
        PASSWORD
    }

    private final AppNavigator navigator;
    private final SignupDraft draft;
    private RecoveryTab recoveryTab = RecoveryTab.ID;

    public AuthFlow(AppNavigator navigator, SignupDraft draft) {
        this.navigator = navigator;
        this.draft = draft;
    }

    public String screen() {
        return navigator.currentScreen();
    }

    public void start() {
        navigator.showLoginRoot();
    }

    public RecoveryTab recoveryTab() {
        return recoveryTab;
    }

    public boolean openRecovery(RecoveryTab tab) {
        if (!AuthScreen.LOGIN.equals(navigator.currentScreen())) {
            return false;
        }
        recoveryTab = tab == null ? RecoveryTab.ID : tab;
        navigator.openNonMain(AuthScreen.ACCOUNT_RECOVERY);
        return true;
    }

    public void showRecoveryTab(RecoveryTab tab) {
        if (tab == null || !AuthScreen.ACCOUNT_RECOVERY.equals(navigator.currentScreen())) {
            return;
        }
        recoveryTab = tab;
    }

    public void restoreRecoveryTab(RecoveryTab tab) {
        if (tab != null) {
            recoveryTab = tab;
        }
    }

    public boolean openRecoveryVerify() {
        if (!AuthScreen.ACCOUNT_RECOVERY.equals(navigator.currentScreen())) {
            return false;
        }
        navigator.openNonMain(AuthScreen.ACCOUNT_RECOVERY_VERIFY);
        return true;
    }

    public boolean openRecoveryPassword() {
        String screen = navigator.currentScreen();
        if (!AuthScreen.ACCOUNT_RECOVERY.equals(screen)
                && !AuthScreen.ACCOUNT_RECOVERY_VERIFY.equals(screen)) {
            return false;
        }
        navigator.openNonMain(AuthScreen.ACCOUNT_RECOVERY_PASSWORD);
        return true;
    }

    public boolean openRecoveryResult() {
        if (AuthScreen.ACCOUNT_RECOVERY_VERIFY.equals(navigator.currentScreen())) {
            navigator.pop();
        }
        if (!AuthScreen.ACCOUNT_RECOVERY.equals(navigator.currentScreen())) {
            return false;
        }
        navigator.openNonMain(AuthScreen.ACCOUNT_RECOVERY_RESULT);
        return true;
    }

    public boolean openRecoveryDone() {
        if (!AuthScreen.isRecovery(navigator.currentScreen())) {
            return false;
        }
        while (!AuthScreen.ACCOUNT_RECOVERY.equals(navigator.currentScreen()) && navigator.pop()) {
            // 비밀번호 입력 화면은 인증 정보가 폐기된 뒤 다시 보이지 않게 한다.
        }
        if (!AuthScreen.ACCOUNT_RECOVERY.equals(navigator.currentScreen())) {
            return false;
        }
        navigator.openNonMain(AuthScreen.ACCOUNT_RECOVERY_DONE);
        return true;
    }

    public boolean backToRecoveryForm() {
        if (!AuthScreen.isRecovery(navigator.currentScreen())) {
            return false;
        }
        return navigator.popTo(AuthScreen.ACCOUNT_RECOVERY);
    }

    public boolean reopenRecoveryVerify() {
        if (!AuthScreen.isRecovery(navigator.currentScreen())) {
            return false;
        }
        while (!AuthScreen.ACCOUNT_RECOVERY.equals(navigator.currentScreen()) && navigator.pop()) {
            // 인증을 처음부터 다시 하므로 그 위의 입력 화면을 걷어 낸다.
        }
        if (!AuthScreen.ACCOUNT_RECOVERY.equals(navigator.currentScreen())) {
            return false;
        }
        navigator.openNonMain(AuthScreen.ACCOUNT_RECOVERY_VERIFY);
        return true;
    }

    public boolean restartRecovery(RecoveryTab tab) {
        if (!AuthScreen.isRecovery(navigator.currentScreen())) {
            return false;
        }
        while (navigator.pop()) {
            // 로그인 루트만 남긴다.
        }
        recoveryTab = tab == null ? RecoveryTab.ID : tab;
        navigator.openNonMain(AuthScreen.ACCOUNT_RECOVERY);
        return true;
    }

    public boolean openType(SignupConsent consent) {
        if (consent == null || !consent.allowsSignup()) {
            return false;
        }
        if (AuthScreen.SIGNUP_TYPE.equals(navigator.currentScreen())) {
            return true;
        }
        if (!AuthScreen.LOGIN.equals(navigator.currentScreen())) {
            return false;
        }
        navigator.openNonMain(AuthScreen.SIGNUP_TYPE);
        return true;
    }

    public TypeStep next(AccountType type) {
        if (type == null) {
            return TypeStep.NEED_TYPE;
        }
        draft.setAccountType(type);
        if (type == AccountType.CAREGIVER) {
            return TypeStep.CAREGIVER_NOTICE;
        }
        openForm();
        return TypeStep.FORM;
    }

    public void continueAfterCaregiverNotice() {
        openForm();
    }

    public void returnToLogin() {
        while (navigator.pop()) {
            // 가입 화면을 걷어 내고 로그인 루트로 돌아간다.
        }
        draft.clear();
    }

    public boolean back() {
        return navigator.pop();
    }

    private void openForm() {
        if (!AuthScreen.SIGNUP_FORM.equals(navigator.currentScreen())) {
            navigator.openNonMain(AuthScreen.SIGNUP_FORM);
        }
    }
}
