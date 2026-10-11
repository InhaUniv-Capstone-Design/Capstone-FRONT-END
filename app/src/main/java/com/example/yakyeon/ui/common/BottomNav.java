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

    private enum Selection {
        HOME,
        GRAPH,
        PILLBOX,
        NONE
    }

    public static void bind(AppCompatActivity activity) {
        bind(activity, false);
    }

    public static void bind(AppCompatActivity activity, boolean graphSelected) {
        attach(activity, graphSelected ? Selection.GRAPH : Selection.HOME);
    }

    public static void bindPillbox(AppCompatActivity activity) {
        attach(activity, Selection.PILLBOX);
    }

    public static void bindNeutral(AppCompatActivity activity) {
        attach(activity, Selection.NONE);
    }

    private static void attach(AppCompatActivity activity, Selection selection) {
        activity.findViewById(R.id.nav_home).setOnClickListener(view -> {
            if (!(activity instanceof MainActivity)) {
                AppNavigator.showHome(activity);
            }
        });
        activity.findViewById(R.id.nav_graph).setOnClickListener(view -> {
            if (selection != Selection.GRAPH) {
                AppNavigator.openGraph(activity);
            }
        });
        activity.findViewById(R.id.nav_pillbox).setOnClickListener(
                view -> AppNavigator.openPillbox(activity)
        );
        activity.findViewById(R.id.nav_settings).setOnClickListener(
                view -> AppNavigator.openSettings(activity)
        );
        if (selection == Selection.GRAPH) {
            showGraphSelected(activity);
        } else if (selection == Selection.PILLBOX) {
            showPillboxSelected(activity);
        } else if (selection == Selection.NONE) {
            showNoneSelected(activity);
        }
    }

    private static void showNoneSelected(AppCompatActivity activity) {
        int inactive = ContextCompat.getColor(activity, R.color.nav_inactive);
        ImageView homeIcon = activity.findViewById(R.id.nav_home_icon);
        TextView homeLabel = activity.findViewById(R.id.nav_home_label);
        homeIcon.setImageTintList(ColorStateList.valueOf(inactive));
        homeLabel.setTextColor(inactive);
        homeLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
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

    private static void showPillboxSelected(AppCompatActivity activity) {
        int active = ContextCompat.getColor(activity, R.color.nav_active);
        int inactive = ContextCompat.getColor(activity, R.color.nav_inactive);
        ImageView homeIcon = activity.findViewById(R.id.nav_home_icon);
        ImageView pillboxIcon = activity.findViewById(R.id.nav_pillbox_icon);
        TextView homeLabel = activity.findViewById(R.id.nav_home_label);
        TextView pillboxLabel = activity.findViewById(R.id.nav_pillbox_label);
        homeIcon.setImageTintList(ColorStateList.valueOf(inactive));
        pillboxIcon.setImageTintList(ColorStateList.valueOf(active));
        homeLabel.setTextColor(inactive);
        homeLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        pillboxLabel.setTextColor(active);
        pillboxLabel.setTypeface(Typeface.DEFAULT_BOLD);
    }
}
