package com.example.yakyeon.data;

import com.example.yakyeon.data.model.DiscoveredPillbox;
import com.example.yakyeon.data.model.PillboxSession;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public interface PillboxRepository {

    boolean isHardwareConnected();

    boolean hasSimulatedLink();

    List<DiscoveredPillbox> simulatedNearbyDevices();

    boolean linkSimulatedDevice(String deviceId);

    void unlinkSimulatedDevice();

    PillboxSession session();

    boolean rename(String displayName);

    void setNotificationsEnabled(boolean enabled);

    String recordSimulatedSync(Calendar calendar);

    boolean saveAlarmTimes(Map<String, List<String>> alarmTimes);
}
