package com.example.yakyeon.data.mock;

import com.example.yakyeon.data.MedicationRepository;
import com.example.yakyeon.data.model.Medication;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MockMedicationRepository implements MedicationRepository {

    private static final Pattern PERIOD = Pattern.compile(
            "(\\d{4})\\.(\\d{2})\\.(\\d{2})\\s*~\\s*(\\d{4})\\.(\\d{2})\\.(\\d{2})"
    );

    private static MockMedicationRepository instance;

    private final List<Medication> medications = new ArrayList<>();

    public MockMedicationRepository() {
        medications.add(metformin());
        medications.add(atorvastatin());
        medications.add(amlodipine());
        medications.add(omeprazole());
    }

    public static MockMedicationRepository getInstance() {
        if (instance == null) {
            instance = new MockMedicationRepository();
        }
        return instance;
    }

    @Override
    public List<Medication> getMedications() {
        updateExpiredMedications();
        return Collections.unmodifiableList(new ArrayList<>(medications));
    }

    @Override
    public List<Medication> getActiveMedications() {
        updateExpiredMedications();
        return filter(Medication.Classification.ACTIVE);
    }

    @Override
    public List<Medication> getEndedMedications() {
        updateExpiredMedications();
        return filter(Medication.Classification.ENDED);
    }

    @Override
    public int countActive() {
        return getActiveMedications().size();
    }

    @Override
    public Medication findById(String id) {
        int index = indexOf(id);
        if (index < 0) {
            return null;
        }
        return medications.get(index);
    }

    @Override
    public boolean updateMedication(
            String id,
            String name,
            double doseAmount,
            String doseUnit,
            String timesPerDay,
            String doseTimes,
            String doseTiming,
            String duration
    ) {
        int index = indexOf(id);
        if (index < 0) {
            return false;
        }
        Medication current = medications.get(index);
        String nextName = text(name).isEmpty() ? current.getName() : text(name);
        String nextUnit = text(doseUnit);
        String nextTimesPerDay = text(timesPerDay);
        String nextDoseTimes = text(doseTimes);
        String nextTiming = text(doseTiming);
        String nextDuration = text(duration);
        String summary = summary(
                nextDoseTimes,
                nextTimesPerDay,
                nextTiming,
                Medication.formatAmount(doseAmount) + nextUnit
        );
        String nextFrequency = current.getClassification() == Medication.Classification.ENDED || summary.isEmpty()
                ? current.getFrequency()
                : summary;
        String nextUsage = summary.isEmpty() ? current.getUsage() : summary;
        medications.set(index, current.withSchedule(
                nextName,
                doseAmount,
                nextUnit,
                nextTimesPerDay,
                nextDoseTimes,
                nextTiming,
                nextDuration,
                nextFrequency,
                nextUsage
        ));
        return true;
    }

    @Override
    public boolean deleteMedication(String id) {
        int index = indexOf(id);
        if (index < 0) {
            return false;
        }
        medications.remove(index);
        return true;
    }

    @Override
    public boolean endMedication(String id) {
        int index = indexOf(id);
        if (index < 0) {
            return false;
        }
        Medication current = medications.get(index);
        if (current.getClassification() == Medication.Classification.ENDED) {
            return false;
        }
        medications.set(index, current.ended(endedLabel(Calendar.getInstance())));
        return true;
    }

    @Override
    public String addMedication(
            String name,
            String dosage,
            double doseAmount,
            String doseUnit,
            String timesPerDay,
            String doseTimes,
            String doseTiming,
            String duration
    ) {
        String nextUnit = text(doseUnit);
        String nextTimes = text(timesPerDay);
        String nextTimesOfDay = text(doseTimes);
        String nextTiming = text(doseTiming);
        String nextDuration = text(duration);
        String doseLabel = Medication.formatAmount(doseAmount) + nextUnit;
        String schedule = summary(nextTimesOfDay, nextTimes, nextTiming, doseLabel);
        String id = "ocr-" + System.currentTimeMillis() + "-" + medications.size();
        medications.add(new Medication(
                id,
                text(name),
                text(dosage),
                schedule,
                Medication.Classification.ACTIVE,
                "캡슐".equals(nextUnit) ? Medication.Form.PILL : Medication.Form.TABLET,
                "",
                "",
                schedule,
                "",
                "복용 중 이상 증상 발생 시 의료진과 상담하세요.",
                doseAmount,
                nextUnit,
                nextTimes,
                nextTimesOfDay,
                nextTiming,
                nextDuration
        ));
        return id;
    }

    @Override
    public void updateExpiredMedications() {
        Calendar today = Calendar.getInstance();
        for (int index = 0; index < medications.size(); index++) {
            Medication current = medications.get(index);
            if (current.getClassification() != Medication.Classification.ACTIVE) {
                continue;
            }
            Calendar end = endDate(current.getDuration());
            if (end != null && dateKey(today) > dateKey(end)) {
                medications.set(index, current.ended(endedLabel(end)));
            }
        }
    }

    private List<Medication> filter(Medication.Classification classification) {
        List<Medication> filtered = new ArrayList<>();
        for (Medication medication : medications) {
            if (medication.getClassification() == classification) {
                filtered.add(medication);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    private int indexOf(String id) {
        for (int index = 0; index < medications.size(); index++) {
            if (medications.get(index).getId().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private static String summary(String doseTimes, String timesPerDay, String doseTiming, String dose) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, doseTimes);
        appendPart(builder, timesPerDay);
        appendPart(builder, doseTiming);
        appendPart(builder, dose);
        return builder.toString();
    }

    private static void appendPart(StringBuilder builder, String part) {
        if (part == null || part.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" · ");
        }
        builder.append(part);
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String endedLabel(Calendar calendar) {
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format(Locale.KOREA, "복용 종료 · %d월 %d일", month, day);
    }

    private static Calendar endDate(String duration) {
        Matcher matcher = PERIOD.matcher(duration == null ? "" : duration.trim());
        if (!matcher.matches()) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(
                Integer.parseInt(matcher.group(4)),
                Integer.parseInt(matcher.group(5)) - 1,
                Integer.parseInt(matcher.group(6))
        );
        return calendar;
    }

    private static int dateKey(Calendar calendar) {
        return calendar.get(Calendar.YEAR) * 10_000
                + (calendar.get(Calendar.MONTH) + 1) * 100
                + calendar.get(Calendar.DAY_OF_MONTH);
    }

    private static Medication metformin() {
        return new Medication(
                "metformin",
                "메트포르민정 500mg",
                "500mg",
                "아침·저녁 식후 30분",
                Medication.Classification.ACTIVE,
                Medication.Form.TABLET,
                "혈당강하제",
                "당뇨병 치료제 (혈당강하제)",
                "1일 2회, 식후 30분 복용",
                "초기 복용 시 소화불량, 가벼운 설사 또는 복부 팽만감이 발생할 수 있습니다.",
                "복용 중 이상 증상 발생 시 의료진과 상담하세요.",
                1,
                "정",
                "2회",
                "08:00 · 19:00",
                "식후 30분",
                "30일"
        );
    }

    private static Medication atorvastatin() {
        return new Medication(
                "atorvastatin",
                "아토르바스타틴정 10mg",
                "10mg",
                "매일 저녁 1정",
                Medication.Classification.ACTIVE,
                Medication.Form.PILL,
                "",
                "",
                "매일 저녁 1정",
                "",
                "복용 중 이상 증상 발생 시 의료진과 상담하세요.",
                1,
                "정",
                "1회",
                "19:00",
                "저녁",
                ""
        );
    }

    private static Medication amlodipine() {
        return new Medication(
                "amlodipine",
                "암로디핀정 5mg",
                "5mg",
                "매일 아침 1정",
                Medication.Classification.ACTIVE,
                Medication.Form.TABLET,
                "",
                "",
                "매일 아침 1정",
                "",
                "복용 중 이상 증상 발생 시 의료진과 상담하세요.",
                1,
                "정",
                "1회",
                "08:00",
                "아침",
                ""
        );
    }

    private static Medication omeprazole() {
        return new Medication(
                "omeprazole",
                "오메프라졸캡슐 20mg",
                "20mg",
                "복용 종료 · 9월 18일",
                Medication.Classification.ENDED,
                Medication.Form.PILL,
                "",
                "",
                "",
                "",
                "복용 중 이상 증상 발생 시 의료진과 상담하세요.",
                1,
                "캡슐",
                "1회",
                "",
                "",
                ""
        );
    }
}
