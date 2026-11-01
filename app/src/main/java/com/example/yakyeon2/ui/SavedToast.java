package com.example.yakyeon2.ui;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon2.R;

public final class SavedToast {

    private SavedToast() {
    }

    public static void show(
            @NonNull AppCompatActivity activity,
            @NonNull CharSequence title,
            @NonNull CharSequence detail
    ) {
        ViewGroup host = activity.findViewById(R.id.screen_container);
        if (host == null) {
            return;
        }
        View toast = LayoutInflater.from(activity).inflate(R.layout.view_saved_toast, host, false);
        ((TextView) toast.findViewById(R.id.text_toast_title)).setText(title);
        ((TextView) toast.findViewById(R.id.text_toast_detail)).setText(detail);
        int margin = Math.round(16f * activity.getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        params.setMargins(margin, 0, margin, margin);
        host.addView(toast, params);
        toast.bringToFront();
        toast.postDelayed(() -> {
            if (toast.getParent() == host) {
                host.removeView(toast);
            }
        }, 2800);
    }
}
