package com.example.yakyeon2.dev;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.yakyeon2.R;

/**
 * Debug builds can open the local home without a server session.
 * Release uses a different source file that does not add this control.
 */
public final class ManualCheckGate {

    private ManualCheckGate() {
    }

    public static boolean available() {
        return true;
    }

    public static void attachLoginButton(Context context, View root, Runnable onClick) {
        if (context == null || root == null || onClick == null || !(root instanceof ViewGroup)) {
            return;
        }
        if (root.findViewWithTag(TAG) != null) {
            return;
        }
        TextView button = new TextView(context);
        button.setTag(TAG);
        button.setText(R.string.auth_dev_home);
        button.setContentDescription(context.getString(R.string.auth_dev_home));
        button.setTextColor(context.getColor(R.color.yakyeon_brand));
        button.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        button.setGravity(android.view.Gravity.CENTER);
        int pad = Math.round(12f * context.getResources().getDisplayMetrics().density);
        button.setPadding(pad, pad, pad, pad);
        button.setMinHeight(Math.round(48f * context.getResources().getDisplayMetrics().density));
        button.setOnClickListener(v -> onClick.run());
        ((ViewGroup) root).addView(button);
    }

    private static final String TAG = "yakyeon-dev-home";
}
