package com.example.yakyeon.data.model;

public final class DiscoveredPillbox {

    private final String id;
    private final String name;
    private final String serialLabel;

    public DiscoveredPillbox(String id, String name, String serialLabel) {
        this.id = id;
        this.name = name;
        this.serialLabel = serialLabel;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSerialLabel() {
        return serialLabel;
    }
}
