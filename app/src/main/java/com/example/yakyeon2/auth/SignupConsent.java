package com.example.yakyeon2.auth;

public final class SignupConsent {

    private final boolean terms;
    private final boolean privacy;
    private final boolean notice;
    private final boolean sensitive;

    public SignupConsent(boolean terms, boolean privacy, boolean notice, boolean sensitive) {
        this.terms = terms;
        this.privacy = privacy;
        this.notice = notice;
        this.sensitive = sensitive;
    }

    public boolean allowsSignup() {
        return terms && privacy && notice && sensitive;
    }
}
