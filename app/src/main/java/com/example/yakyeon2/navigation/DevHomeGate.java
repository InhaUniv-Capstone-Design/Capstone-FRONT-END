package com.example.yakyeon2.navigation;

public final class DevHomeGate {

    private DevHomeGate() {
    }

    public static boolean open(AppNavigator navigator, boolean debuggable) {
        if (!debuggable || navigator == null) {
            return false;
        }
        navigator.selectTab(ScreenId.HOME);
        return navigator.isMainArea() && navigator.currentTab() == ScreenId.HOME;
    }
}
