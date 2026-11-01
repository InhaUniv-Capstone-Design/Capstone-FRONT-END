package com.example.yakyeon2.navigation;

import com.example.yakyeon2.data.DevFixtures;
import com.example.yakyeon2.data.EnvelopeDraft;
import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.ScenarioPrescriptions;
import com.example.yakyeon2.data.YakyeonSession;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DevPillboxScenarioGateTest {

    @Test
    public void releaseBuildStaysOnLoginWithoutSavingOrConnecting() {
        AppNavigator navigator = new AppNavigator();
        navigator.showLoginRoot();
        MedicationStore store = new MedicationStore(new YakyeonSession());

        assertFalse(DevPillboxScenarioGate.open(navigator, store, false));
        assertEquals(AuthScreen.LOGIN, navigator.currentScreen());
        assertFalse(navigator.isMainArea());
        assertFalse(store.pillboxConnected());
        assertNull(ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC));
    }

    @Test
    public void debugBuildSavesTheClinicEnvelopeConnectsThePillboxAndOpensHome() {
        AppNavigator navigator = new AppNavigator();
        navigator.showLoginRoot();
        MedicationStore store = new MedicationStore(new YakyeonSession());

        assertTrue(DevPillboxScenarioGate.open(navigator, store, true));
        assertTrue(navigator.isMainArea());
        assertEquals(ScreenId.HOME, navigator.currentTab());
        assertTrue(store.pillboxConnected());
        assertEquals(3, store.activeMedicineCount());
        assertEquals(
                Integer.valueOf(ScenarioPrescriptions.CLINIC_SLOT),
                ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC).getSlotIndex()
        );
        assertTrue(store.sessionSlotOccupied(ScenarioPrescriptions.CLINIC_SLOT));
        assertNull(ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.INTERNAL));
        assertTrue(store.exportState().contains(ScenarioPrescriptions.CLINIC));
        assertFalse(store.exportState().contains("fixture-"));

        int medicines = store.activeMedicineCount();
        assertTrue(DevPillboxScenarioGate.open(navigator, store, true));
        assertEquals(medicines, store.activeMedicineCount());
        assertTrue(store.pillboxConnected());

        DevFixtures.reset(store);
        assertTrue(store.pillboxConnected());
        assertEquals(medicines, store.activeMedicineCount());
    }

    @Test
    public void draftInProgressDoesNotConnectOrLeaveLogin() {
        AppNavigator navigator = new AppNavigator();
        navigator.showLoginRoot();
        MedicationStore store = new MedicationStore(new YakyeonSession());
        EnvelopeDraft draft = store.beginDraft();
        draft.setHospitalName("작성중");

        assertFalse(DevPillboxScenarioGate.open(navigator, store, true));
        assertEquals(AuthScreen.LOGIN, navigator.currentScreen());
        assertFalse(store.pillboxConnected());
        assertNull(ScenarioPrescriptions.envelope(store, ScenarioPrescriptions.CLINIC));
    }
}
