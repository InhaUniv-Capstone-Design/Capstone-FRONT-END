package com.example.yakyeon.data.mock;

import com.example.yakyeon.data.MedicationReminderRepository;
import com.example.yakyeon.data.model.ReminderDay;
import com.example.yakyeon.data.model.ReminderRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MockMedicationReminderRepository implements MedicationReminderRepository {

    private static MockMedicationReminderRepository instance;

    private final List<ReminderRecord> records = new ArrayList<>();

    public MockMedicationReminderRepository() {
        records.add(record("2026-09-25-0805", "2026.09.25 (목)", "08:05", "아스피린 100mg", ReminderRecord.STATUS_ON_TIME));
        records.add(record("2026-09-25-1210", "2026.09.25 (목)", "12:10", "메트포르민 500mg", ReminderRecord.STATUS_ON_TIME));
        records.add(record("2026-09-25-1845", "2026.09.25 (목)", "18:45", "와파린 2mg", ReminderRecord.STATUS_DELAYED));
        records.add(record("2026-09-24-0802", "2026.09.24 (수)", "08:02", "아스피린 100mg", ReminderRecord.STATUS_ON_TIME));
        records.add(record("2026-09-24-1200", "2026.09.24 (수)", "12:00", "메트포르민 500mg", ReminderRecord.STATUS_MISSED));
        records.add(record("2026-09-24-1830", "2026.09.24 (수)", "18:30", "와파린 2mg", ReminderRecord.STATUS_ON_TIME));
        records.add(record("2026-09-23-0840", "2026.09.23 (화)", "08:40", "아스피린 100mg", ReminderRecord.STATUS_DELAYED));
        records.add(record("2026-09-23-1205", "2026.09.23 (화)", "12:05", "메트포르민 500mg", ReminderRecord.STATUS_ON_TIME));
        records.add(record("2026-09-23-1900", "2026.09.23 (화)", "19:00", "와파린 2mg", ReminderRecord.STATUS_MISSED));
        records.add(record("2026-09-22-0800", "2026.09.22 (월)", "08:00", "아스피린 100mg", ReminderRecord.STATUS_ON_TIME));
        records.add(record("2026-09-22-1200", "2026.09.22 (월)", "12:00", "메트포르민 500mg", ReminderRecord.STATUS_ON_TIME));
        records.add(record("2026-09-22-1830", "2026.09.22 (월)", "18:30", "와파린 2mg", ReminderRecord.STATUS_ON_TIME));
    }

    public static MockMedicationReminderRepository getInstance() {
        if (instance == null) {
            instance = new MockMedicationReminderRepository();
        }
        return instance;
    }

    @Override
    public String periodRange() {
        return "2026.09.12 - 2026.09.25";
    }

    @Override
    public String alertTimeLabel(ReminderRecord record) {
        String[] parts = record.getClock().split(":");
        int hour24 = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        int hour12 = hour24 % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        String period = hour24 < 12 ? "오전" : "오후";
        return String.format(Locale.KOREA, "%s %d:%02d", period, hour12, minute);
    }

    @Override
    public String checklistTitle(ReminderRecord record) {
        int hour24 = Integer.parseInt(record.getClock().split(":")[0]);
        String period;
        if (hour24 < 12) {
            period = "아침";
        } else if (hour24 < 17) {
            period = "낮";
        } else {
            period = "저녁";
        }
        return "오늘 " + period + " 약물 리스트";
    }

    @Override
    public List<ReminderDay> days(String filter) {
        List<ReminderDay> days = new ArrayList<>();
        String currentDate = null;
        List<ReminderRecord> bucket = new ArrayList<>();
        for (ReminderRecord record : records) {
            if (!matches(record, filter)) {
                continue;
            }
            if (!record.getDateLabel().equals(currentDate)) {
                if (currentDate != null) {
                    days.add(new ReminderDay(currentDate, Collections.unmodifiableList(bucket)));
                }
                currentDate = record.getDateLabel();
                bucket = new ArrayList<>();
            }
            bucket.add(record);
        }
        if (currentDate != null) {
            days.add(new ReminderDay(currentDate, Collections.unmodifiableList(bucket)));
        }
        return Collections.unmodifiableList(days);
    }

    @Override
    public ReminderRecord find(String id) {
        if (id == null) {
            return null;
        }
        for (ReminderRecord record : records) {
            if (id.equals(record.getId())) {
                return record;
            }
        }
        return null;
    }

    @Override
    public boolean markUserCompleted(String id) {
        for (int index = 0; index < records.size(); index++) {
            ReminderRecord record = records.get(index);
            if (!record.getId().equals(id)) {
                continue;
            }
            if (!record.isCompleted()) {
                records.set(index, record.withStatus(ReminderRecord.STATUS_USER_COMPLETED));
            }
            return true;
        }
        return false;
    }

    private static ReminderRecord record(
            String id,
            String dateLabel,
            String clock,
            String medicationName,
            String status
    ) {
        return new ReminderRecord(id, dateLabel, clock, medicationName, status);
    }

    private static boolean matches(ReminderRecord record, String filter) {
        if (filter == null || ReminderRecord.FILTER_ALL.equals(filter)) {
            return true;
        }
        if (ReminderRecord.STATUS_ON_TIME.equals(filter)) {
            return record.isCompleted();
        }
        return filter.equals(record.getStatus());
    }
}
