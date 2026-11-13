package com.example.yakyeon2.ui;

import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

public final class ShortMessage {

    public static final int DURATION_MS = 1000;

    private ShortMessage() {
    }

    public static void show(@NonNull View anchor, @NonNull CharSequence message) {
        Snackbar snackbar = Snackbar.make(anchor, message, DURATION_MS);
        snackbar.getView().setFocusable(false);
        snackbar.getView().setFocusableInTouchMode(false);
        snackbar.show();
    }
}
