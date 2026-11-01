package com.example.yakyeon2.data;

public final class ProfileSnapshot {

    public final long userId;
    public final String loginId;
    public final String name;
    public final String birthDate;
    public final String phoneMasked;
    public final String accountType;

    public ProfileSnapshot(
            long userId,
            String loginId,
            String name,
            String birthDate,
            String phoneMasked,
            String accountType
    ) {
        this.userId = userId;
        this.loginId = loginId;
        this.name = name;
        this.birthDate = birthDate;
        this.phoneMasked = phoneMasked;
        this.accountType = accountType;
    }
}
