package com.example.yakyeon2.auth;

import android.text.InputFilter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LatinCredentialFilterTest {

    @Test
    public void idKeepsAsciiLettersAndDigits() {
        assertNull(apply(LatinCredentialFilter.Kind.ID, "eden"));
        assertNull(apply(LatinCredentialFilter.Kind.ID, "Eden"));
        assertNull(apply(LatinCredentialFilter.Kind.ID, "eden123"));
    }

    @Test
    public void idDropsHangulOnTypingAndPaste() {
        assertEquals("", apply(LatinCredentialFilter.Kind.ID, "아이디"));
        assertEquals("eden123", apply(LatinCredentialFilter.Kind.ID, "eden한글123"));
        assertEquals("eden", apply(LatinCredentialFilter.Kind.ID, "eden!"));
    }

    @Test
    public void passwordKeepsLettersDigitsAndAsciiSymbols() {
        assertNull(apply(LatinCredentialFilter.Kind.PASSWORD, "abcde12!"));
        assertNull(apply(LatinCredentialFilter.Kind.PASSWORD, "Ab9#"));
        assertEquals("abcde12!", apply(LatinCredentialFilter.Kind.PASSWORD, "비밀abcde12!"));
        assertEquals("ab!", apply(LatinCredentialFilter.Kind.PASSWORD, "a한글b!"));
    }

    @Test
    public void passwordDoesNotKeepHangulOrWhitespace() {
        assertEquals("", apply(LatinCredentialFilter.Kind.PASSWORD, "비밀번호"));
        assertEquals("abc", apply(LatinCredentialFilter.Kind.PASSWORD, "abc 한글"));
        assertFalse(LatinCredentialFilter.allows(LatinCredentialFilter.Kind.PASSWORD, '가'));
        assertTrue(LatinCredentialFilter.allows(LatinCredentialFilter.Kind.PASSWORD, 'Z'));
    }

    @Test
    public void consentSectionsKeepBothDocuments() {
        String body = "약연 서비스 이용약관 및 동의\n이용약관\n제1조 목적\n약관 본문\n"
                + ConsentSections.PRIVACY_MARKER
                + "\n민감정보 처리 항목\n서비스 이용 고지\n고지 본문";
        ConsentSections sections = ConsentSections.split(body);
        assertTrue(sections.terms.contains("이용약관"));
        assertTrue(sections.terms.contains("제1조 목적"));
        assertFalse(sections.terms.contains("민감정보 처리 항목"));
        assertTrue(sections.otherAgreements.startsWith(ConsentSections.PRIVACY_MARKER));
        assertTrue(sections.otherAgreements.contains("민감정보 처리 항목"));
        assertTrue(sections.otherAgreements.contains("서비스 이용 고지"));
        assertTrue(sections.otherAgreements.contains("고지 본문"));
    }

    private static CharSequence apply(LatinCredentialFilter.Kind kind, String value) {
        InputFilter filter = LatinCredentialFilter.filter(kind);
        return filter.filter(value, 0, value.length(), null, 0, 0);
    }
}
