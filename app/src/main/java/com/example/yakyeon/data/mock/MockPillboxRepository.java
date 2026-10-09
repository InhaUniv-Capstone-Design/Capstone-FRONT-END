package com.example.yakyeon.data.mock;

import com.example.yakyeon.data.PillboxRepository;
import com.example.yakyeon.data.model.DiscoveredPillbox;
import com.example.yakyeon.data.model.PillboxMedicine;
import com.example.yakyeon.data.model.PillboxSession;
import com.example.yakyeon.data.model.PillboxSlot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sample pillbox session for the connection screens.
 * This repository does not scan, pair, or communicate with Bluetooth or ESP32 hardware.
 * {@link #isHardwareConnected()} stays false. Linked screens read {@link #hasSimulatedLink()}.
 */
public final class MockPillboxRepository implements PillboxRepository {

    public static final String SAMPLE_DEVICE_ID = "yakyeon-0328";
    public static final String SAMPLE_SETTINGS_NAME = "기기명";
    public static final String SAMPLE_BATTERY_LABEL = "배터리 82%";
    public static final String SAMPLE_CONNECTION_DETAIL = "방금 전 연결 · 배터리 82%";
    public static final String SAMPLE_SYNC_LABEL = "최근 동기화 오늘 09:38";

    private static MockPillboxRepository instance;

    private final DiscoveredPillbox sampleDevice = new DiscoveredPillbox(
            SAMPLE_DEVICE_ID,
            "스마트 약통",
            "기기 번호 0328"
    );
    private final List<SlotSeed> slotSeeds = new ArrayList<>();
    private final Map<String, MutableMedicine> medicines = new LinkedHashMap<>();

    private boolean simulatedLink;
    private String displayName = SAMPLE_SETTINGS_NAME;
    private boolean notificationsEnabled = true;
    private String syncLabel = SAMPLE_SYNC_LABEL;

    public MockPillboxRepository() {
        slotSeeds.add(new SlotSeed(
                1,
                "한의원 처방",
                "약국: 한의원 약국",
                PillboxSlot.Status.NORMAL,
                null
        ));
        slotSeeds.add(new SlotSeed(
                2,
                "내과 처방",
                "약국: 중앙약국",
                PillboxSlot.Status.NORMAL,
                null
        ));
        slotSeeds.add(new SlotSeed(
                3,
                "피부과 처방",
                "약국: 피부과 약국",
                PillboxSlot.Status.OPEN_ERROR,
                "3번 칸이 열렸습니다. 확인 후 다시 닫아주세요."
        ));
        addMedicine(1, "s1-vitamin", "비타민D", "1일 1회", false, 1);
        addMedicine(1, "s1-calcium", "칼슘제", "1일 1회", false, 1);
        addMedicine(1, "s1-joint", "관절보호제", "1일 1회", false, 1);
        addMedicine(2, "s2-bp", "혈압약", "1일 1회", false, 1);
        addMedicine(2, "s2-dm", "당뇨약", "1일 2회", false, 2);
        addMedicine(2, "s2-digest", "소화제", "필요 시", true, 1);
        addMedicine(3, "s3-cream", "피부염 크림", "1일 2회", false, 2);
        addMedicine(3, "s3-anti", "항히스타민", "필요 시", true, 1);
        addMedicine(3, "s3-steroid", "스테로이드 연고", "1일 1회", false, 1);
    }

    public static MockPillboxRepository getInstance() {
        if (instance == null) {
            instance = new MockPillboxRepository();
        }
        return instance;
    }

    @Override
    public boolean isHardwareConnected() {
        return false;
    }

    @Override
    public boolean hasSimulatedLink() {
        return simulatedLink;
    }

    @Override
    public List<DiscoveredPillbox> simulatedNearbyDevices() {
        List<DiscoveredPillbox> devices = new ArrayList<>();
        devices.add(sampleDevice);
        return Collections.unmodifiableList(devices);
    }

    @Override
    public boolean linkSimulatedDevice(String deviceId) {
        if (!SAMPLE_DEVICE_ID.equals(deviceId)) {
            return false;
        }
        simulatedLink = true;
        return true;
    }

    @Override
    public void unlinkSimulatedDevice() {
        simulatedLink = false;
    }

    @Override
    public PillboxSession session() {
        if (!simulatedLink) {
            return null;
        }
        return new PillboxSession(
                displayName,
                SAMPLE_CONNECTION_DETAIL,
                SAMPLE_BATTERY_LABEL,
                syncLabel,
                notificationsEnabled,
                true,
                snapshotSlots()
        );
    }

    @Override
    public boolean rename(String nextName) {
        if (!simulatedLink || nextName == null) {
            return false;
        }
        String trimmed = nextName.trim();
        if (trimmed.length() == 0) {
            return false;
        }
        displayName = trimmed;
        return true;
    }

    @Override
    public void setNotificationsEnabled(boolean enabled) {
        if (simulatedLink) {
            notificationsEnabled = enabled;
        }
    }

    @Override
    public String recordSimulatedSync(Calendar calendar) {
        if (!simulatedLink) {
            return syncLabel;
        }
        syncLabel = String.format(
                Locale.KOREA,
                "최근 동기화 오늘 %02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
        );
        return syncLabel;
    }

    @Override
    public boolean saveAlarmTimes(Map<String, List<String>> alarmTimes) {
        if (!simulatedLink || alarmTimes == null) {
            return false;
        }
        for (Map.Entry<String, List<String>> entry : alarmTimes.entrySet()) {
            MutableMedicine medicine = medicines.get(entry.getKey());
            if (medicine == null || entry.getValue() == null
                    || entry.getValue().size() != medicine.times.size()) {
                return false;
            }
        }
        for (Map.Entry<String, List<String>> entry : alarmTimes.entrySet()) {
            MutableMedicine medicine = medicines.get(entry.getKey());
            medicine.times.clear();
            medicine.times.addAll(entry.getValue());
        }
        return true;
    }

    private List<PillboxSlot> snapshotSlots() {
        List<PillboxSlot> slots = new ArrayList<>();
        for (SlotSeed seed : slotSeeds) {
            List<PillboxMedicine> slotMedicines = new ArrayList<>();
            for (MutableMedicine medicine : medicines.values()) {
                if (medicine.slotNumber == seed.number) {
                    slotMedicines.add(medicine.toModel());
                }
            }
            slots.add(new PillboxSlot(
                    seed.number,
                    seed.prescriptionName,
                    seed.pharmacyLabel,
                    seed.status,
                    seed.errorMessage,
                    slotMedicines
            ));
        }
        return slots;
    }

    private void addMedicine(
            int slotNumber,
            String id,
            String name,
            String frequency,
            boolean asNeeded,
            int alarmCount
    ) {
        medicines.put(id, new MutableMedicine(id, slotNumber, name, frequency, asNeeded, alarmCount));
    }

    private static final class SlotSeed {
        private final int number;
        private final String prescriptionName;
        private final String pharmacyLabel;
        private final PillboxSlot.Status status;
        private final String errorMessage;

        private SlotSeed(
                int number,
                String prescriptionName,
                String pharmacyLabel,
                PillboxSlot.Status status,
                String errorMessage
        ) {
            this.number = number;
            this.prescriptionName = prescriptionName;
            this.pharmacyLabel = pharmacyLabel;
            this.status = status;
            this.errorMessage = errorMessage;
        }
    }

    private static final class MutableMedicine {
        private final String id;
        private final int slotNumber;
        private final String name;
        private final String frequency;
        private final boolean asNeeded;
        private final List<String> times = new ArrayList<>();

        private MutableMedicine(
                String id,
                int slotNumber,
                String name,
                String frequency,
                boolean asNeeded,
                int alarmCount
        ) {
            this.id = id;
            this.slotNumber = slotNumber;
            this.name = name;
            this.frequency = frequency;
            this.asNeeded = asNeeded;
            for (int index = 0; index < alarmCount; index++) {
                times.add("");
            }
        }

        private PillboxMedicine toModel() {
            return new PillboxMedicine(id, name, frequency, asNeeded, times);
        }
    }
}
