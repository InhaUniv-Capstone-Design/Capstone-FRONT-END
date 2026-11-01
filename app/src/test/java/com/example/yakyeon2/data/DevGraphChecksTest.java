package com.example.yakyeon2.data;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DevGraphChecksTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 8);
    private static final long NOW = DAY.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();

    @Test
    public void emptyCheckLeavesNoNodesAndKeepsAUserMedicine() {
        MedicationStore store = userStore();
        String userId = store.envelopes().get(0).getId();
        store.setDevGraphRelationScenario(true);

        assertTrue(store.installGraphChecks(0, ZONE, NOW));

        assertEquals(1, DrugRelations.takingNodes(store).size());
        assertEquals("kept", DrugRelations.takingNodes(store).get(0).getName());
        assertEquals(userId, store.envelopes().get(0).getId());
        assertFalse(store.isDevGraphRelationScenario());
        assertEquals(0, DrugRelations.confirmedCount(store));
    }

    @Test
    public void oneCheckIsANodeWithoutAnEdgeAndSurvivesReloadWithoutTheFlag() {
        MedicationStore store = new MedicationStore(new YakyeonSession());

        assertTrue(store.installGraphChecks(1, ZONE, NOW));

        List<DrugRelations.Node> nodes = DrugRelations.takingNodes(store);
        assertEquals(1, nodes.size());
        assertEquals("그래프 검증 1", nodes.get(0).getName());
        assertEquals(0, DrugRelations.confirmedCount(store));
        String json = GraphDocument.json(nodes, DrugRelations.confirmedPairs(store));
        assertTrue(json.contains("\"name\":\"그래프 검증 1\""));
        assertTrue(json.contains("\"edges\":[]"));
        assertTrue(store.exportState().contains("그래프 검증 1"));
        assertFalse(store.exportState().contains("fixture-"));

        MedicationStore restored = new MedicationStore(new YakyeonSession());
        restored.importState(store.exportState());
        assertEquals(1, DrugRelations.takingNodes(restored).size());
        assertFalse(restored.isDevGraphRelationScenario());
        assertEquals(0, DrugRelations.confirmedCount(restored));
    }

    @Test
    public void threeChecksStayNodesUntilTheDevRelationIsTurnedOn() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        assertTrue(store.installGraphChecks(3, ZONE, NOW));

        List<DrugRelations.Node> nodes = DrugRelations.takingNodes(store);
        assertEquals(3, nodes.size());
        assertEquals(0, DrugRelations.confirmedPairs(store).size());
        String quiet = GraphDocument.json(nodes, DrugRelations.confirmedPairs(store));
        assertTrue(quiet.contains("\"edges\":[]"));

        store.setDevGraphRelationScenario(true);
        List<DrugRelations.Pair> pairs = DrugRelations.confirmedPairs(store);
        assertEquals(3, DrugRelations.takingNodes(store).size());
        assertEquals(1, pairs.size());
        assertTrue(pairs.get(0).isDevScenario());
        assertEquals("그래프 검증 1", pairs.get(0).getLeftName());
        assertEquals("그래프 검증 2", pairs.get(0).getRightName());
        String linked = GraphDocument.json(DrugRelations.takingNodes(store), pairs);
        assertTrue(linked.contains("\"edges\":[{"));

        store.setDevGraphRelationScenario(false);
        assertEquals(3, DrugRelations.takingNodes(store).size());
        assertEquals(0, DrugRelations.confirmedPairs(store).size());

        MedicationStore restored = new MedicationStore(new YakyeonSession());
        restored.importState(store.exportState());
        assertEquals(3, DrugRelations.takingNodes(restored).size());
        assertEquals(0, DrugRelations.confirmedCount(restored));
        assertFalse(restored.exportState().contains("devGraph"));
    }

    @Test
    public void fixtureEnvelopesStayOutOfTheCheckGraph() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        assertTrue(DevFixtures.apply(store, DevFixtures.Scenario.MULTIPLE_ENVELOPES, ZONE, NOW));
        assertTrue(store.installGraphChecks(3, ZONE, NOW));

        List<DrugRelations.Node> nodes = DrugRelations.takingNodes(store);
        assertEquals(3, nodes.size());
        for (DrugRelations.Node node : nodes) {
            assertFalse(node.getName().contains("개발용 예시"));
        }
        assertTrue(store.hasFixture());
        assertFalse(store.exportState().contains("fixture-"));
        assertTrue(store.exportState().contains("그래프 검증 3"));
    }

    @Test
    public void replacingChecksKeepsTheRegistrationDraftAndTheUserEnvelope() {
        MedicationStore store = userStore();
        store.beginDraft();
        store.draft().setHospitalName("작성 중");
        MedicineDraft draft = store.addMedicine(DAY);
        draft.setName("아직 등록 전");

        assertTrue(store.installGraphChecks(1, ZONE, NOW));

        assertEquals("작성 중", store.draft().getHospitalName());
        assertEquals("아직 등록 전", store.draft().medicines().get(0).getName());
        assertEquals(2, DrugRelations.takingNodes(store).size());
        store.clearGraphChecks();
        assertEquals(1, store.envelopes().size());
        assertEquals("kept", store.envelopes().get(0).medicines().get(0).getName());
        assertEquals("아직 등록 전", store.draft().medicines().get(0).getName());
    }

    private static MedicationStore userStore() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        EnvelopeDraft draft = store.beginDraft();
        draft.setHospitalName("user");
        MedicineDraft medicine = store.addMedicine(DAY);
        medicine.setName("kept");
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.rounds().add(DoseRound.LUNCH);
        medicine.rounds().add(DoseRound.EVENING);
        medicine.setEnd(DAY);
        medicine.setConfirmed(true);
        draft.alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        draft.alarmMinutes().put(DoseRound.LUNCH, 13 * 60);
        draft.alarmMinutes().put(DoseRound.EVENING, 18 * 60);
        assertEquals(MedicationRules.SaveBlock.OK, store.commit(ZONE, NOW));
        return store;
    }
}
