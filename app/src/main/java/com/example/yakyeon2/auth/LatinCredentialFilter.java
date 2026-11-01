package com.example.yakyeon2.auth;

import android.text.InputFilter;

public final class LatinCredentialFilter {

    public enum Kind {
        ID,
        PASSWORD
    }

    private LatinCredentialFilter() {
    }

    public static boolean allows(Kind kind, char c) {
        if (kind == Kind.ID) {
            return isAsciiLetter(c) || isDigit(c);
        }
        return c >= 0x21 && c <= 0x7E;
    }

    public static String keepAllowed(Kind kind, CharSequence source) {
        if (source == null || source.length() == 0) {
            return "";
        }
        StringBuilder kept = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (Character.isHighSurrogate(c)) {
                continue;
            }
            if (allows(kind, c)) {
                kept.append(c);
            }
        }
        return kept.toString();
    }

    public static InputFilter filter(Kind kind) {
        return (source, start, end, dest, dstart, dend) -> sanitize(kind, source, start, end);
    }

    private static CharSequence sanitize(Kind kind, CharSequence source, int start, int end) {
        if (source == null) {
            return "";
        }
        boolean changed = false;
        StringBuilder kept = new StringBuilder(end - start);
        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            if (Character.isHighSurrogate(c)) {
                changed = true;
                if (i + 1 < end && Character.isLowSurrogate(source.charAt(i + 1))) {
                    i++;
                }
                continue;
            }
            if (allows(kind, c)) {
                kept.append(c);
            } else {
                changed = true;
            }
        }
        if (!changed) {
            return null;
        }
        return kept.toString();
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
