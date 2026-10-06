package com.example.yakyeon.ui.auth;

public final class SignupDraft {

    public enum Type {
        SELF,
        GUARDIAN
    }

    private final Type type;
    private final String name;
    private final String birthDate;
    private final String email;
    private final String phone;
    private final String userId;
    private final String password;
    private final String passwordConfirm;

    public SignupDraft(
            Type type,
            String name,
            String birthDate,
            String email,
            String phone,
            String userId,
            String password,
            String passwordConfirm
    ) {
        this.type = type;
        this.name = name;
        this.birthDate = birthDate;
        this.email = email;
        this.phone = phone;
        this.userId = userId;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }
}
