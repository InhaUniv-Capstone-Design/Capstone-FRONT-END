package com.example.yakyeon2.navigation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class AppNavigator {

    private ScreenId currentTab = ScreenId.HOME;
    private final ArrayDeque<String> nonMainTitles = new ArrayDeque<>();
    private String authRootKey;

    public ScreenId currentTab() {
        return currentTab;
    }

    public boolean isMainArea() {
        return authRootKey == null && nonMainTitles.isEmpty();
    }

    public boolean isInAuthFlow() {
        return authRootKey != null;
    }

    public void showLoginRoot() {
        nonMainTitles.clear();
        authRootKey = AuthScreen.LOGIN;
    }

    public String currentScreen() {
        if (!nonMainTitles.isEmpty()) {
            return nonMainTitles.peek();
        }
        return authRootKey;
    }

    public String nonMainTitle() {
        return nonMainTitles.peek();
    }

    public void selectTab(ScreenId tab) {
        if (tab == null || !tab.isMainTab()) {
            throw new IllegalArgumentException("tab must be a main tab");
        }
        authRootKey = null;
        nonMainTitles.clear();
        currentTab = tab;
    }

    public void openNonMain(String title) {
        if (title == null || title.length() == 0) {
            throw new IllegalArgumentException("title required");
        }
        nonMainTitles.push(title);
    }

    public boolean pop() {
        if (nonMainTitles.isEmpty()) {
            return false;
        }
        nonMainTitles.pop();
        return true;
    }

    public boolean popTo(String title) {
        if (title == null || title.length() == 0) {
            return false;
        }
        boolean found = false;
        for (String item : nonMainTitles) {
            if (title.equals(item)) {
                found = true;
                break;
            }
        }
        if (!found) {
            return false;
        }
        while (!title.equals(nonMainTitles.peek())) {
            nonMainTitles.pop();
        }
        return true;
    }

    public void openOrPopTo(String title) {
        if (popTo(title)) {
            return;
        }
        nonMainTitles.clear();
        openNonMain(title);
    }

    public List<String> nonMainStack() {
        return new ArrayList<>(nonMainTitles);
    }
}
