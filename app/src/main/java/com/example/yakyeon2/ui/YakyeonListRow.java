package com.example.yakyeon2.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.yakyeon2.R;

public class YakyeonListRow extends LinearLayout {

    private final TextView titleView;
    private final TextView detailView;
    private final TextView trailingView;

    public YakyeonListRow(Context context) {
        this(context, null);
    }

    public YakyeonListRow(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        inflate(context, R.layout.view_list_row, this);
        titleView = findViewById(R.id.row_title);
        detailView = findViewById(R.id.row_detail);
        trailingView = findViewById(R.id.row_trailing);
    }

    public void setTitle(CharSequence title) {
        titleView.setText(title);
    }

    public void setDetail(@Nullable CharSequence detail) {
        if (detail == null || detail.length() == 0) {
            detailView.setVisibility(GONE);
            return;
        }
        detailView.setVisibility(VISIBLE);
        detailView.setText(detail);
    }

    public void setHighlighted(boolean highlighted) {
        setBackgroundColor(highlighted ? getContext().getColor(R.color.yakyeon_mint) : android.graphics.Color.TRANSPARENT);
    }

    public void setTrailing(@Nullable CharSequence trailing) {
        if (trailing == null || trailing.length() == 0) {
            trailingView.setVisibility(GONE);
            return;
        }
        trailingView.setVisibility(VISIBLE);
        trailingView.setText(trailing);
    }
}
