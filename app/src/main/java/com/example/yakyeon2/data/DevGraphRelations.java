package com.example.yakyeon2.data;

import java.util.Collections;
import java.util.List;

/**
 * One fixed test pair for the debug graph scenario.
 * It is not a registry result and is not stored with the medicines.
 */
public final class DevGraphRelations {

    private DevGraphRelations() {
    }

    public static List<DrugRelations.Pair> scenarioPairs(List<DrugRelations.Node> nodes) {
        if (nodes == null || nodes.size() < 2) {
            return Collections.emptyList();
        }
        DrugRelations.Node left = nodes.get(0);
        DrugRelations.Node right = nodes.get(1);
        return Collections.singletonList(new DrugRelations.Pair(
                left.getMedicineId(),
                left.getName(),
                right.getMedicineId(),
                right.getName(),
                true
        ));
    }
}
