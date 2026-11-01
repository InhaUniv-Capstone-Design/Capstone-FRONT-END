package com.example.yakyeon2.data;

public final class AccountRoles {

    public static final String PATIENT = "PATIENT";
    public static final String GUARDIAN = "GUARDIAN";

    private AccountRoles() {
    }

    public static AccountType toLocal(String serverAccountType) {
        if (PATIENT.equals(serverAccountType)) {
            return AccountType.SELF;
        }
        if (GUARDIAN.equals(serverAccountType)) {
            return AccountType.CAREGIVER;
        }
        return null;
    }

    public static String toServer(AccountType type) {
        if (type == AccountType.SELF) {
            return PATIENT;
        }
        if (type == AccountType.CAREGIVER) {
            return GUARDIAN;
        }
        return null;
    }
}
