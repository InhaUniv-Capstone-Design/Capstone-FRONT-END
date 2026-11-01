package com.example.yakyeon2.navigation;

public final class RegistrationChrome {

    private RegistrationChrome() {
    }

    public static boolean showsBottomNav(boolean mainArea, String screen) {
        return mainArea
                || MedScreen.LIST.equals(screen)
                || MedScreen.CONSENT.equals(screen)
                || MedScreen.ENVELOPE.equals(screen)
                || MedScreen.SLOT.equals(screen)
                || MedScreen.SCHEDULE.equals(screen)
                || MedScreen.HISTORY.equals(screen);
    }

    public static boolean usesCommentAction(String screen) {
        return MedScreen.LIST.equals(screen)
                || MedScreen.CONSENT.equals(screen)
                || MedScreen.ENVELOPE.equals(screen)
                || MedScreen.SCHEDULE.equals(screen)
                || MedScreen.HISTORY.equals(screen);
    }
}
