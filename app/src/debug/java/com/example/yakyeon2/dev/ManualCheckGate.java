package com.example.yakyeon2.dev;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.yakyeon2.R;
import com.google.android.material.button.MaterialButton;

/**
 * Debug-only control that opens the local screens without a server session.
 */
public final class ManualCheckGate {

    private ManualCheckGate() {
    }

    public static boolean available() {
        return true;
    }

    public static void attachLoginButton(Context context, View root, Runnable onClick) {
        View login = root.findViewById(R.id.button_login);
        if (login == null || !(login.getParent() instanceof ViewGroup)) {
            return;
        }
        ViewGroup parent = (ViewGroup) login.getParent();
        int index = parent.indexOfChild(login) + 1;
        MaterialButton button = new MaterialButton(
                context,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
        );
        button.setId(R.id.button_manual_check);
        button.setText(R.string.auth_manual_check);
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setCornerRadius(dp(context, 26));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setStrokeWidth(dp(context, 1));
        int color = context.getColor(R.color.yakyeon_muted);
        button.setStrokeColor(ColorStateList.valueOf(color));
        button.setTextColor(color);
        button.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.white)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 48)
        );
        params.topMargin = dp(context, 10);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> onClick.run());
        parent.addView(button, index);

        TextView note = new TextView(context);
        note.setText(R.string.auth_manual_check_note);
        note.setTextColor(color);
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        noteParams.topMargin = dp(context, 6);
        note.setLayoutParams(noteParams);
        parent.addView(note, index + 1);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
