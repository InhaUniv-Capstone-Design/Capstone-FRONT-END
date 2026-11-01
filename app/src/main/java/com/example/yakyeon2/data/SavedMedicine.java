package com.example.yakyeon2.data;

import java.time.LocalDate;
import java.util.EnumSet;

public final class SavedMedicine {

    private final String id;
    private String name;
    private DoseAmount amount;
    private final EnumSet<DoseRound> rounds = EnumSet.noneOf(DoseRound.class);
    private final EnumSet<DoseTiming> timings = EnumSet.noneOf(DoseTiming.class);
    private LocalDate start;
    private LocalDate end;

    public SavedMedicine(String id, MedicineDraft draft) {
        this.id = id;
        apply(draft);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DoseAmount getAmount() {
        return amount;
    }

    public EnumSet<DoseRound> rounds() {
        return rounds;
    }

    public EnumSet<DoseTiming> timings() {
        return timings;
    }

    public DoseTiming getTiming() {
        if (timings.size() != 1) {
            return null;
        }
        return timings.iterator().next();
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void apply(MedicineDraft draft) {
        name = draft.getName();
        amount = draft.getAmount();
        rounds.clear();
        rounds.addAll(draft.rounds());
        timings.clear();
        timings.addAll(draft.timings());
        start = draft.getStart();
        end = draft.getEnd();
    }
}
