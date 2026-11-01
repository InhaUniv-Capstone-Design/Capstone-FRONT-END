package com.example.yakyeon2.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clinician summary for the share PNG.
 * Each medicine line keeps the name and prescribing institution. The dose period is start - end.
 * Adherence, dose counts, and the DUR reference-date note are omitted.
 */
public final class ShareSummary {

    public static final String PERIOD_LABEL = "복용기간";
    public static final String TITLE_DUR = "병용금기 등재 정보";
    public static final String TITLE_TAKING = "복용 중인 약물";
    public static final String TITLE_FINISHED = "복약한 약물";
    public static final String EMPTY_TAKING = "현재 복용 중인 약이 없습니다.";
    public static final String EMPTY_DUR = "확인된 병용금기 등재 정보가 없습니다.";
    public static final String LABEL_OVERLAP = "함께 복용한 기간";
    public static final String LABEL_SOURCE = "출처";
    public static final String LABEL_REFERENCE = "기준일";
    public static final String LABEL_START = "시작";
    public static final String LABEL_END = "종료";
    public static final String DEV_NOTE = "개발용 · 등재 결과 아님";
    public static final String SOURCE_NONE = "확인된 식품의약품안전처 DUR 등재가 없습니다";
    public static final String SOURCE_DEV = "개발용 관계 · 식품의약품안전처 DUR 등재 결과가 아닙니다";
    public static final String SOURCE_REGISTRY = "식품의약품안전처 DUR";
    public static final String DISCLAIMER =
            "앱에 기록된 복약 정보입니다. 등재 정보는 진단이 아니며, 처방 변경 전 의료진 확인이 필요합니다.";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public final String patientLine;
    public final int periodDays;
    public final String periodDaysText;
    public final String periodStartText;
    public final String periodEndText;
    public final boolean includeDur;
    public final List<Interaction> interactions;
    public final List<MedicineLine> taking;
    public final List<MedicineLine> finished;
    public final String sourceValue;
    public final String referenceValue;
    public final String disclaimer;

    private ShareSummary(
            String patientLine,
            int periodDays,
            String periodDaysText,
            String periodStartText,
            String periodEndText,
            boolean includeDur,
            List<Interaction> interactions,
            List<MedicineLine> taking,
            List<MedicineLine> finished,
            String sourceValue,
            String referenceValue,
            String disclaimer
    ) {
        this.patientLine = patientLine;
        this.periodDays = periodDays;
        this.periodDaysText = periodDaysText;
        this.periodStartText = periodStartText;
        this.periodEndText = periodEndText;
        this.includeDur = includeDur;
        this.interactions = interactions;
        this.taking = taking;
        this.finished = finished;
        this.sourceValue = sourceValue;
        this.referenceValue = referenceValue;
        this.disclaimer = disclaimer;
    }

    public static ShareSummary build(
            MedicationStore store,
            ClinicalShareOptions options,
            String patientName,
            LocalDate today
    ) {
        ClinicalShareOptions share = options == null ? new ClinicalShareOptions() : options;
        LocalDate anchor = today == null ? LocalDate.now() : today;
        LocalDate end = share.resolveEnd(anchor);
        LocalDate start = share.resolveStart(anchor);
        boolean includeSpan = share.isIncludeSpan();
        Map<String, MedRef> meds = index(store);
        List<MedicineLine> taking = new ArrayList<>();
        List<MedicineLine> finished = new ArrayList<>();
        if (store != null) {
            for (SavedEnvelope envelope : store.envelopes()) {
                if (store.isSampleEnvelope(envelope.getId())) {
                    continue;
                }
                for (SavedMedicine medicine : envelope.medicines()) {
                    boolean active = takingNow(envelope, medicine, end);
                    if (!active && !overlaps(medicine, start, end)) {
                        continue;
                    }
                    MedicineLine line = line(envelope, medicine, includeSpan, active);
                    if (active) {
                        if (share.isIncludeCurrent()) {
                            taking.add(line);
                        }
                    } else if (share.isIncludeFinished()) {
                        finished.add(line);
                    }
                }
            }
        }
        List<Interaction> interactions = new ArrayList<>();
        boolean anyReal = false;
        boolean anyDev = false;
        if (share.isIncludeDur() && store != null) {
            for (DrugRelations.Pair pair : DrugRelations.confirmedPairs(store)) {
                if (pair.isDevScenario()) {
                    anyDev = true;
                } else {
                    anyReal = true;
                }
                interactions.add(interaction(pair, meds.get(pair.getLeftMedicineId()), meds.get(pair.getRightMedicineId()), includeSpan));
            }
        }
        String source = null;
        if (share.isIncludeDur()) {
            if (anyReal) {
                source = SOURCE_REGISTRY;
            } else if (anyDev) {
                source = SOURCE_DEV;
            } else {
                source = SOURCE_NONE;
            }
        }
        return new ShareSummary(
                patientLine(patientName),
                share.getPeriodDays(),
                share.getPeriodDays() + "일",
                DAY.format(start),
                DAY.format(end),
                share.isIncludeDur(),
                Collections.unmodifiableList(interactions),
                Collections.unmodifiableList(taking),
                Collections.unmodifiableList(finished),
                source,
                null,
                DISCLAIMER
        );
    }

    public String plainText() {
        StringBuilder text = new StringBuilder();
        text.append(patientLine).append('\n');
        text.append(PERIOD_LABEL).append('\n');
        text.append(periodDaysText).append('\n');
        text.append(periodStartText).append('\n');
        text.append(LABEL_START).append('\n');
        text.append(periodEndText).append('\n');
        text.append(LABEL_END).append('\n');
        if (includeDur) {
            text.append(TITLE_DUR).append('\n');
            text.append(interactions.size()).append("건").append('\n');
            if (interactions.isEmpty()) {
                text.append(EMPTY_DUR).append('\n');
            }
            for (Interaction item : interactions) {
                text.append(item.leftName).append('\n');
                text.append(item.rightName).append('\n');
                if (!item.note.isEmpty()) {
                    text.append(item.note).append('\n');
                }
                if (!item.overlap.isEmpty()) {
                    text.append(LABEL_OVERLAP).append('\n');
                    text.append(item.overlap).append('\n');
                }
            }
        }
        text.append(TITLE_TAKING).append('\n');
        if (taking.isEmpty()) {
            text.append(EMPTY_TAKING).append('\n');
        }
        for (MedicineLine line : taking) {
            appendMedicine(text, line);
        }
        if (!finished.isEmpty()) {
            text.append(TITLE_FINISHED).append('\n');
            for (MedicineLine line : finished) {
                appendMedicine(text, line);
            }
        }
        if (sourceValue != null) {
            text.append(LABEL_SOURCE).append('\n');
            text.append(sourceValue).append('\n');
        }
        text.append(disclaimer).append('\n');
        return text.toString();
    }

    public static String patientLine(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "복약 요약기록입니다";
        }
        return name.trim() + "님의 복약 요약기록입니다";
    }

    private static void appendMedicine(StringBuilder text, MedicineLine line) {
        text.append(line.name).append('\n');
        if (!line.prescription.isEmpty()) {
            text.append(line.prescription).append('\n');
        }
        if (!line.range.isEmpty()) {
            text.append(line.range).append('\n');
        }
        if (!line.duration.isEmpty()) {
            text.append(line.duration).append('\n');
        }
    }

    private static Map<String, MedRef> index(MedicationStore store) {
        Map<String, MedRef> meds = new LinkedHashMap<>();
        if (store == null) {
            return meds;
        }
        for (SavedEnvelope envelope : store.envelopes()) {
            if (store.isSampleEnvelope(envelope.getId())) {
                continue;
            }
            for (SavedMedicine medicine : envelope.medicines()) {
                meds.put(medicine.getId(), new MedRef(medicine));
            }
        }
        return meds;
    }

    private static boolean takingNow(SavedEnvelope envelope, SavedMedicine medicine, LocalDate today) {
        if (envelope.isStopped()) {
            return false;
        }
        LocalDate end = medicine.getEnd();
        return end == null || !end.isBefore(today);
    }

    private static boolean overlaps(SavedMedicine medicine, LocalDate windowStart, LocalDate windowEnd) {
        LocalDate from = medicine.getStart() != null ? medicine.getStart() : windowStart;
        LocalDate to = medicine.getEnd() != null ? medicine.getEnd() : windowEnd;
        return !from.isAfter(windowEnd) && !to.isBefore(windowStart);
    }

    private static MedicineLine line(
            SavedEnvelope envelope,
            SavedMedicine medicine,
            boolean includeSpan,
            boolean taking
    ) {
        LocalDate start = medicine.getStart();
        LocalDate end = medicine.getEnd();
        String range = "";
        if (includeSpan && start != null && end != null && !end.isBefore(start)) {
            range = period(start, end);
        } else if (includeSpan && start != null) {
            range = DAY.format(start);
        } else if (includeSpan && end != null) {
            range = DAY.format(end);
        }
        return new MedicineLine(
                displayName(medicine.getName()),
                prescribingPlace(envelope.getHospitalName()),
                range,
                "",
                taking
        );
    }

    private static String prescribingPlace(String hospital) {
        if (hospital == null || hospital.trim().isEmpty()) {
            return "";
        }
        return "처방 " + hospital.trim();
    }

    private static String period(LocalDate start, LocalDate end) {
        return DAY.format(start) + " - " + DAY.format(end);
    }

    private static Interaction interaction(
            DrugRelations.Pair pair,
            MedRef left,
            MedRef right,
            boolean includeSpan
    ) {
        String overlap = "";
        if (includeSpan && left != null && right != null) {
            overlap = overlap(left.medicine, right.medicine);
        }
        return new Interaction(
                pair.getLeftName(),
                pair.getRightName(),
                pair.isDevScenario() ? DEV_NOTE : "",
                overlap
        );
    }

    private static String overlap(SavedMedicine left, SavedMedicine right) {
        LocalDate start = later(left.getStart(), right.getStart());
        if (start == null) {
            return "";
        }
        LocalDate end = earlier(left.getEnd(), right.getEnd());
        if (end == null || start.isAfter(end)) {
            return "";
        }
        return period(start, end);
    }

    private static LocalDate later(LocalDate left, LocalDate right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private static LocalDate earlier(LocalDate left, LocalDate right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isBefore(right) ? left : right;
    }

    private static String displayName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "이름 없는 약";
        }
        return name.trim();
    }

    public static final class MedicineLine {
        public final String name;
        public final String prescription;
        public final String range;
        public final String duration;
        public final boolean taking;

        MedicineLine(String name, String prescription, String range, String duration, boolean taking) {
            this.name = name;
            this.prescription = prescription;
            this.range = range;
            this.duration = duration;
            this.taking = taking;
        }
    }

    public static final class Interaction {
        public final String leftName;
        public final String rightName;
        public final String note;
        public final String overlap;

        Interaction(String leftName, String rightName, String note, String overlap) {
            this.leftName = leftName == null ? "" : leftName;
            this.rightName = rightName == null ? "" : rightName;
            this.note = note == null ? "" : note;
            this.overlap = overlap == null ? "" : overlap;
        }
    }

    private static final class MedRef {
        final SavedMedicine medicine;

        MedRef(SavedMedicine medicine) {
            this.medicine = medicine;
        }
    }
}
