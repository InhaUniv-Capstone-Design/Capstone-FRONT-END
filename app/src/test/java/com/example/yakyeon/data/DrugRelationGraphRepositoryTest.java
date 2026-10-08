package com.example.yakyeon.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.yakyeon.data.mock.MockDurInteractionRepository;
import com.example.yakyeon.data.mock.MockMedicationRepository;
import com.example.yakyeon.data.model.DrugGraphEdge;
import com.example.yakyeon.data.model.DrugRelationGraph;
import com.example.yakyeon.data.model.DurInteraction;
import com.example.yakyeon.data.model.Medication;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DrugRelationGraphRepositoryTest {

    @Test
    public void registeredDrugsBecomeNodesAndOnlyContraindicatedPairsBecomeEdges() {
        DrugRelationGraph graph = load(
                medications("a", "약물 A", "b", "약물 B", "c", "약물 C", "d", "약물 D"),
                interactions(
                        interaction("ab", "약물 A", "약물 B"),
                        interaction("bd", "약물 B", "약물 D")
                )
        );

        assertEquals(Arrays.asList("a", "b", "c", "d"), ids(graph));
        assertEquals(2, graph.getEdges().size());
        assertEdge(graph, "ab", "a", "b");
        assertEdge(graph, "bd", "b", "d");
    }

    @Test
    public void drugsWithoutAContraindicationStayAsNodesWithoutEdges() {
        DrugRelationGraph graph = load(
                medications("a", "약물 A", "b", "약물 B"),
                interactions(interaction("ax", "약물 A", "미등록"))
        );

        assertEquals(Arrays.asList("a", "b"), ids(graph));
        assertTrue(graph.getEdges().isEmpty());
    }

    @Test
    public void interactionWithAnUnregisteredDrugIsNotDrawn() {
        DrugRelationGraph graph = load(
                medications("a", "약물 A", "b", "약물 B", "c", "약물 C"),
                interactions(
                        interaction("ab", "약물 A", "약물 B"),
                        interaction("bd", "약물 B", "약물 D")
                )
        );

        assertEquals(Arrays.asList("a", "b", "c"), ids(graph));
        assertEquals(1, graph.getEdges().size());
        assertEdge(graph, "ab", "a", "b");
        assertNull(edge(graph, "bd"));
    }

    @Test
    public void nameMatchingIgnoresSurroundingWhitespace() {
        DrugRelationGraph graph = load(
                medications("a", "  약물 A  ", "b", "약물   B"),
                interactions(interaction("ab", "약물 A", "약물 B"))
        );

        assertEdge(graph, "ab", "a", "b");
    }

    @Test
    public void findInteractionReusesDurRepository() {
        DurInteraction interaction = interaction("ab", "약물 A", "약물 B");
        DrugRelationGraphRepository repository = new DrugRelationGraphRepository(
                medications("a", "약물 A"),
                interactions(interaction)
        );

        assertEquals("ab", repository.findInteraction("ab").getId());
        assertEquals(interaction.getInteractionDescription(), repository.findInteraction("ab").getInteractionDescription());
        assertNull(repository.findInteraction("missing"));
    }

    @Test
    public void currentMockShowsEveryRegisteredDrugAndOnlyPairsBothRegistered() {
        DrugRelationGraph graph = new DrugRelationGraphRepository(
                new MockMedicationRepository(),
                new MockDurInteractionRepository()
        ).load();

        assertEquals(Arrays.asList("metformin", "atorvastatin", "amlodipine", "omeprazole"), ids(graph));
        assertEquals(1, graph.getEdges().size());
        assertEdge(graph, "dur-002", "metformin", "omeprazole");
        assertNull(edge(graph, "dur-001"));
        assertNull(edge(graph, "dur-003"));
        assertNotNull(new DrugRelationGraphRepository(
                new MockMedicationRepository(),
                new MockDurInteractionRepository()
        ).findInteraction("dur-002"));
    }

    private static DrugRelationGraph load(MedicationRepository medications, DurInteractionRepository interactions) {
        return new DrugRelationGraphRepository(medications, interactions).load();
    }

    private static void assertEdge(DrugRelationGraph graph, String interactionId, String sourceId, String targetId) {
        DrugGraphEdge found = edge(graph, interactionId);
        assertNotNull(found);
        assertEquals(sourceId, found.getSourceMedicationId());
        assertEquals(targetId, found.getTargetMedicationId());
    }

    private static DrugGraphEdge edge(DrugRelationGraph graph, String interactionId) {
        for (DrugGraphEdge candidate : graph.getEdges()) {
            if (interactionId.equals(candidate.getInteractionId())) {
                return candidate;
            }
        }
        return null;
    }

    private static List<String> ids(DrugRelationGraph graph) {
        List<String> ids = new ArrayList<>();
        for (Medication medication : graph.getMedications()) {
            ids.add(medication.getId());
        }
        return ids;
    }

    private static MedicationRepository medications(String... idAndName) {
        List<Medication> medications = new ArrayList<>();
        for (int index = 0; index < idAndName.length; index += 2) {
            medications.add(medication(idAndName[index], idAndName[index + 1]));
        }
        return new MedicationRepository() {
            @Override
            public List<Medication> getMedications() {
                return medications;
            }

            @Override
            public List<Medication> getActiveMedications() {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Medication> getEndedMedications() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int countActive() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Medication findById(String id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean updateMedication(
                    String id,
                    String name,
                    double doseAmount,
                    String doseUnit,
                    String timesPerDay,
                    String doseTimes,
                    String doseTiming,
                    String duration
            ) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean deleteMedication(String id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean endMedication(String id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void updateExpiredMedications() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String addMedication(
                    String name,
                    String dosage,
                    double doseAmount,
                    String doseUnit,
                    String timesPerDay,
                    String doseTimes,
                    String doseTiming,
                    String duration
            ) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static DurInteractionRepository interactions(DurInteraction... interactions) {
        List<DurInteraction> items = Arrays.asList(interactions);
        return new DurInteractionRepository() {
            @Override
            public List<DurInteraction> getInteractions() {
                return items;
            }

            @Override
            public DurInteraction findById(String id) {
                for (DurInteraction interaction : items) {
                    if (interaction.getId().equals(id)) {
                        return interaction;
                    }
                }
                return null;
            }
        };
    }

    private static Medication medication(String id, String name) {
        return new Medication(
                id,
                name,
                "",
                "",
                Medication.Classification.ACTIVE,
                Medication.Form.TABLET,
                "",
                "",
                "",
                "",
                "",
                1,
                "정",
                "",
                "",
                "",
                ""
        );
    }

    private static DurInteraction interaction(String id, String drugA, String drugB) {
        return new DurInteraction(
                id,
                drugA,
                drugB,
                "분류 A",
                "분류 B",
                "병용금기 등재 안내",
                "사유",
                id + " 상세",
                "주의",
                "주의"
        );
    }
}
