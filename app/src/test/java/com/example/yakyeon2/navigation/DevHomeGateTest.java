package com.example.yakyeon2.navigation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DevHomeGateTest {

    @Test
    public void releaseBuildDoesNotLeaveLogin() {
        AppNavigator navigator = new AppNavigator();
        navigator.showLoginRoot();

        assertFalse(DevHomeGate.open(navigator, false));
        assertEquals(AuthScreen.LOGIN, navigator.currentScreen());
        assertFalse(navigator.isMainArea());
    }

    @Test
    public void debugBuildOpensTheExistingHomeTab() {
        AppNavigator navigator = new AppNavigator();
        navigator.showLoginRoot();

        assertTrue(DevHomeGate.open(navigator, true));
        assertTrue(navigator.isMainArea());
        assertEquals(ScreenId.HOME, navigator.currentTab());
        navigator.selectTab(ScreenId.GRAPH);
        assertEquals(ScreenId.GRAPH, navigator.currentTab());
        navigator.selectTab(ScreenId.PILLBOX);
        navigator.selectTab(ScreenId.SETTINGS);
        assertEquals(ScreenId.SETTINGS, navigator.currentTab());
        assertTrue(navigator.isMainArea());
    }
}
