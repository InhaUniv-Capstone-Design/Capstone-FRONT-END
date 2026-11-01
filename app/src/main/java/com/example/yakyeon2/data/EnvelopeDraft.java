package com.example.yakyeon2.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class EnvelopeDraft {

    private String hospitalName = "";
    private String pharmacyName = "";
    private String prescribedDate;
    private boolean fromOcr;
    private final List<MedicineDraft> medicines = new ArrayList<>();
    private final Map<DoseRound, Integer> alarmMinutes = new EnumMap<>(DoseRound.class);
    private Integer slotIndex;
    private boolean collectionChecked;
    private boolean noticeChecked;
    private boolean devSample;
    private boolean fixturePractice;

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName == null ? "" : hospitalName;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName == null ? "" : pharmacyName;
    }

    public String getPrescribedDate() {
        return prescribedDate;
    }

    public void setPrescribedDate(String prescribedDate) {
        this.prescribedDate = prescribedDate;
    }

    public boolean isFromOcr() {
        return fromOcr;
    }

    public void setFromOcr(boolean fromOcr) {
        this.fromOcr = fromOcr;
    }

    public List<MedicineDraft> medicines() {
        return medicines;
    }

    public Map<DoseRound, Integer> alarmMinutes() {
        return alarmMinutes;
    }

    public Integer getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(Integer slotIndex) {
        this.slotIndex = slotIndex;
    }

    public boolean isCollectionChecked() {
        return collectionChecked;
    }

    public void setCollectionChecked(boolean collectionChecked) {
        this.collectionChecked = collectionChecked;
    }

    public boolean isNoticeChecked() {
        return noticeChecked;
    }

    public void setNoticeChecked(boolean noticeChecked) {
        this.noticeChecked = noticeChecked;
    }

    public boolean isDevSample() {
        return devSample;
    }

    public void setDevSample(boolean devSample) {
        this.devSample = devSample;
    }

    public boolean isFixturePractice() {
        return fixturePractice;
    }

    public void setFixturePractice(boolean fixturePractice) {
        this.fixturePractice = fixturePractice;
    }
}
