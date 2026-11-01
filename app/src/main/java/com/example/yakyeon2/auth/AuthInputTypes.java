package com.example.yakyeon2.auth;

import android.text.InputType;

public final class AuthInputTypes {

    public static final int ID = InputType.TYPE_CLASS_TEXT;

    /**
     * Password variation and visible-password variation stay off.
     * Those variations keep the IME in password mode, which drops Latin entry.
     */
    public static final int PASSWORD = InputType.TYPE_CLASS_TEXT;

    private AuthInputTypes() {
    }

    public static boolean passwordKeepsLetterInput() {
        int variation = PASSWORD & InputType.TYPE_MASK_VARIATION;
        return (PASSWORD & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
                && variation == 0;
    }
}
