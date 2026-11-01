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

    private final AppNavigator navigator;
    private final SignupDraft draft;

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
