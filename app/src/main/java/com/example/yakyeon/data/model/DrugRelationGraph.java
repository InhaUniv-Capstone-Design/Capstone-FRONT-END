package com.example.yakyeon.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DrugRelationGraph {

    private final List<Medication> medications;
    private final List<DrugGraphEdge> edges;

    public DrugRelationGraph(List<Medication> medications, List<DrugGraphEdge> edges) {
        this.medications = Collections.unmodifiableList(new ArrayList<>(medications));
        this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
    }

    public List<Medication> getMedications() {
        return medications;
    }

    public List<DrugGraphEdge> getEdges() {
        return edges;
    }
}
