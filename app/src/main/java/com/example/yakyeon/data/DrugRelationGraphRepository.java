package com.example.yakyeon.data;

import com.example.yakyeon.data.model.DrugGraphEdge;
import com.example.yakyeon.data.model.DrugRelationGraph;
import com.example.yakyeon.data.model.DurInteraction;
import com.example.yakyeon.data.model.Medication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DrugRelationGraphRepository {

    private final MedicationRepository medications;
    private final DurInteractionRepository interactions;

    public DrugRelationGraphRepository(
            MedicationRepository medications,
            DurInteractionRepository interactions
    ) {
        this.medications = medications;
        this.interactions = interactions;
    }

    public DrugRelationGraph load() {
        List<Medication> registered = medications.getMedications();
        Map<String, Medication> byName = new LinkedHashMap<>();
        for (Medication medication : registered) {
            String key = normalize(medication.getName());
            if (!key.isEmpty() && !byName.containsKey(key)) {
                byName.put(key, medication);
            }
        }

        List<DrugGraphEdge> edges = new ArrayList<>();
        for (DurInteraction interaction : interactions.getInteractions()) {
            Medication source = byName.get(normalize(interaction.getDrugAName()));
            Medication target = byName.get(normalize(interaction.getDrugBName()));
            if (source == null || target == null || source.getId().equals(target.getId())) {
                continue;
            }
            edges.add(new DrugGraphEdge(interaction.getId(), source.getId(), target.getId()));
        }
        return new DrugRelationGraph(registered, edges);
    }

    public DurInteraction findInteraction(String interactionId) {
        return interactions.findById(interactionId);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
