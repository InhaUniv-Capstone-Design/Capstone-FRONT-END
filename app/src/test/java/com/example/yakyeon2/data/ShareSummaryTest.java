package com.example.yakyeon2.data;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ShareSummaryTest {

    @Test
    public void summaryUsesPrescriptionFieldsAndDropsTimesAndAdherence() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        ClinicalShareOptions options = new ClinicalShareOptions();
        options.setIncludeAdherence(true);
        options.setIncludeRecords(true);

        ShareSummary summary = ShareSummary.build(
                store,
                options,
                "김약연",
                ScenarioPrescriptions.REFERENCE_DAY
        );

        assertEquals("김약연님의 복약 요약기록입니다", summary.patientLine);
        assertEquals(ShareSummary.PERIOD_LABEL, "복용기간");
        assertEquals("30일", summary.periodDaysText);
        ShareSummary.MedicineLine first = summary.taking.get(0);
        assertEquals("약1", first.name);
        assertEquals("처방 한의원", first.prescription);
        assertEquals("2026.10.13 - 2026.10.30", first.range);
        assertEquals("", first.duration);
        assertTrue(summary.finished.isEmpty());

        String text = summary.plainText();
        assertTrue(text.startsWith("김약연님의 복약 요약기록입니다"));
        assertTrue(text.contains("복용기간"));
        assertFalse(text.contains("공유기간"));
        assertFalse(text.contains("공유 기간"));
        assertFalse(text.contains("복용률"));
        assertFalse(text.contains("%"));
        assertFalse(text.contains("아침"));
        assertFalse(text.contains("점심"));
        assertFalse(text.contains("저녁"));
        assertFalse(text.contains("취침"));
        assertFalse(text.contains("식후"));
        assertFalse(text.contains("식전"));
        assertFalse(text.contains(":"));
        assertFalse(text.contains("1회"));
        assertFalse(text.contains("1일"));
        assertFalse(text.contains("총 "));
        assertFalse(text.contains("기준일"));
        assertTrue(text.indexOf("출처") > text.lastIndexOf("약6"));
        assertEquals(ShareSummary.SOURCE_NONE, summary.sourceValue);
        assertNull(summary.referenceValue);
    }

    @Test
    public void blankNameOmitsTheHonorificAndRecordToggleDoesNotChangeThePage() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        ClinicalShareOptions options = new ClinicalShareOptions();
        LocalDate today = ScenarioPrescriptions.REFERENCE_DAY;
        String withRecords = ShareSummary.build(store, options, "  ", today).plainText();
        options.setIncludeRecords(false);
        options.setIncludeAdherence(true);
        ShareSummary again = ShareSummary.build(store, options, null, today);
        assertEquals("복약 요약기록입니다", again.patientLine);
        assertEquals(withRecords, again.plainText());
    }

    @Test
    public void endedMedicineMovesToFinishedAndKeepsDoseCounts() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        SavedMedicine medicine = ScenarioPrescriptions.medicine(store, "약6");
        MedicineDraft draft = new MedicineDraft();
        draft.setName(medicine.getName());
        draft.setAmount(medicine.getAmount());
        draft.rounds().addAll(medicine.rounds());
        draft.timings().addAll(medicine.timings());
        draft.setStart(LocalDate.of(2026, 9, 20));
        draft.setEnd(LocalDate.of(2026, 10, 1));
        medicine.apply(draft);

        ShareSummary summary = ShareSummary.build(
                store,
                new ClinicalShareOptions(),
                "김약연",
                ScenarioPrescriptions.REFERENCE_DAY
        );

        assertEquals(5, summary.taking.size());
        assertEquals(1, summary.finished.size());
        ShareSummary.MedicineLine finished = summary.finished.get(0);
        assertEquals("약6", finished.name);
        assertFalse(finished.taking);
        assertEquals("처방 내과", finished.prescription);
        assertEquals("2026.09.20 - 2026.10.01", finished.range);
        assertEquals("", finished.duration);
    }

    @Test
    public void devPairStaysLabeledAndSourceMovesBelowTheMedicines() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        store.setDevGraphRelationScenario(true);
        ClinicalShareOptions options = new ClinicalShareOptions();

        ShareSummary summary = ShareSummary.build(store, options, "김약연", ScenarioPrescriptions.REFERENCE_DAY);

        assertEquals(1, summary.interactions.size());
        ShareSummary.Interaction pair = summary.interactions.get(0);
        assertEquals("약1", pair.leftName);
        assertEquals("약2", pair.rightName);
        assertEquals(ShareSummary.DEV_NOTE, pair.note);
        assertEquals("2026.10.13 - 2026.10.30", pair.overlap);
        assertEquals(ShareSummary.SOURCE_DEV, summary.sourceValue);
        String text = summary.plainText();
        assertFalse(text.contains("함께 복용 시 주의"));
        assertFalse(text.contains("2026년 9월"));
        assertTrue(text.indexOf(ShareSummary.LABEL_SOURCE) > text.lastIndexOf("약6"));
    }

    @Test
    public void hidingSpanAndDurDropsDatesSourceAndTotalDays() {
        MedicationStore store = ScenarioPrescriptions.knownStore();
        store.setDevGraphRelationScenario(true);
        ClinicalShareOptions options = new ClinicalShareOptions();
        options.setIncludeSpan(false);
        options.setIncludeDur(false);

        ShareSummary summary = ShareSummary.build(store, options, "김약연", ScenarioPrescriptions.REFERENCE_DAY);

        assertFalse(summary.includeDur);
        assertTrue(summary.interactions.isEmpty());
        assertNull(summary.sourceValue);
        ShareSummary.MedicineLine first = summary.taking.get(0);
        assertEquals("", first.range);
        assertEquals("", first.duration);
        assertEquals("처방 한의원", first.prescription);
        String text = summary.plainText();
        assertFalse(text.contains("병용금기"));
        assertFalse(text.contains("출처"));
        assertFalse(text.contains("2026.10.13"));
        assertTrue(text.contains(ShareSummary.DISCLAIMER));
    }
}
