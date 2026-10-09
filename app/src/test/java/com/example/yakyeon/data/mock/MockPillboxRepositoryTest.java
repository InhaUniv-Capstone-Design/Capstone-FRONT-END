package com.example.yakyeon.data.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.yakyeon.data.model.DiscoveredPillbox;
import com.example.yakyeon.data.model.PillboxMedicine;
import com.example.yakyeon.data.model.PillboxSession;
import com.example.yakyeon.data.model.PillboxSlot;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MockPillboxRepositoryTest {

    @Test
    public void startsDisconnectedAndNeverReportsHardware() {
        MockPillboxRepository repository = new MockPillboxRepository();

        assertFalse(repository.isHardwareConnected());
        assertFalse(repository.hasSimulatedLink());
        assertNull(repository.session());
    }

    @Test
    public void simulatedScanReturnsTheSampleDeviceOnly() {
        MockPillboxRepository repository = new MockPillboxRepository();

        List<DiscoveredPillbox> devices = repository.simulatedNearbyDevices();

        assertEquals(1, devices.size());
        assertEquals(MockPillboxRepository.SAMPLE_DEVICE_ID, devices.get(0).getId());
        assertEquals("스마트 약통", devices.get(0).getName());
        assertEquals("기기 번호 0328", devices.get(0).getSerialLabel());
        assertFalse(repository.linkSimulatedDevice("unknown"));
        assertFalse(repository.hasSimulatedLink());
    }

    @Test
    public void linkingSampleDeviceShowsDesignedSessionWithoutHardware() {
        MockPillboxRepository repository = new MockPillboxRepository();

        assertTrue(repository.linkSimulatedDevice(MockPillboxRepository.SAMPLE_DEVICE_ID));

        PillboxSession session = repository.session();
        assertFalse(repository.isHardwareConnected());
        assertTrue(session.isSimulated());
        assertEquals(MockPillboxRepository.SAMPLE_SETTINGS_NAME, session.getDisplayName());
        assertEquals(MockPillboxRepository.SAMPLE_BATTERY_LABEL, session.getBatteryLabel());
        assertEquals(MockPillboxRepository.SAMPLE_CONNECTION_DETAIL, session.getConnectionDetail());
        assertEquals(MockPillboxRepository.SAMPLE_SYNC_LABEL, session.getSyncLabel());
        assertTrue(session.isNotificationsEnabled());
        assertEquals(3, session.getSlots().size());
        assertEquals(PillboxSlot.Status.OPEN_ERROR, session.getSlots().get(2).getStatus());
        assertEquals("3번 칸이 열렸습니다. 확인 후 다시 닫아주세요.", session.getSlots().get(2).getErrorMessage());
        assertEquals("피부염 크림", session.getSlots().get(2).getMedicines().get(0).getName());
        assertTrue(session.getSlots().get(1).getMedicines().get(2).isAsNeeded());
    }

    @Test
    public void unlinkClearsTheSimulatedSession() {
        MockPillboxRepository repository = new MockPillboxRepository();
        repository.linkSimulatedDevice(MockPillboxRepository.SAMPLE_DEVICE_ID);

        repository.unlinkSimulatedDevice();

        assertFalse(repository.hasSimulatedLink());
        assertNull(repository.session());
        assertFalse(repository.isHardwareConnected());
    }

    @Test
    public void renameNotificationsAndSyncStayInsideTheSimulatedSession() {
        MockPillboxRepository repository = new MockPillboxRepository();
        assertFalse(repository.rename("거실 약통"));
        repository.linkSimulatedDevice(MockPillboxRepository.SAMPLE_DEVICE_ID);

        assertTrue(repository.rename("  거실 약통  "));
        assertFalse(repository.rename("   "));
        repository.setNotificationsEnabled(false);
        Calendar calendar = Calendar.getInstance(Locale.KOREA);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 38);
        String syncLabel = repository.recordSimulatedSync(calendar);

        PillboxSession session = repository.session();
        assertEquals("거실 약통", session.getDisplayName());
        assertFalse(session.isNotificationsEnabled());
        assertEquals("최근 동기화 오늘 09:38", syncLabel);
        assertEquals(syncLabel, session.getSyncLabel());
    }

    @Test
    public void saveAlarmTimesRejectsTheWrongCountAndKeepsValidTimes() {
        MockPillboxRepository repository = new MockPillboxRepository();
        assertFalse(repository.saveAlarmTimes(Collections.singletonMap("s2-dm", times("08:00", "20:00"))));
        repository.linkSimulatedDevice(MockPillboxRepository.SAMPLE_DEVICE_ID);

        Map<String, List<String>> invalid = new HashMap<>();
        invalid.put("s2-dm", times("08:00"));
        assertFalse(repository.saveAlarmTimes(invalid));
        assertEquals("", alarmAt(repository, "s2-dm", 0));

        Map<String, List<String>> valid = new HashMap<>();
        valid.put("s2-dm", times("08:00", "20:00"));
        assertTrue(repository.saveAlarmTimes(valid));
        assertEquals("08:00", alarmAt(repository, "s2-dm", 0));
        assertEquals("20:00", alarmAt(repository, "s2-dm", 1));
        assertFalse(repository.isHardwareConnected());
    }

    private static List<String> times(String... values) {
        List<String> times = new ArrayList<>();
        Collections.addAll(times, values);
        return times;
    }

    private static String alarmAt(MockPillboxRepository repository, String medicineId, int index) {
        for (PillboxSlot slot : repository.session().getSlots()) {
            for (PillboxMedicine medicine : slot.getMedicines()) {
                if (medicineId.equals(medicine.getId())) {
                    return medicine.getAlarmTimes().get(index);
                }
            }
        }
        return null;
    }
}
