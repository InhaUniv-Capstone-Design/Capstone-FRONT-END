package com.example.yakyeon2.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.yakyeon2.R;

public class StatePanel extends LinearLayout {

    public enum Mode {
        LOADING,
        EMPTY,
        ERROR
    }

    private final ProgressBar progress;
    private final TextView titleView;
    private final TextView messageView;

    public StatePanel(Context context) {
        this(context, null);
    }

    public StatePanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        int padding = getResources().getDimensionPixelSize(R.dimen.space_l);
        setPadding(padding, padding, padding, padding);
        inflate(context, R.layout.view_state_panel, this);
        progress = findViewById(R.id.state_progress);
        titleView = findViewById(R.id.state_title);
        messageView = findViewById(R.id.state_message);
    }

    public void show(Mode mode, CharSequence title, CharSequence message) {
        progress.setVisibility(mode == Mode.LOADING ? VISIBLE : GONE);
        titleView.setText(title);
        messageView.setText(message);
    }
}
