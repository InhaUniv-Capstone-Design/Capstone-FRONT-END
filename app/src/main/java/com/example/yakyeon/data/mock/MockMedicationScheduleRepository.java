package com.example.yakyeon.data.mock;

import com.example.yakyeon.data.MedicationRepository;
import com.example.yakyeon.data.MedicationScheduleRepository;
import com.example.yakyeon.data.model.Medication;
import com.example.yakyeon.data.model.ScheduleDose;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MockMedicationScheduleRepository implements MedicationScheduleRepository {

    private static final Pattern CLOCK = Pattern.compile("(\\d{1,2}):(\\d{2})");
    private static MockMedicationScheduleRepository instance;

    private final MedicationRepository medications;

    public MockMedicationScheduleRepository(MedicationRepository medications) {
        this.medications = medications;
    }

    public static MockMedicationScheduleRepository getInstance() {
        if (instance == null) {
            instance = new MockMedicationScheduleRepository(MockMedicationRepository.getInstance());
        }
        return instance;
    }

    @Override
    public List<ScheduleDose> dosesOn(Calendar date) {
        List<ScheduleDose> doses = new ArrayList<>();
        Calendar day = dateOnly(date);
        Calendar today = dateOnly(Calendar.getInstance());
        int nowMinutes = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60
                + Calendar.getInstance().get(Calendar.MINUTE);
        for (Medication medication : medications.getActiveMedications()) {
            for (String clock : clocksOf(medication)) {
                int minutes = minutesOf(clock);
                doses.add(new ScheduleDose(
                        medication.getName(),
                        medication.formatDose(),
                        clock,
                        timeLabel(minutes),
                        statusFor(day, today, minutes, nowMinutes)
                ));
            }
        }
        Collections.sort(doses, Comparator.comparingInt(dose -> minutesOf(dose.getClock())));
        return Collections.unmodifiableList(doses);
    }

    private static String statusFor(Calendar day, Calendar today, int minutes, int nowMinutes) {
        int compare = dateKey(day) - dateKey(today);
        if (compare > 0) {
            return ScheduleDose.PLANNED;
        }
        if (compare < 0) {
            return (dateKey(day) + minutes) % 2 == 0 ? ScheduleDose.DONE : ScheduleDose.MISSED;
        }
        return minutes <= nowMinutes ? ScheduleDose.DONE : ScheduleDose.PLANNED;
    }

    private static List<String> clocksOf(Medication medication) {
        List<String> clocks = new ArrayList<>();
        Matcher matcher = CLOCK.matcher(medication.getDoseTimes() == null ? "" : medication.getDoseTimes());
        while (matcher.find()) {
            clocks.add(String.format(
                    java.util.Locale.KOREA,
                    "%02d:%02d",
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))
            ));
        }
        return clocks;
    }

    private static int minutesOf(String clock) {
        Matcher matcher = CLOCK.matcher(clock);
        if (!matcher.matches() && !matcher.find()) {
            return 0;
        }
        matcher.reset();
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
    }

    private static String timeLabel(int minutes) {
        int hour = minutes / 60;
        int minute = minutes % 60;
        String period = hour < 12 ? "오전" : "오후";
        int hour12 = hour % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        return String.format(java.util.Locale.KOREA, "%s %d:%02d", period, hour12, minute);
    }

    private static Calendar dateOnly(Calendar source) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(source.getTimeInMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private static int dateKey(Calendar calendar) {
        return calendar.get(Calendar.YEAR) * 10_000
                + (calendar.get(Calendar.MONTH) + 1) * 100
                + calendar.get(Calendar.DAY_OF_MONTH);
    }
}
