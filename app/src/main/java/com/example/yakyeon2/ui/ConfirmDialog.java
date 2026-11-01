package com.example.yakyeon2.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.yakyeon2.R;

public final class ConfirmDialog {

    public enum Mark {
        NONE,
        TRASH,
        CHECK
    }

    public enum Tone {
        PRIMARY,
        DANGER
    }

    public interface Callback {
        void onConfirm();

        void onCancel();
    }

    private ConfirmDialog() {
    }

    public static void show(
            @NonNull android.content.Context context,
            @NonNull CharSequence title,
            @NonNull CharSequence message,
            @NonNull CharSequence confirmText,
            @Nullable CharSequence cancelText,
            @NonNull Callback callback
    ) {
        show(context, title, message, confirmText, cancelText, callback, Mark.NONE, Tone.PRIMARY);
    }

    public static void show(
            @NonNull android.content.Context context,
            @NonNull CharSequence title,
            @NonNull CharSequence message,
            @NonNull CharSequence confirmText,
            @Nullable CharSequence cancelText,
            @NonNull Callback callback,
            @NonNull Mark mark,
            @NonNull Tone tone
    ) {
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null);
        ((TextView) root.findViewById(R.id.text_confirm_title)).setText(title);
        TextView body = root.findViewById(R.id.text_confirm_message);
        if (message.toString().trim().isEmpty()) {
            body.setVisibility(View.GONE);
        } else {
            body.setVisibility(View.VISIBLE);
            body.setText(message);
        }
        FrameLayout iconWrap = root.findViewById(R.id.confirm_icon_wrap);
        ImageView icon = root.findViewById(R.id.image_confirm_icon);
        if (mark == Mark.NONE) {
            iconWrap.setVisibility(View.GONE);
        } else {
            iconWrap.setVisibility(View.VISIBLE);
            int plate = mark == Mark.TRASH ? R.drawable.bg_modal_icon_danger : R.drawable.bg_modal_icon_mint;
            int glyph = mark == Mark.TRASH ? R.drawable.ic_modal_trash : R.drawable.ic_modal_check_circle;
            icon.setBackgroundResource(plate);
            int pad = Math.round(15f * context.getResources().getDisplayMetrics().density);
            icon.setPadding(pad, pad, pad, pad);
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                    Math.round(54f * context.getResources().getDisplayMetrics().density),
                    Math.round(54f * context.getResources().getDisplayMetrics().density),
                    android.view.Gravity.CENTER
            );
            icon.setLayoutParams(iconParams);
            icon.setImageResource(glyph);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
        TextView cancel = root.findViewById(R.id.button_confirm_cancel);
        TextView confirm = root.findViewById(R.id.button_confirm_ok);
        confirm.setText(confirmText);
        confirm.setBackgroundResource(tone == Tone.DANGER
                ? R.drawable.bg_modal_danger
                : R.drawable.bg_modal_primary);
        Dialog dialog = new Dialog(context, R.style.ThemeOverlay_Yakyeon_Popup);
        dialog.setContentView(root);
        dialog.setCancelable(true);
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onConfirm();
        });
        if (cancelText == null) {
            cancel.setVisibility(View.GONE);
        } else {
            cancel.setText(cancelText);
            cancel.setOnClickListener(v -> {
                dialog.dismiss();
                callback.onCancel();
            });
            dialog.setOnCancelListener(d -> callback.onCancel());
        }
        dialog.setOnShowListener(shown -> {
            Window window = dialog.getWindow();
            if (window == null) {
                return;
            }
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int width = Math.min(
                    Math.round(304f * metrics.density) + Math.round(16f * metrics.density),
                    metrics.widthPixels - Math.round(16f * metrics.density)
            );
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        });
        dialog.show();
    }
}
