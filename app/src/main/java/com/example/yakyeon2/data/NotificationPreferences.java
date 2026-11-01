package com.example.yakyeon2.data;

public final class NotificationPreferences {

    private boolean doseTime = true;
    private boolean missedDose = true;
    private boolean endingSoon = true;
    private boolean pillboxStatus = true;
    private boolean boxOpened = true;
    private boolean doseButton = true;
    private boolean missedByDevice = true;
    private boolean deviceFault = true;
    private boolean savedBoxOpened;
    private boolean savedDoseButton;
    private boolean savedMissedByDevice;
    private boolean savedDeviceFault;

    public boolean isDoseTime() {
        return doseTime;
    }

    public void setDoseTime(boolean doseTime) {
        this.doseTime = doseTime;
    }

    public boolean isMissedDose() {
        return missedDose;
    }

    public void setMissedDose(boolean missedDose) {
        this.missedDose = missedDose;
    }

    public boolean isEndingSoon() {
        return endingSoon;
    }

    public void setEndingSoon(boolean endingSoon) {
        this.endingSoon = endingSoon;
    }

    public boolean isPillboxStatus() {
        return pillboxStatus;
    }

    public void setPillboxStatus(boolean enabled) {
        if (pillboxStatus == enabled) {
            return;
        }
        if (!enabled) {
            savedBoxOpened = boxOpened;
            savedDoseButton = doseButton;
            savedMissedByDevice = missedByDevice;
            savedDeviceFault = deviceFault;
            boxOpened = false;
            doseButton = false;
            missedByDevice = false;
            deviceFault = false;
        } else {
            boxOpened = savedBoxOpened;
            doseButton = savedDoseButton;
            missedByDevice = savedMissedByDevice;
            deviceFault = savedDeviceFault;
        }
        pillboxStatus = enabled;
    }

    public void setBoxOpened(boolean boxOpened) {
        if (!pillboxStatus) {
            return;
        }
        this.boxOpened = boxOpened;
    }

    public void setDoseButton(boolean doseButton) {
        if (!pillboxStatus) {
            return;
        }
        this.doseButton = doseButton;
    }

    public void setMissedByDevice(boolean missedByDevice) {
        if (!pillboxStatus) {
            return;
        }
        this.missedByDevice = missedByDevice;
    }

    public void setDeviceFault(boolean deviceFault) {
        if (!pillboxStatus) {
            return;
        }
        this.deviceFault = deviceFault;
    }

    public void applyServerFlags(
            boolean dose,
            boolean missed,
            boolean ending,
            boolean device,
            boolean open,
            boolean taken,
            boolean missedDevice,
            boolean fault
    ) {
        doseTime = dose;
        missedDose = missed;
        endingSoon = ending;
        pillboxStatus = device;
        boxOpened = open;
        doseButton = taken;
        missedByDevice = missedDevice;
        deviceFault = fault;
        if (device) {
            savedBoxOpened = open;
            savedDoseButton = taken;
            savedMissedByDevice = missedDevice;
            savedDeviceFault = fault;
        }
    }

    public void reset() {
        doseTime = true;
        missedDose = true;
        endingSoon = true;
        pillboxStatus = true;
        boxOpened = true;
        doseButton = true;
        missedByDevice = true;
        deviceFault = true;
        savedBoxOpened = false;
        savedDoseButton = false;
        savedMissedByDevice = false;
        savedDeviceFault = false;
    }

    public String exportState() {
        return bit(doseTime) + bit(missedDose) + bit(endingSoon) + bit(pillboxStatus)
                + bit(boxOpened) + bit(doseButton) + bit(missedByDevice) + bit(deviceFault)
                + bit(savedBoxOpened) + bit(savedDoseButton) + bit(savedMissedByDevice) + bit(savedDeviceFault);
    }

    public boolean restoreState(String raw) {
        if (raw == null || raw.length() != 12) {
            return false;
        }
        for (int index = 0; index < raw.length(); index++) {
            char mark = raw.charAt(index);
            if (mark != '0' && mark != '1') {
                return false;
            }
        }
        doseTime = on(raw, 0);
        missedDose = on(raw, 1);
        endingSoon = on(raw, 2);
        pillboxStatus = on(raw, 3);
        boxOpened = on(raw, 4);
        doseButton = on(raw, 5);
        missedByDevice = on(raw, 6);
        deviceFault = on(raw, 7);
        savedBoxOpened = on(raw, 8);
        savedDoseButton = on(raw, 9);
        savedMissedByDevice = on(raw, 10);
        savedDeviceFault = on(raw, 11);
        return true;
    }

    private static String bit(boolean value) {
        return value ? "1" : "0";
    }

    private static boolean on(String raw, int index) {
        return raw.charAt(index) == '1';
    }

    public boolean isBoxOpened() {
        return boxOpened;
    }

    public boolean isDoseButton() {
        return doseButton;
    }

    public boolean isMissedByDevice() {
        return missedByDevice;
    }

    public boolean isDeviceFault() {
        return deviceFault;
    }
}
