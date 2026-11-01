package com.example.yakyeon2.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MedicineNameCatalog {

    public enum Source {
        EXAMPLE,
        CUSTOM
    }

    public enum AddStatus {
        ADDED,
        DUPLICATE,
        EMPTY
    }

    public static final class Match {
        public final String name;
        public final Source source;

        public Match(String name, Source source) {
            this.name = name;
            this.source = source;
        }
    }

    public static final class AddResult {
        public final AddStatus status;
        public final String name;

        public AddResult(AddStatus status, String name) {
            this.status = status;
            this.name = name;
        }
    }

    private static final List<String> EXAMPLES = Collections.unmodifiableList(Arrays.asList(
            "약1", "약2", "약3", "약4", "약5", "약6", "약7", "약8", "약9", "약10"
    ));

    private final List<String> custom = new ArrayList<>();

    public List<String> examples() {
        return EXAMPLES;
    }

    public List<String> customNames() {
        return Collections.unmodifiableList(custom);
    }

    public Source sourceOf(String raw) {
        String name = trim(raw);
        if (EXAMPLES.contains(name)) {
            return Source.EXAMPLE;
        }
        if (custom.contains(name)) {
            return Source.CUSTOM;
        }
        return null;
    }

    public List<Match> search(String query) {
        String needle = trim(query);
        List<Match> hits = new ArrayList<>();
        for (String name : EXAMPLES) {
            if (needle.isEmpty() || name.contains(needle)) {
                hits.add(new Match(name, Source.EXAMPLE));
            }
        }
        for (String name : custom) {
            if (needle.isEmpty() || name.contains(needle)) {
                hits.add(new Match(name, Source.CUSTOM));
            }
        }
        return hits;
    }

    public AddResult addCustom(String raw) {
        String name = trim(raw);
        if (name.isEmpty() || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {
            return new AddResult(AddStatus.EMPTY, null);
        }
        if (EXAMPLES.contains(name) || custom.contains(name)) {
            return new AddResult(AddStatus.DUPLICATE, name);
        }
        custom.add(name);
        return new AddResult(AddStatus.ADDED, name);
    }

    public void restore(List<String> saved) {
        custom.clear();
        if (saved == null) {
            return;
        }
        for (String name : saved) {
            addCustom(name);
        }
    }

    private static String trim(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
