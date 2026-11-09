package com.example.yakyeon2.ui;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.yakyeon2.R;

public final class SavedToast {

    private static final String TAG = "yakyeon-saved-toast";

    private SavedToast() {
    }

    public static void dismiss(@NonNull AppCompatActivity activity) {
        ViewGroup host = host(activity);
        if (host == null) {
            return;
        }
        View previous = host.findViewWithTag(TAG);
        if (previous != null) {
            host.removeView(previous);
        }
    }

    public static void show(
            @NonNull AppCompatActivity activity,
            @NonNull CharSequence title,
            @NonNull CharSequence detail
    ) {
        ViewGroup host = host(activity);
        if (host == null) {
            return;
        }
        View previous = host.findViewWithTag(TAG);
        if (previous != null) {
            host.removeView(previous);
        }
        View toast = LayoutInflater.from(activity).inflate(R.layout.view_saved_toast, host, false);
        toast.setTag(TAG);
        ((TextView) toast.findViewById(R.id.text_toast_title)).setText(title);
        TextView detailView = toast.findViewById(R.id.text_toast_detail);
        if (detail.length() == 0) {
            detailView.setVisibility(View.GONE);
        } else {
            detailView.setText(detail);
        }
        int margin = Math.round(16f * activity.getResources().getDisplayMetrics().density);
        if (host instanceof ConstraintLayout) {
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            params.bottomToTop = R.id.bottom_nav;
            params.setMargins(margin, 0, margin, margin);
            host.addView(toast, params);
        } else {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );
            params.setMargins(margin, 0, margin, margin);
            host.addView(toast, params);
        }
        toast.bringToFront();
        ViewGroup parent = host;
        toast.postDelayed(() -> {
            if (toast.getParent() == parent) {
                parent.removeView(toast);
            }
        }, 2800);
    }

    private static ViewGroup host(AppCompatActivity activity) {
        ViewGroup host = activity.findViewById(R.id.main);
        if (host == null) {
            host = activity.findViewById(R.id.screen_container);
        }
        return host;
    }
}
