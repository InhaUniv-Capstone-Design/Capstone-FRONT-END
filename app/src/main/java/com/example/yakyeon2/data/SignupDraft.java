package com.example.yakyeon2.data;

public final class SignupDraft {

    private AccountType accountType;

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public void clear() {
        accountType = null;
    }
}
