package com.example.yakyeon2.navigation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppNavigatorTest {

    @Test
    public void startsOnHomeWithoutNonMainScreen() {
        AppNavigator navigator = new AppNavigator();

        assertEquals(ScreenId.HOME, navigator.currentTab());
        assertTrue(navigator.isMainArea());
    }

    @Test
    public void selectingAnotherTabStaysInMainArea() {
        AppNavigator navigator = new AppNavigator();

        navigator.selectTab(ScreenId.PILLBOX);

        assertEquals(ScreenId.PILLBOX, navigator.currentTab());
        assertTrue(navigator.isMainArea());
        assertFalse(navigator.pop());
    }

    @Test
    public void nonMainScreenHidesTabsUntilBack() {
        AppNavigator navigator = new AppNavigator();
        navigator.selectTab(ScreenId.SETTINGS);

        navigator.openNonMain("이용약관");

        assertFalse(navigator.isMainArea());
        assertEquals("이용약관", navigator.nonMainTitle());
        assertTrue(navigator.pop());
        assertTrue(navigator.isMainArea());
        assertEquals(ScreenId.SETTINGS, navigator.currentTab());
    }

    @Test
    public void popToKeepsTheRequestedScreen() {
        AppNavigator navigator = new AppNavigator();
        navigator.openNonMain("med-list");
        navigator.openNonMain("med-consent");
        navigator.openNonMain("med-alarm");

        assertTrue(navigator.popTo("med-list"));
        assertEquals("med-list", navigator.currentScreen());
        assertFalse(navigator.isMainArea());
    }

    @Test
    public void selectingTabClearsNonMainStack() {
        AppNavigator navigator = new AppNavigator();
        navigator.openNonMain("동의");

        navigator.selectTab(ScreenId.GRAPH);

        assertTrue(navigator.isMainArea());
        assertEquals(ScreenId.GRAPH, navigator.currentTab());
    }
}
