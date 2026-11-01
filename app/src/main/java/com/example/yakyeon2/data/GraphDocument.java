package com.example.yakyeon2.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JSON passed to the offline graph page. Nodes and edges stay separate lists.
 */
public final class GraphDocument {

    private GraphDocument() {
    }

    public static String json(List<DrugRelations.Node> nodes, List<DrugRelations.Pair> pairs) {
        Set<String> related = new HashSet<>();
        if (pairs != null) {
            for (DrugRelations.Pair pair : pairs) {
                related.add(pair.getLeftMedicineId());
                related.add(pair.getRightMedicineId());
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append("{\"nodes\":[");
        if (nodes != null) {
            for (int index = 0; index < nodes.size(); index++) {
                if (index > 0) {
                    builder.append(',');
                }
                DrugRelations.Node node = nodes.get(index);
                builder.append("{\"id\":").append(quote(node.getMedicineId()));
                builder.append(",\"name\":").append(quote(node.getName()));
                builder.append(",\"related\":").append(related.contains(node.getMedicineId()) ? "true" : "false");
                builder.append('}');
            }
        }
        builder.append("],\"edges\":[");
        if (pairs != null) {
            for (int index = 0; index < pairs.size(); index++) {
                if (index > 0) {
                    builder.append(',');
                }
                DrugRelations.Pair pair = pairs.get(index);
                builder.append("{\"id\":").append(quote(edgeId(pair)));
                builder.append(",\"source\":").append(quote(pair.getLeftMedicineId()));
                builder.append(",\"target\":").append(quote(pair.getRightMedicineId()));
                builder.append('}');
            }
        }
        builder.append("]}");
        return builder.toString();
    }

    public static String edgeId(DrugRelations.Pair pair) {
        return pair.getLeftMedicineId() + "::" + pair.getRightMedicineId();
    }

    public static DrugRelations.Pair findPair(List<DrugRelations.Pair> pairs, String edgeId) {
        if (pairs == null || edgeId == null) {
            return null;
        }
        for (DrugRelations.Pair pair : pairs) {
            if (edgeId.equals(edgeId(pair))) {
                return pair;
            }
        }
        return null;
    }

    public static DrugRelations.Node findNode(List<DrugRelations.Node> nodes, String medicineId) {
        if (nodes == null || medicineId == null) {
            return null;
        }
        for (DrugRelations.Node node : nodes) {
            if (medicineId.equals(node.getMedicineId())) {
                return node;
            }
        }
        return null;
    }

    private static String quote(String value) {
        String text = value == null ? "" : value;
        StringBuilder builder = new StringBuilder(text.length() + 2);
        builder.append('"');
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
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
                default:
                    if (character < 0x20 || character == '\u2028' || character == '\u2029' || character == '<') {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                    break;
            }
        }
        builder.append('"');
        return builder.toString();
    }
}
