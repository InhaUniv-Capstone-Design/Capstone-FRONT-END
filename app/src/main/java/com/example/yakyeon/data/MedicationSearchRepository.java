package com.example.yakyeon.data;

import java.util.List;

public interface MedicationSearchRepository {

    List<String> searchMedicationNames(String query);

    boolean isKnownName(String name);
}
