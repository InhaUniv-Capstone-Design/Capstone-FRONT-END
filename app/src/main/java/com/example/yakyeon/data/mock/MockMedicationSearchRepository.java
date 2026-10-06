package com.example.yakyeon.data.mock;

import com.example.yakyeon.data.MedicationSearchRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MockMedicationSearchRepository implements MedicationSearchRepository {

    private static final List<String> CATALOG = List.of(
            "타이레놀정",
            "아모디핀정",
            "글리메피리드정",
            "메트포르민정",
            "메트포르민정 250mg",
            "메트포르민정 500mg",
            "메트포르민정 1000mg",
            "로수바스타틴정",
            "아토르바스타틴정 10mg",
            "암로디핀정 5mg",
            "오메프라졸캡슐 20mg"
    );

    private static MockMedicationSearchRepository instance;

    public static MockMedicationSearchRepository getInstance() {
        if (instance == null) {
            instance = new MockMedicationSearchRepository();
        }
        return instance;
    }

    @Override
    public List<String> searchMedicationNames(String query) {
        String needle = query == null ? "" : query.trim();
        List<String> matched = new ArrayList<>();
        for (String name : CATALOG) {
            if (needle.isEmpty() || name.contains(needle)) {
                matched.add(name);
            }
        }
        return Collections.unmodifiableList(matched);
    }

    @Override
    public boolean isKnownName(String name) {
        if (name == null) {
            return false;
        }
        String candidate = name.trim();
        for (String catalogName : CATALOG) {
            if (catalogName.equals(candidate)) {
                return true;
            }
        }
        return false;
    }
}
