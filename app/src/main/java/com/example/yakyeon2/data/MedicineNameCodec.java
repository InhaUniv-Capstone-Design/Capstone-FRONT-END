package com.example.yakyeon2.data;

import java.util.ArrayList;
import java.util.List;

public final class MedicineNameCodec {

    private MedicineNameCodec() {
    }

    public static String encode(List<String> names) {
        if (names == null || names.isEmpty()) {
            return "";
        }
        StringBuilder raw = new StringBuilder();
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            if (raw.length() > 0) {
                raw.append('\n');
            }
            raw.append(name.trim());
        }
        return raw.toString();
    }

    public static List<String> decode(String raw) {
        List<String> names = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return names;
        }
        String[] lines = raw.split("\n", -1);
        for (String line : lines) {
            String name = line.trim();
            if (name.isEmpty() || names.contains(name)) {
                continue;
            }
            names.add(name);
        }
        return names;
    }
}
