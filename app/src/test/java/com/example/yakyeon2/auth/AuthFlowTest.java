package com.example.yakyeon2.auth;

import com.example.yakyeon2.data.AccountType;
import com.example.yakyeon2.data.InMemoryYakyeonRepository;
import com.example.yakyeon2.data.SignupDraft;
import com.example.yakyeon2.data.YakyeonSession;
import com.example.yakyeon2.navigation.AppNavigator;
import com.example.yakyeon2.navigation.AuthScreen;
import com.example.yakyeon2.navigation.ScreenId;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AuthFlowTest {

    @Test
    public void appStartsOnLoginWithoutMainTabs() {
        AppNavigator navigator = new AppNavigator();
        AuthFlow flow = new AuthFlow(navigator, new SignupDraft());

        flow.start();

        assertEquals(AuthScreen.LOGIN, navigator.currentScreen());
        assertFalse(navigator.isMainArea());
        assertFalse(flow.back());
    }

    @Test
    public void signupRequiresEveryConsentBeforeTypeSelection() {
        AuthFlow flow = new AuthFlow(new AppNavigator(), new SignupDraft());
        flow.start();

        assertFalse(flow.openType(new SignupConsent(true, true, true, false)));
        assertEquals(AuthScreen.LOGIN, flow.screen());
        assertTrue(flow.openType(new SignupConsent(true, true, true, true)));
    }

    @Test
    public void missingTypeStaysOnTypeScreen() {
        AuthFlow flow = startedTypeFlow();

        assertEquals(AuthFlow.TypeStep.NEED_TYPE, flow.next(null));
        assertEquals(AuthScreen.SIGNUP_TYPE, flow.screen());
    }

    @Test
    public void selfGoesToFormAndBackReturnsToTypeThenLogin() {
        SignupDraft draft = new SignupDraft();
        AuthFlow flow = startedTypeFlow(draft);

        assertEquals(AuthFlow.TypeStep.FORM, flow.next(AccountType.SELF));
        assertEquals(AccountType.SELF, draft.getAccountType());
        assertTrue(flow.back());
        assertEquals(AuthScreen.SIGNUP_TYPE, flow.screen());
        assertTrue(flow.back());
        assertEquals(AuthScreen.LOGIN, flow.screen());
        assertFalse(flow.back());
    }

    @Test
    public void caregiverNoticeDoesNotOpenTheFormUntilContinue() {
        AuthFlow flow = startedTypeFlow();

        assertEquals(AuthFlow.TypeStep.CAREGIVER_NOTICE, flow.next(AccountType.CAREGIVER));
        assertEquals(AuthScreen.SIGNUP_TYPE, flow.screen());

        flow.continueAfterCaregiverNotice();

        assertEquals(AuthScreen.SIGNUP_FORM, flow.screen());
        assertTrue(flow.back());
        assertEquals(AuthScreen.SIGNUP_TYPE, flow.screen());
    }

    @Test
    public void unfinishedSignupDoesNotBecomeALoggedInSession() {
        InMemoryYakyeonRepository repository = new InMemoryYakyeonRepository();
        YakyeonSession session = repository.session();
        AuthFlow flow = new AuthFlow(new AppNavigator(), repository.signupDraft());
        flow.start();
        flow.openType(new SignupConsent(true, true, true, true));
        flow.next(AccountType.CAREGIVER);

        assertEquals(AccountType.CAREGIVER, repository.signupDraft().getAccountType());
        assertNull(session.getAccountType());
        assertFalse(session.isPillboxConnected());

        flow.returnToLogin();

        assertNull(repository.signupDraft().getAccountType());
        assertNull(session.getAccountType());
        assertEquals(AuthScreen.LOGIN, flow.screen());
    }

    @Test
    public void leavingAuthStillReachesTheMainTabs() {
        AppNavigator navigator = new AppNavigator();
        new AuthFlow(navigator, new SignupDraft()).start();

        navigator.selectTab(ScreenId.GRAPH);

        assertTrue(navigator.isMainArea());
        assertEquals(ScreenId.GRAPH, navigator.currentTab());
    }

    private static AuthFlow startedTypeFlow() {
        return startedTypeFlow(new SignupDraft());
    }

    private static AuthFlow startedTypeFlow(SignupDraft draft) {
        AppNavigator navigator = new AppNavigator();
        AuthFlow flow = new AuthFlow(navigator, draft);
        flow.start();
        assertTrue(flow.openType(new SignupConsent(true, true, true, true)));
        return flow;
    }
}
