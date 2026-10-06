package com.example.yakyeon.ui.common;

import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.MainActivity;
import com.example.yakyeon.R;

public final class BottomNav {

    private BottomNav() {
    }

    public static void bind(AppCompatActivity activity) {
        activity.findViewById(R.id.nav_home).setOnClickListener(view -> {
            if (!(activity instanceof MainActivity)) {
                AppNavigator.showHome(activity);
            }
        });
        activity.findViewById(R.id.nav_graph).setOnClickListener(
                view -> AppNavigator.openGraph(activity)
        );
        activity.findViewById(R.id.nav_pillbox).setOnClickListener(
                view -> AppNavigator.openPillbox(activity)
        );
        activity.findViewById(R.id.nav_settings).setOnClickListener(
                view -> AppNavigator.openSettings(activity)
        );
    }
}
