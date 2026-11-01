package com.example.yakyeon2.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.yakyeon2.R;
import com.google.android.material.color.MaterialColors;

public class YakyeonHeader extends LinearLayout {

    private final ImageButton backButton;
    private final TextView titleView;
    private final TextView actionView;
    private final ImageButton actionIcon;

    public YakyeonHeader(Context context) {
        this(context, null);
    }

    public YakyeonHeader(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        inflate(context, R.layout.view_yakyeon_header, this);
        backButton = findViewById(R.id.header_back);
        titleView = findViewById(R.id.header_title);
        actionView = findViewById(R.id.header_action);
        actionIcon = findViewById(R.id.header_action_icon);
        backButton.setImageTintList(ColorStateList.valueOf(
                MaterialColors.getColor(backButton, com.google.android.material.R.attr.colorOnSurface)
        ));
    }

    public void setTitle(CharSequence title) {
        titleView.setText(title);
    }

    public void setBackVisible(boolean visible) {
        backButton.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setBackPlated(boolean plated) {
        if (plated) {
            backButton.setBackgroundResource(R.drawable.yakyeon_back_plate);
            return;
        }
        TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true);
        backButton.setBackgroundResource(value.resourceId);
    }

    public void setOnBackClick(@Nullable View.OnClickListener listener) {
        backButton.setOnClickListener(listener);
    }

    public void setAction(@Nullable CharSequence text, @Nullable View.OnClickListener listener) {
        actionIcon.setVisibility(GONE);
        actionIcon.setOnClickListener(null);
        if (text == null || text.length() == 0 || listener == null) {
            actionView.setVisibility(GONE);
            actionView.setOnClickListener(null);
            return;
        }
        actionView.setVisibility(VISIBLE);
        actionView.setText(text);
        actionView.setOnClickListener(listener);
    }

    public void setActionIcon(int drawableRes, @Nullable View.OnClickListener listener) {
        actionView.setVisibility(GONE);
        actionView.setOnClickListener(null);
        if (drawableRes == 0 || listener == null) {
            actionIcon.setVisibility(GONE);
            actionIcon.setOnClickListener(null);
            return;
        }
        actionIcon.setVisibility(VISIBLE);
        actionIcon.setImageResource(drawableRes);
        actionIcon.setContentDescription(getContext().getString(R.string.med_history_action));
        actionIcon.setOnClickListener(listener);
    }

    public void setActionIconPlate(boolean plated) {
        if (actionIcon.getVisibility() != VISIBLE) {
            return;
        }
        if (plated) {
            actionIcon.setBackgroundResource(R.drawable.bg_header_icon);
            return;
        }
        TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true);
        actionIcon.setBackgroundResource(value.resourceId);
    }

    public void setForegroundColor(int color) {
        titleView.setTextColor(color);
        backButton.setImageTintList(ColorStateList.valueOf(color));
    }
}
