package com.example.yakyeon2.data;

import java.time.LocalDate;
import java.util.EnumSet;

public final class MedicineDraft {

    private String name;
    private DoseAmount amount;
    private final EnumSet<DoseRound> rounds = EnumSet.noneOf(DoseRound.class);
    private final EnumSet<DoseTiming> timings = EnumSet.noneOf(DoseTiming.class);
    private LocalDate start;
    private LocalDate end;
    private boolean confirmed;
    private Long drugId;
    private String doseText;
    private String doseUnit;
    private boolean prn;
    private String mealRelation;
    private Integer mealOffsetMin;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.drugId = null;
    }

    public Long getDrugId() {
        return drugId;
    }

    public void setDrugId(Long drugId) {
        this.drugId = drugId;
    }

    public String getDoseText() {
        return doseText;
    }

    public void setDoseText(String doseText) {
        this.doseText = doseText;
    }

    public String getDoseUnit() {
        return doseUnit;
    }

    public boolean hasDose() {
        if (amount != null) {
            return true;
        }
        return doseText != null && !doseText.isEmpty() && DoseAmount.knownUnit(doseUnit);
    }

    public void setFreeDose(String quantity, String unit) {
        this.doseText = quantity;
        this.doseUnit = unit;
        this.amount = DoseAmount.from(unit, quantity);
    }

    public boolean isPrn() {
        return prn;
    }

    public void setPrn(boolean prn) {
        this.prn = prn;
    }

    public String getMealRelation() {
        return mealRelation;
    }

    public Integer getMealOffsetMin() {
        return mealOffsetMin;
    }

    public void setMealRelation(String mealRelation, Integer mealOffsetMin) {
        this.mealRelation = mealRelation;
        this.mealOffsetMin = mealOffsetMin;
    }

    public void clearMealRelation() {
        this.mealRelation = null;
        this.mealOffsetMin = null;
    }

    public DoseAmount getAmount() {
        return amount;
    }

    public void setAmount(DoseAmount amount) {
        this.amount = amount;
        if (amount == null) {
            this.doseText = null;
            this.doseUnit = null;
            return;
        }
        this.doseText = amount.quantityText();
        this.doseUnit = amount.unitText();
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

    public void setTiming(DoseTiming timing) {
        timings.clear();
        if (timing != null) {
            timings.add(timing);
        }
    }

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }
}
