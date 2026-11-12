package com.example.yakyeon2.dev;

import android.content.Context;
import android.view.View;

/**
 * The shared test build does not offer a login bypass.
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
