package com.example.yakyeon.data.model;

public final class DurInteraction {

    private final String id;
    private final String drugAName;
    private final String drugBName;
    private final String drugACaption;
    private final String drugBCaption;
    private final String interactionTitle;
    private final String interactionReason;
    private final String interactionDescription;
    private final String caution;
    private final String severity;

    public DurInteraction(
            String id,
            String drugAName,
            String drugBName,
            String drugACaption,
            String drugBCaption,
            String interactionTitle,
            String interactionReason,
            String interactionDescription,
            String caution,
            String severity
    ) {
        this.id = id;
        this.drugAName = drugAName;
        this.drugBName = drugBName;
        this.drugACaption = drugACaption == null ? "" : drugACaption;
        this.drugBCaption = drugBCaption == null ? "" : drugBCaption;
        this.interactionTitle = interactionTitle == null ? "" : interactionTitle;
        this.interactionReason = interactionReason == null ? "" : interactionReason;
        this.interactionDescription = interactionDescription == null ? "" : interactionDescription;
        this.caution = caution == null ? "" : caution;
        this.severity = severity == null ? "" : severity;
    }

    public String getId() {
        return id;
    }

    public String getDrugAName() {
        return drugAName;
    }

    public String getDrugBName() {
        return drugBName;
    }

    public String getDrugACaption() {
        return drugACaption;
    }

    public String getDrugBCaption() {
        return drugBCaption;
    }

    public String getInteractionTitle() {
        return interactionTitle;
    }

    public String getInteractionReason() {
        return interactionReason;
    }

    public String getInteractionDescription() {
        return interactionDescription;
    }

    public String getCaution() {
        return caution;
    }

    public String getSeverity() {
        return severity;
    }
}
