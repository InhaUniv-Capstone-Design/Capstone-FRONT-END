package com.example.yakyeon.ui.common;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.yakyeon.MainActivity;
import com.example.yakyeon.R;

public final class BottomNav {

    private BottomNav() {
    }

    public static void bind(AppCompatActivity activity) {
        bind(activity, false);
    }

    public static void bind(AppCompatActivity activity, boolean graphSelected) {
        activity.findViewById(R.id.nav_home).setOnClickListener(view -> {
            if (!(activity instanceof MainActivity)) {
                AppNavigator.showHome(activity);
            }
        });
        activity.findViewById(R.id.nav_graph).setOnClickListener(view -> {
            if (!graphSelected) {
                AppNavigator.openGraph(activity);
            }
        });
        if (graphSelected) {
            showGraphSelected(activity);
        }
        activity.findViewById(R.id.nav_pillbox).setOnClickListener(
                view -> AppNavigator.openPillbox(activity)
        );
        activity.findViewById(R.id.nav_settings).setOnClickListener(
                view -> AppNavigator.openSettings(activity)
        );
    }

    private static void showGraphSelected(AppCompatActivity activity) {
        int active = ContextCompat.getColor(activity, R.color.nav_active);
        int inactive = ContextCompat.getColor(activity, R.color.nav_inactive);
        ImageView homeIcon = activity.findViewById(R.id.nav_home_icon);
        ImageView graphIcon = activity.findViewById(R.id.nav_graph_icon);
        TextView homeLabel = activity.findViewById(R.id.nav_home_label);
        TextView graphLabel = activity.findViewById(R.id.nav_graph_label);
        homeIcon.setImageTintList(ColorStateList.valueOf(inactive));
        graphIcon.setImageTintList(ColorStateList.valueOf(active));
        homeLabel.setTextColor(inactive);
        homeLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        graphLabel.setTextColor(active);
        graphLabel.setTypeface(Typeface.DEFAULT_BOLD);
    }
}
