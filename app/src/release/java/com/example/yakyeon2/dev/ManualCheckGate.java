package com.example.yakyeon2.dev;

import android.content.Context;
import android.view.View;

/**
 * Release builds do not show the local screen check.
 */
public final class ManualCheckGate {

    private ManualCheckGate() {
    }

    public static boolean available() {
        return false;
    }

    public static void attachLoginButton(Context context, View root, Runnable onClick) {
        if (context == null || root == null || onClick == null) {
            return;
        }
    }
}
