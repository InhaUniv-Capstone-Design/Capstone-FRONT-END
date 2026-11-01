package com.example.yakyeon2.data;

public final class LegalMarks {

    private boolean terms;
    private boolean privacy;
    private boolean sensitive;
    private boolean drugCollection;
    private boolean drugNotice;
    private boolean device;

    public boolean isTerms() {
        return terms;
    }

    public void setTerms(boolean terms) {
        this.terms = terms;
    }

    public boolean isPrivacy() {
        return privacy;
    }

    public void setPrivacy(boolean privacy) {
        this.privacy = privacy;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public boolean isDrugCollection() {
        return drugCollection;
    }

    public void setDrugCollection(boolean drugCollection) {
        this.drugCollection = drugCollection;
    }

    public boolean isDrugNotice() {
        return drugNotice;
    }

    public void setDrugNotice(boolean drugNotice) {
        this.drugNotice = drugNotice;
    }

    public boolean isDevice() {
        return device;
    }

    public void setDevice(boolean device) {
        this.device = device;
    }

    public String exportState() {
        return bit(terms) + bit(privacy) + bit(sensitive) + bit(drugCollection) + bit(drugNotice) + bit(device);
    }

    public boolean restoreState(String raw) {
        if (raw == null || raw.length() != 6) {
            return false;
        }
        for (int index = 0; index < raw.length(); index++) {
            char mark = raw.charAt(index);
            if (mark != '0' && mark != '1') {
                return false;
            }
        }
        terms = raw.charAt(0) == '1';
        privacy = raw.charAt(1) == '1';
        sensitive = raw.charAt(2) == '1';
        drugCollection = raw.charAt(3) == '1';
        drugNotice = raw.charAt(4) == '1';
        device = raw.charAt(5) == '1';
        return true;
    }

    private static String bit(boolean value) {
        return value ? "1" : "0";
    }
}
