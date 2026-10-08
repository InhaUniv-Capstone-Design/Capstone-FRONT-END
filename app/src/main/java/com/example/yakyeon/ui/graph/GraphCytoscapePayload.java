package com.example.yakyeon.ui.graph;

import com.example.yakyeon.data.model.DrugGraphEdge;
import com.example.yakyeon.data.model.DrugRelationGraph;
import com.example.yakyeon.data.model.Medication;

public final class GraphCytoscapePayload {

    private GraphCytoscapePayload() {
    }

    public static String toJson(DrugRelationGraph graph) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"nodes\":[");
        ListAppender nodes = new ListAppender(builder);
        for (Medication medication : graph.getMedications()) {
            nodes.next();
            builder.append("{\"data\":{\"id\":\"")
                    .append(escape(medication.getId()))
                    .append("\",\"label\":\"")
                    .append(escape(medication.getName()))
                    .append("\"}}");
        }
        builder.append("],\"edges\":[");
        ListAppender edges = new ListAppender(builder);
        for (DrugGraphEdge edge : graph.getEdges()) {
            edges.next();
            builder.append("{\"data\":{\"id\":\"")
                    .append(escape(edge.getInteractionId()))
                    .append("\",\"source\":\"")
                    .append(escape(edge.getSourceMedicationId()))
                    .append("\",\"target\":\"")
                    .append(escape(edge.getTargetMedicationId()))
                    .append("\",\"interactionId\":\"")
                    .append(escape(edge.getInteractionId()))
                    .append("\"}}");
        }
        builder.append("]}");
        return builder.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\u2028':
                    builder.append("\\u2028");
                    break;
                case '\u2029':
                    builder.append("\\u2029");
                    break;
                default:
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                    break;
            }
        }
        return builder.toString();
    }

    private static final class ListAppender {
        private final StringBuilder builder;
        private boolean started;

        private ListAppender(StringBuilder builder) {
            this.builder = builder;
        }

        private void next() {
            if (started) {
                builder.append(',');
            }
            started = true;
        }
    }
}
