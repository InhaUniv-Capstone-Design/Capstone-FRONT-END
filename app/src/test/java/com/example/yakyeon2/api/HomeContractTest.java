package com.example.yakyeon2.api;

import com.example.yakyeon2.data.DoseAmount;
import com.example.yakyeon2.data.DoseRound;
import com.example.yakyeon2.data.DoseTiming;
import com.example.yakyeon2.data.EnvelopeDraft;
import com.example.yakyeon2.data.MedicineDraft;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;

public class HomeContractTest {

    @Test
    public void homeSummaryKeepsPendingNextDoseAndNullNextDose() throws Exception {
        HomeContracts.HomeSummary summary = HomeContracts.readHomeSummary(new JSONObject(
                "{"
                        + "\"date\":\"2026-10-27\","
                        + "\"total\":3,\"done\":1,\"normal\":1,\"delayed\":0,\"missed\":0,\"pending\":2,"
                        + "\"slots\":["
                        + "{\"timing_slot\":\"MORNING\",\"state\":\"DONE\"},"
                        + "{\"timing_slot\":\"LUNCH\",\"state\":\"PENDING\"},"
                        + "{\"timing_slot\":\"DINNER\",\"state\":\"NONE\"}"
                        + "],"
                        + "\"next_dose\":{"
                        + "\"scheduled_dose_id\":8,"
                        + "\"hospital_name\":null,"
                        + "\"timing_slot\":\"LUNCH\","
                        + "\"scheduled_at\":\"2026-10-27T13:00:00+09:00\","
                        + "\"status\":\"PENDING\","
                        + "\"medications\":[{\"medication_id\":3,\"drug_name\":\"약\"}]"
                        + "}"
                        + "}"
        ));
        Assert.assertEquals(1L, summary.done);
        Assert.assertEquals(3L, summary.total);
        Assert.assertEquals(HomeContracts.STATE_NONE, HomeContracts.slotState(summary.slots, HomeContracts.SLOT_DINNER));
        Assert.assertFalse(HomeContracts.containsSlot(summary.slots, HomeContracts.SLOT_BEDTIME));
        Assert.assertNull(summary.nextDose.hospitalName);
        Assert.assertEquals(HomeContracts.STATUS_PENDING, summary.nextDose.status);

        HomeContracts.HomeSummary empty = HomeContracts.readHomeSummary(new JSONObject(
                "{"
                        + "\"date\":\"2026-10-27\","
                        + "\"total\":0,\"done\":0,\"normal\":0,\"delayed\":0,\"missed\":0,\"pending\":0,"
                        + "\"slots\":["
                        + "{\"timing_slot\":\"MORNING\",\"state\":\"NONE\"},"
                        + "{\"timing_slot\":\"LUNCH\",\"state\":\"NONE\"},"
                        + "{\"timing_slot\":\"DINNER\",\"state\":\"NONE\"}"
                        + "],"
                        + "\"next_dose\":null"
                        + "}"
        ));
        Assert.assertNull(empty.nextDose);
    }

    @Test
    public void unknownSlotStateIsNotTreatedAsSuccess() throws Exception {
        try {
            HomeContracts.readHomeSummary(new JSONObject(
                    "{"
                            + "\"date\":\"2026-10-27\","
                            + "\"total\":1,\"done\":0,\"normal\":0,\"delayed\":0,\"missed\":0,\"pending\":1,"
                            + "\"slots\":[{\"timing_slot\":\"MORNING\",\"state\":\"ACTIVE\"}],"
                            + "\"next_dose\":null"
                            + "}"
            ));
            Assert.fail();
        } catch (ApiException exception) {
            Assert.assertTrue(exception.failure() == com.example.yakyeon2.api.ApiFailure.Kind.RESPONSE);
        }
    }

    @Test
    public void doseAllowsNullTakenAtAndSortsPending() throws Exception {
        JSONArray array = new JSONArray("[{"
                + "\"scheduled_dose_id\":2,\"prescription_id\":4,\"hospital_name\":null,"
                + "\"timing_slot\":\"DINNER\",\"scheduled_at\":\"2026-10-27T19:00:00+09:00\","
                + "\"normal_until\":\"2026-10-27T19:30:00+09:00\",\"close_at\":\"2026-10-27T22:00:00+09:00\","
                + "\"status\":\"PENDING\",\"taken_at\":null,\"decision_source\":null,\"corrected\":false,"
                + "\"medications\":[{\"medication_id\":1,\"drug_name\":\"저녁약\",\"dose_per_intake\":0.5}]"
                + "},{"
                + "\"scheduled_dose_id\":1,\"prescription_id\":4,\"hospital_name\":\"병원\","
                + "\"timing_slot\":\"MORNING\",\"scheduled_at\":\"2026-10-27T08:00:00+09:00\","
                + "\"normal_until\":\"2026-10-27T08:30:00+09:00\",\"close_at\":\"2026-10-27T11:00:00+09:00\","
                + "\"status\":\"NORMAL\",\"taken_at\":\"2026-10-27T08:10:00+09:00\","
                + "\"decision_source\":\"MANUAL\",\"corrected\":true,"
                + "\"medications\":[{\"medication_id\":1,\"drug_name\":\"아침약\",\"dose_per_intake\":1}]"
                + "}]");
        java.util.List<HomeContracts.Dose> doses = HomeContracts.readDoses(array);
        Assert.assertNull(doses.get(0).takenAt);
        Assert.assertNull(doses.get(0).decisionSource);
        Assert.assertEquals("0.5", doses.get(0).medications.get(0).dosePerIntake);
        Assert.assertTrue(doses.get(1).corrected);
        java.util.List<HomeContracts.Dose> pending = HomeContracts.pendingSorted(doses);
        Assert.assertEquals(1, pending.size());
        Assert.assertEquals(2L, pending.get(0).scheduledDoseId);
    }

    @Test
    public void medicationKeepsHalfDoseAndNullableFields() throws Exception {
        HomeContracts.Medication medication = HomeContracts.readMedication(new JSONObject(
                "{"
                        + "\"medication_id\":9,\"prescription_id\":4,\"hospital_name\":null,\"drug_id\":null,"
                        + "\"drug_name\":\"직접입력\",\"manufacturer\":null,\"match_status\":\"UNMATCHED\","
                        + "\"dose_per_intake\":0.5,\"is_prn\":false,\"times_per_day\":1,"
                        + "\"timing_slots\":[\"MORNING\"],\"meal_relation\":null,\"meal_offset_min\":null,"
                        + "\"total_days\":7,\"start_date\":\"2026-10-27\",\"end_date\":\"2026-11-02\","
                        + "\"ended_on\":null,\"status\":\"ACTIVE\""
                        + "}"
        ));
        Assert.assertEquals("0.5", medication.dosePerIntake);
        Assert.assertNull(medication.drugId);
        Assert.assertNull(medication.hospitalName);
        Assert.assertNull(medication.endedOn);
        Assert.assertEquals(HomeContracts.STATUS_ACTIVE, medication.status);
    }

    @Test
    public void emptyRecognizedListIsNotAnError() throws Exception {
        HomeContracts.OcrDraft draft = HomeContracts.readOcr(new JSONObject(
                "{\"hospital_name\":null,\"pharmacy_name\":null,\"prescribed_date\":null,\"recognized\":[]}"
        ));
        Assert.assertTrue(draft.recognized.isEmpty());
        Assert.assertNull(draft.hospitalName);
    }

    @Test
    public void pathsAndHistoryLimit() throws Exception {
        Assert.assertEquals(
                "/patients/7/medications?status=ACTIVE",
                HomeContracts.medicationsPath(7L, HomeContracts.STATUS_ACTIVE)
        );
        Assert.assertEquals(
                "/patients/7/doses?date=2026-10-27",
                HomeContracts.dosesPath(7L, LocalDate.parse("2026-10-27"))
        );
        Assert.assertEquals(
                "/patients/7/dose-history",
                HomeContracts.historyPath(7L, null, null, null)
        );
        Assert.assertEquals(
                "/patients/7/dose-history?status=NORMAL",
                HomeContracts.historyPath(7L, null, null, HomeContracts.STATUS_NORMAL)
        );
        Assert.assertEquals(
                "/patients/7/dose-history?from=2026-10-01&to=2026-10-14&status=MISSED",
                HomeContracts.historyPath(
                        7L,
                        LocalDate.parse("2026-10-01"),
                        LocalDate.parse("2026-10-14"),
                        HomeContracts.STATUS_MISSED
                )
        );
        Assert.assertEquals(90, (int) HomeContracts.historySpanDays(
                LocalDate.parse("2026-08-03"),
                LocalDate.parse("2026-10-31")
        ));
        Assert.assertNull(HomeContracts.historySpanDays(
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-10-31")
        ));
        Assert.assertTrue(HomeContracts.autocompletePath("메트", 10).startsWith("/medications/autocomplete?q="));
        Assert.assertEquals("/prescriptions/ocr?patient_id=7", HomeContracts.ocrPath(7L));
    }

    @Test
    public void bedtimeIsNotSentAndHalfDoseIsNumeric() throws Exception {
        EnvelopeDraft draft = draft();
        draft.medicines().get(0).rounds().add(DoseRound.BEDTIME);
        HomeRegistration.Prepared blocked = HomeRegistration.prepare(draft);
        Assert.assertEquals(HomeRegistration.Block.BEDTIME, blocked.block);
        Assert.assertNull(blocked.prescription);

        EnvelopeDraft ready = draft();
        MedicineDraft medicine = ready.medicines().get(0);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.rounds().add(DoseRound.EVENING);
        medicine.setAmount(DoseAmount.TABLET_HALF);
        medicine.timings().add(DoseTiming.AFTER_MEAL_30);
        medicine.setConfirmed(true);
        ready.alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        ready.alarmMinutes().put(DoseRound.EVENING, 19 * 60);
        ready.setSlotIndex(0);
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(ready);
        Assert.assertEquals(HomeRegistration.Block.NONE, prepared.block);
        Assert.assertEquals(1, prepared.compartmentNo.intValue());
        JSONArray medications = prepared.prescription.getJSONArray("medications");
        JSONArray slots = medications.getJSONObject(0).getJSONArray("timing_slots");
        Assert.assertEquals("MORNING", slots.getString(0));
        Assert.assertEquals("DINNER", slots.getString(1));
        Assert.assertEquals(0.5d, medications.getJSONObject(0).getDouble("dose_per_intake"), 0.001d);
        Assert.assertEquals("MANUAL", prepared.prescription.getString("source"));
        Assert.assertEquals("08:00", prepared.schedule.getJSONArray("settings").getJSONObject(0).getString("dose_time"));
        Assert.assertEquals("19:00", prepared.schedule.getJSONArray("settings").getJSONObject(1).getString("dose_time"));
        try {
            HomeContracts.scheduleBody(
                    java.util.Collections.singletonList(HomeContracts.SLOT_BEDTIME),
                    java.util.Collections.singletonList("22:00")
            );
            Assert.fail();
        } catch (ApiException exception) {
            Assert.assertFalse(exception.network());
        }
    }

    @Test
    public void takenBodyAndTermsVersion() throws Exception {
        Assert.assertNull(HomeContracts.takenBody(null));
        Assert.assertEquals(
                "2026-10-27T08:10:00+09:00",
                HomeContracts.takenBody("2026-10-27T08:10:00+09:00").getString("taken_at")
        );
        JSONArray terms = new JSONArray("["
                + "{\"terms_type\":\"TOS\",\"terms_version_id\":1,\"version\":\"1\",\"content\":\"a\",\"is_required\":true},"
                + "{\"terms_type\":\"MEDICATION\",\"terms_version_id\":9,\"version\":\"1\",\"content\":\"b\",\"is_required\":true}"
                + "]");
        Assert.assertEquals(Long.valueOf(9L), HomeContracts.medicationTermsVersion(terms));
        Assert.assertNull(HomeContracts.medicationTermsVersion(new JSONArray("[]")));
        Assert.assertEquals("2026-10-27T08:10:00+09:00", HomeContracts.takenAt(
                java.time.OffsetDateTime.parse("2026-10-27T08:10:00+09:00").toInstant().toEpochMilli()
        ));
    }

    @Test
    public void errorCodesHaveFallbacks() {
        Assert.assertNotNull(ApiMessages.fallback(ApiCodes.DOSE_NOT_DECIDED));
        Assert.assertNotNull(ApiMessages.fallback(ApiCodes.DOSE_EXPIRED));
        Assert.assertNotNull(ApiMessages.fallback(ApiCodes.OCR_UNAVAILABLE));
        Assert.assertNotNull(ApiMessages.fallback(ApiCodes.CONSENT_REQUIRED));
        Assert.assertNotNull(ApiMessages.fallback(ApiCodes.INVALID_SCHEDULE));
        Assert.assertNotNull(ApiMessages.fallback(ApiCodes.SHARE_NOT_GRANTED));
    }

    private static EnvelopeDraft draft() {
        EnvelopeDraft draft = new EnvelopeDraft();
        draft.setHospitalName("인하병원");
        MedicineDraft medicine = new MedicineDraft();
        medicine.setName("메트포르민");
        medicine.setStart(LocalDate.parse("2026-10-27"));
        medicine.setEnd(LocalDate.parse("2026-11-02"));
        draft.medicines().add(medicine);
        return draft;
    }
}
