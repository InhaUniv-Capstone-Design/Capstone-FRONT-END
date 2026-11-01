package com.example.yakyeon2.navigation;

public enum ScreenId {
    HOME(true),
    GRAPH(true),
    PILLBOX(true),
    SETTINGS(true);

    private final boolean mainTab;

    ScreenId(boolean mainTab) {
        this.mainTab = mainTab;
    }

    public boolean isMainTab() {
        return mainTab;
    }
}
