package com.example.yakyeon2.data;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DrugRelationsTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 8);

    @Test
    public void noMedicinesHaveNoNodesAndNoRelations() {
        MedicationStore store = new MedicationStore(new YakyeonSession());

        assertTrue(DrugRelations.takingNodes(store).isEmpty());
        assertTrue(DrugRelations.confirmedPairs(store).isEmpty());
        assertEquals(0, DrugRelations.confirmedCount(store));
        assertEquals(DrugRelations.confirmedPairs(store).size(), DrugRelations.confirmedCount(store));
    }

    @Test
    public void savedNamesAreNodesWithoutInventedPairs() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        commit(store, "약1");
        commit(store, "약7");

        List<DrugRelations.Node> nodes = DrugRelations.takingNodes(store);
        assertEquals(2, nodes.size());
        assertEquals("약1", nodes.get(0).getName());
        assertEquals("약7", nodes.get(1).getName());
        assertTrue(DrugRelations.confirmedPairs(store).isEmpty());
        assertEquals(0, DrugRelations.confirmedCount(store));
    }

    @Test
    public void stoppedAndUncommittedMedicinesAreNotNodes() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        commit(store, "kept");
        commit(store, "stopped");
        assertTrue(store.stopEnvelope(store.envelopes().get(1).getId(), DAY.atStartOfDay(ZONE).toInstant().toEpochMilli()));

        List<DrugRelations.Node> nodes = DrugRelations.takingNodes(store);
        assertEquals(1, nodes.size());
        assertEquals("kept", nodes.get(0).getName());

        assertTrue(DevFixtures.apply(store, DevFixtures.Scenario.ALARM_DRAFT, ZONE, System.currentTimeMillis()));
        assertEquals(1, DrugRelations.takingNodes(store).size());
        assertEquals("kept", DrugRelations.takingNodes(store).get(0).getName());
    }

    @Test
    public void fixturesAndDevSamplesAreNotTakingNodesOrRelations() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        long now = DAY.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();
        assertTrue(DevFixtures.apply(store, DevFixtures.Scenario.MULTIPLE_ENVELOPES, ZONE, now));
        assertFalse(store.envelopes().isEmpty());
        assertTrue(DrugRelations.takingNodes(store).isEmpty());
        assertEquals(0, DrugRelations.confirmedCount(store));

        MedicationStore samples = new MedicationStore(new YakyeonSession());
        assertTrue(samples.loadDevListSample(ZONE));
        assertTrue(DrugRelations.takingNodes(samples).isEmpty());
        assertEquals(0, DrugRelations.confirmedCount(samples));
    }

    @Test
    public void realMedicineStaysWhenFixturesAreAlsoLoaded() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        commit(store, "registered-name");
        long now = DAY.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();
        assertTrue(DevFixtures.apply(store, DevFixtures.Scenario.MULTIPLE_ENVELOPES, ZONE, now));

        List<DrugRelations.Node> nodes = DrugRelations.takingNodes(store);
        assertEquals(1, nodes.size());
        assertEquals("registered-name", nodes.get(0).getName());
        assertEquals(0, DrugRelations.confirmedCount(store));
    }

    @Test
    public void oneSavedMedicineStillHasANodeWhenThereIsNoRelation() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        commit(store, "only");

        List<DrugRelations.Node> nodes = DrugRelations.takingNodes(store);
        assertEquals(1, nodes.size());
        assertEquals("only", nodes.get(0).getName());
        assertEquals(0, DrugRelations.confirmedCount(store));
        String json = GraphDocument.json(nodes, DrugRelations.confirmedPairs(store));
        assertTrue(json.contains("\"name\":\"only\""));
        assertTrue(json.contains("\"edges\":[]"));
        assertEquals(DrugRelations.confirmedPairs(store).size(), DrugRelations.confirmedCount(store));
    }

    @Test
    public void severalMedicinesStayNodesWhenRelationsAreEmpty() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        commit(store, "alpha");
        commit(store, "beta");
        commit(store, "gamma");

        List<DrugRelations.Node> nodes = DrugRelations.takingNodes(store);
        assertEquals(3, nodes.size());
        assertEquals(0, DrugRelations.confirmedCount(store));
        String json = GraphDocument.json(nodes, DrugRelations.confirmedPairs(store));
        assertTrue(json.contains("\"name\":\"alpha\""));
        assertTrue(json.contains("\"name\":\"beta\""));
        assertTrue(json.contains("\"name\":\"gamma\""));
        assertTrue(json.contains("\"edges\":[]"));
        assertEquals(DrugRelations.confirmedCount(store), DrugRelations.confirmedPairs(store).size());
    }

    @Test
    public void devScenarioLinksOnlyTheDefinedPairAndMatchesTheCount() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        commit(store, "alpha");
        commit(store, "beta");
        commit(store, "gamma");
        store.setDevGraphRelationScenario(true);

        List<DrugRelations.Node> nodes = DrugRelations.takingNodes(store);
        List<DrugRelations.Pair> pairs = DrugRelations.confirmedPairs(store);
        assertEquals(3, nodes.size());
        assertEquals(1, pairs.size());
        assertEquals(1, DrugRelations.confirmedCount(store));
        assertEquals(pairs.size(), DrugRelations.confirmedCount(store));
        assertEquals(nodes.get(0).getMedicineId(), pairs.get(0).getLeftMedicineId());
        assertEquals(nodes.get(1).getMedicineId(), pairs.get(0).getRightMedicineId());
        assertTrue(pairs.get(0).isDevScenario());
        assertFalse(pairs.get(0).getLeftMedicineId().equals(nodes.get(2).getMedicineId()));
        assertFalse(pairs.get(0).getRightMedicineId().equals(nodes.get(2).getMedicineId()));

        String json = GraphDocument.json(nodes, pairs);
        int edges = json.indexOf("\"edges\":");
        String edgeJson = json.substring(edges);
        assertTrue(edgeJson.contains(nodes.get(0).getMedicineId()));
        assertTrue(edgeJson.contains(nodes.get(1).getMedicineId()));
        assertFalse(edgeJson.contains(nodes.get(2).getMedicineId()));
        assertTrue(json.contains("\"related\":true"));
        assertTrue(json.contains("\"related\":false"));
        assertEquals(pairs.get(0), GraphDocument.findPair(pairs, GraphDocument.edgeId(pairs.get(0))));
    }

    @Test
    public void devScenarioDoesNotIncludeSamplesAndIsNotExported() {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        long now = DAY.atTime(10, 0).atZone(ZONE).toInstant().toEpochMilli();
        assertTrue(DevFixtures.apply(store, DevFixtures.Scenario.MULTIPLE_ENVELOPES, ZONE, now));
        store.setDevGraphRelationScenario(true);
        assertTrue(DrugRelations.takingNodes(store).isEmpty());
        assertEquals(0, DrugRelations.confirmedCount(store));

        commit(store, "kept");
        commit(store, "also");
        assertEquals(1, DrugRelations.confirmedCount(store));
        String exported = store.exportState();
        MedicationStore restored = new MedicationStore(new YakyeonSession());
        restored.importState(exported);
        assertFalse(restored.isDevGraphRelationScenario());
        assertEquals(2, DrugRelations.takingNodes(restored).size());
        assertEquals(0, DrugRelations.confirmedCount(restored));
        assertEquals(DrugRelations.confirmedCount(restored), DrugRelations.confirmedPairs(restored).size());
    }

    @Test
    public void quotedMedicineNameStaysInsideTheGraphJson() {
        String json = GraphDocument.json(
                java.util.Collections.singletonList(new DrugRelations.Node("id", "a\"b")),
                java.util.Collections.emptyList()
        );
        assertTrue(json.contains("a\\\"b"));
        assertTrue(json.contains("\"edges\":[]"));
    }

    @Test
    public void oneNodeIsCenteredOnTheCircle() {
        GraphPlacement.Result result = GraphPlacement.place(200, 260, new int[]{40}, new int[]{20}, 12);

        assertTrue(result.isCircled());
        assertEquals(1, result.spots().size());
        assertEquals(80, result.spots().get(0).getLeft());
        assertEquals(120, result.spots().get(0).getTop());
        assertEquals(260, result.getContentHeight());
    }

    @Test
    public void severalNodesStaySeparatedOnTheCircle() {
        int[] widths = new int[]{40, 40, 40};
        int[] heights = new int[]{28, 28, 28};
        GraphPlacement.Result result = GraphPlacement.place(360, 260, widths, heights, 12);

        assertTrue(result.isCircled());
        assertEquals(3, result.spots().size());
        assertTrue(separated(result, widths, heights, 12));
        assertInside(result, 360, widths, heights);
    }

    @Test
    public void wideNodesWrapIntoRowsInsteadOfOverlapping() {
        int[] widths = new int[]{100, 100, 100, 100, 100, 100, 100, 100};
        int[] heights = new int[]{30, 30, 30, 30, 30, 30, 30, 30};
        GraphPlacement.Result result = GraphPlacement.place(200, 260, widths, heights, 12);

        assertFalse(result.isCircled());
        assertEquals(324, result.getContentHeight());
        assertTrue(separated(result, widths, heights, 12));
        assertEquals(0, result.spots().get(1).getLeft());
        assertEquals(42, result.spots().get(1).getTop());
    }

    private static boolean separated(GraphPlacement.Result result, int[] widths, int[] heights, int gap) {
        List<GraphPlacement.Spot> spots = result.spots();
        for (int left = 0; left < spots.size(); left++) {
            for (int right = left + 1; right < spots.size(); right++) {
                GraphPlacement.Spot a = spots.get(left);
                GraphPlacement.Spot b = spots.get(right);
                boolean closeX = a.getLeft() < b.getLeft() + widths[right] + gap
                        && b.getLeft() < a.getLeft() + widths[left] + gap;
                boolean closeY = a.getTop() < b.getTop() + heights[right] + gap
                        && b.getTop() < a.getTop() + heights[left] + gap;
                if (closeX && closeY) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void assertInside(GraphPlacement.Result result, int width, int[] widths, int[] heights) {
        List<GraphPlacement.Spot> spots = result.spots();
        for (int index = 0; index < spots.size(); index++) {
            GraphPlacement.Spot spot = spots.get(index);
            assertTrue(spot.getLeft() >= 0);
            assertTrue(spot.getTop() >= 0);
            assertTrue(spot.getLeft() + widths[index] <= width);
            assertTrue(spot.getTop() + heights[index] <= result.getContentHeight());
        }
    }

    private static void commit(MedicationStore store, String name) {
        EnvelopeDraft draft = store.beginDraft();
        draft.setHospitalName("hospital");
        MedicineDraft medicine = store.addMedicine(DAY);
        medicine.setName(name);
        medicine.setAmount(DoseAmount.TABLET_1);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.rounds().add(DoseRound.LUNCH);
        medicine.rounds().add(DoseRound.EVENING);
        medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        medicine.setEnd(DAY);
        medicine.setConfirmed(true);
        draft.alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        draft.alarmMinutes().put(DoseRound.LUNCH, 13 * 60);
        draft.alarmMinutes().put(DoseRound.EVENING, 18 * 60);
        assertEquals(
                MedicationRules.SaveBlock.OK,
                store.commit(ZONE, DAY.atStartOfDay(ZONE).toInstant().toEpochMilli())
        );
    }
}
