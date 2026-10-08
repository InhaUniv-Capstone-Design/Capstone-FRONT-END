package com.example.yakyeon.ui.graph;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.yakyeon.data.DrugRelationGraphRepository;
import com.example.yakyeon.data.DurInteractionRepository;
import com.example.yakyeon.data.MedicationRepository;
import com.example.yakyeon.data.model.DrugRelationGraph;
import com.example.yakyeon.data.model.DurInteraction;
import com.example.yakyeon.data.model.Medication;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class GraphCytoscapePayloadTest {

    @Test
    public void payloadKeepsIsolatedNodesAndOmitsUnregisteredEdges() {
        MedicationRepository medications = new MedicationRepository() {
            @Override
            public List<Medication> getMedications() {
                return Collections.singletonList(new Medication(
                        "a",
                        "약물 \"A\"",
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
                ));
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
            public boolean updateMedication(String id, String name, double doseAmount, String doseUnit, String timesPerDay, String doseTimes, String doseTiming, String duration) {
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
            public String addMedication(String name, String dosage, double doseAmount, String doseUnit, String timesPerDay, String doseTimes, String doseTiming, String duration) {
                throw new UnsupportedOperationException();
            }
        };
        DurInteractionRepository interactions = new DurInteractionRepository() {
            @Override
            public List<DurInteraction> getInteractions() {
                return Collections.singletonList(new DurInteraction(
                        "ax",
                        "약물 \"A\"",
                        "미등록",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                ));
            }

            @Override
            public DurInteraction findById(String id) {
                return null;
            }
        };

        DrugRelationGraph graph = new DrugRelationGraphRepository(medications, interactions).load();
        String json = GraphCytoscapePayload.toJson(graph);

        assertTrue(json.contains("\"id\":\"a\""));
        assertTrue(json.contains("약물 \\\"A\\\""));
        assertTrue(json.contains("\"edges\":[]"));
        assertFalse(json.contains("\"interactionId\""));
    }
}
