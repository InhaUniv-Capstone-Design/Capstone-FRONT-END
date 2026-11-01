package com.example.yakyeon2.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Taking medicines and contraindication pairs are separate.
 * A saved name never creates a pair. Registry pairs stay empty.
 * The debug scenario is the only extra pair source, and it is not a registry result.
 */
public final class DrugRelations {

    public static final class Node {
        private final String medicineId;
        private final String name;

        public Node(String medicineId, String name) {
            this.medicineId = medicineId == null ? "" : medicineId;
            this.name = name == null ? "" : name;
        }

        public String getMedicineId() {
            return medicineId;
        }

        public String getName() {
            return name;
        }
    }

    public static final class Pair {
        private final String leftMedicineId;
        private final String leftName;
        private final String rightMedicineId;
        private final String rightName;
        private final boolean devScenario;

        public Pair(String leftMedicineId, String leftName, String rightMedicineId, String rightName) {
            this(leftMedicineId, leftName, rightMedicineId, rightName, false);
        }

        public Pair(
                String leftMedicineId,
                String leftName,
                String rightMedicineId,
                String rightName,
                boolean devScenario
        ) {
            this.leftMedicineId = leftMedicineId == null ? "" : leftMedicineId;
            this.leftName = leftName == null ? "" : leftName;
            this.rightMedicineId = rightMedicineId == null ? "" : rightMedicineId;
            this.rightName = rightName == null ? "" : rightName;
            this.devScenario = devScenario;
        }

        public String getLeftMedicineId() {
            return leftMedicineId;
        }

        public String getLeftName() {
            return leftName;
        }

        public String getRightMedicineId() {
            return rightMedicineId;
        }

        public String getRightName() {
            return rightName;
        }

        public boolean isDevScenario() {
            return devScenario;
        }
    }

    private DrugRelations() {
    }

    public static List<Node> takingNodes(MedicationStore store) {
        List<Node> nodes = new ArrayList<>();
        if (store == null) {
            return Collections.unmodifiableList(nodes);
        }
        for (SavedEnvelope envelope : store.envelopes()) {
            if (envelope.isStopped() || store.isSampleEnvelope(envelope.getId())) {
                continue;
            }
            for (SavedMedicine medicine : envelope.medicines()) {
                nodes.add(new Node(medicine.getId(), medicine.getName()));
            }
        }
        return Collections.unmodifiableList(nodes);
    }

    public static List<Pair> confirmedPairs(MedicationStore store) {
        if (store != null && store.isDevGraphRelationScenario()) {
            return DevGraphRelations.scenarioPairs(takingNodes(store));
        }
        return Collections.emptyList();
    }

    public static int confirmedCount(MedicationStore store) {
        return confirmedPairs(store).size();
    }
}
