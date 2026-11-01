package com.example.yakyeon2.auth;

public final class ConsentSections {

    public static final String PRIVACY_MARKER = "약연 개인정보 수집·이용 및 민감정보 처리 동의서";

    public final String terms;
    public final String otherAgreements;

    public ConsentSections(String terms, String otherAgreements) {
        this.terms = terms;
        this.otherAgreements = otherAgreements;
    }

    public static ConsentSections split(String body) {
        String source = body == null ? "" : body;
        int privacy = source.indexOf(PRIVACY_MARKER);
        if (privacy < 0) {
            return new ConsentSections(source.trim(), "");
        }
        return new ConsentSections(
                source.substring(0, privacy).trim(),
                source.substring(privacy).trim()
        );
    }
}
