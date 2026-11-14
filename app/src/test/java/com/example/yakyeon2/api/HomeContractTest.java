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

        HomeContracts.HomeSummary four = HomeContracts.readHomeSummary(new JSONObject(
                "{"
                        + "\"date\":\"2026-10-27\","
                        + "\"total\":4,\"done\":1,\"normal\":1,\"delayed\":0,\"missed\":0,\"pending\":2,"
                        + "\"slots\":["
                        + "{\"timing_slot\":\"MORNING\",\"state\":\"DONE\"},"
                        + "{\"timing_slot\":\"LUNCH\",\"state\":\"PENDING\"},"
                        + "{\"timing_slot\":\"DINNER\",\"state\":\"NONE\"},"
                        + "{\"timing_slot\":\"BEDTIME\",\"state\":\"NONE\"}"
                        + "],"
                        + "\"next_dose\":{"
                        + "\"scheduled_dose_id\":9,"
                        + "\"hospital_name\":\"병원\","
                        + "\"timing_slot\":\"BEDTIME\","
                        + "\"scheduled_at\":\"2026-10-27T22:00:00+09:00\","
                        + "\"status\":\"PENDING\","
                        + "\"medications\":[{\"medication_id\":3,\"drug_name\":\"취침약\"}]"
                        + "}"
                        + "}"
        ));
        Assert.assertEquals(4L, four.total);
        Assert.assertEquals(1L, four.done);
        Assert.assertEquals(2L, four.pending);
        Assert.assertEquals(HomeContracts.STATE_NONE, HomeContracts.slotState(four.slots, HomeContracts.SLOT_BEDTIME));
        Assert.assertEquals(HomeContracts.SLOT_BEDTIME, four.nextDose.timingSlot);
    }

    @Test
    public void homeSummaryReadsServerClockUncheckedUpcomingAndSlotProgress() throws Exception {
        HomeContracts.HomeSummary summary = HomeContracts.readHomeSummary(new JSONObject(
                "{"
                        + "\"date\":\"2026-11-09\","
                        + "\"server_time\":\"2026-11-09T08:48:00+09:00\","
                        + "\"total\":4,\"done\":1,\"normal\":1,\"delayed\":0,\"missed\":1,\"pending\":2,"
                        + "\"all_done\":false,"
                        + "\"slots\":["
                        + "{\"timing_slot\":\"MORNING\",\"state\":\"DONE\"},"
                        + "{\"timing_slot\":\"LUNCH\",\"state\":\"PENDING\"},"
                        + "{\"timing_slot\":\"DINNER\",\"state\":\"NONE\"},"
                        + "{\"timing_slot\":\"BEDTIME\",\"state\":\"NONE\"}"
                        + "],"
                        + "\"unchecked_doses\":["
                        + "{\"scheduled_dose_id\":12,\"hospital_name\":\"서울병원\",\"timing_slot\":\"LUNCH\","
                        + "\"scheduled_at\":\"2026-11-09T12:30:00+09:00\","
                        + "\"medications\":[{\"medication_id\":2,\"drug_name\":\"당귀\"}]},"
                        + "{\"scheduled_dose_id\":11,\"hospital_name\":\"한의원\",\"timing_slot\":\"LUNCH\","
                        + "\"scheduled_at\":\"2026-11-09T12:10:00+09:00\","
                        + "\"medications\":[{\"medication_id\":1,\"drug_name\":\"감초\"}]}"
                        + "],"
                        + "\"upcoming_doses\":["
                        + "{\"scheduled_dose_id\":14,\"hospital_name\":\"한의원\",\"timing_slot\":\"BEDTIME\","
                        + "\"scheduled_at\":\"2026-11-09T22:00:00+09:00\","
                        + "\"medications\":[{\"medication_id\":4,\"drug_name\":\"취침약\"}]},"
                        + "{\"scheduled_dose_id\":13,\"hospital_name\":\"서울병원\",\"timing_slot\":\"DINNER\","
                        + "\"scheduled_at\":\"2026-11-09T18:00:00+09:00\","
                        + "\"medications\":[{\"medication_id\":3,\"drug_name\":\"저녁약\"}]}"
                        + "],"
                        + "\"next_dose\":null"
                        + "}"
        ));
        Assert.assertTrue(summary.serverBoard);
        Assert.assertEquals(Boolean.FALSE, summary.allDone);
        Assert.assertEquals("2026-11-09T08:48:00+09:00", summary.serverTime);
        HomeContracts.SlotProgress progress = HomeContracts.slotProgress(summary.slots);
        Assert.assertEquals(1, progress.done);
        Assert.assertEquals(2, progress.scheduled);
        Assert.assertEquals(2, summary.uncheckedDoses.size());
        Assert.assertEquals(11L, summary.uncheckedDoses.get(0).scheduledDoseId);
        Assert.assertEquals("한의원", summary.uncheckedDoses.get(0).hospitalName);
        Assert.assertEquals(12L, summary.uncheckedDoses.get(1).scheduledDoseId);
        Assert.assertEquals("서울병원", summary.uncheckedDoses.get(1).hospitalName);
        Assert.assertEquals(13L, summary.upcomingDoses.get(0).scheduledDoseId);
        Assert.assertEquals(HomeContracts.SLOT_BEDTIME, summary.upcomingDoses.get(1).timingSlot);
        Assert.assertEquals(
                Long.valueOf(3L * 60L + 22L),
                HomeContracts.minutesUntil("2026-11-09T12:10:00+09:00", summary.serverTime)
        );

        HomeContracts.HomeSummary finished = HomeContracts.readHomeSummary(new JSONObject(
                "{"
                        + "\"date\":\"2026-11-09\","
                        + "\"total\":2,\"done\":1,\"normal\":1,\"delayed\":0,\"missed\":1,\"pending\":0,"
                        + "\"all_done\":true,"
                        + "\"slots\":[{\"timing_slot\":\"MORNING\",\"state\":\"DONE\"}],"
                        + "\"unchecked_doses\":[],"
                        + "\"upcoming_doses\":[],"
                        + "\"next_dose\":null"
                        + "}"
        ));
        Assert.assertEquals(Boolean.TRUE, finished.allDone);
        Assert.assertTrue(finished.uncheckedDoses.isEmpty());
        Assert.assertEquals(1L, finished.missed);
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
    public void morningInsideThreeHoursAsksAndLaterSlotsStayQuiet() throws Exception {
        java.time.Instant now = java.time.OffsetDateTime.parse("2026-10-27T09:30:00+09:00").toInstant();
        HomeContracts.Dose morning = dose(
                1L,
                "MORNING",
                "2026-10-27T08:00:00+09:00",
                "2026-10-27T11:00:00+09:00",
                "PENDING",
                null
        );
        HomeContracts.Dose lunch = dose(
                2L,
                "LUNCH",
                "2026-10-27T13:00:00+09:00",
                "2026-10-27T16:00:00+09:00",
                "PENDING",
                null
        );
        HomeContracts.Dose dinner = dose(
                3L,
                "DINNER",
                "2026-10-27T18:00:00+09:00",
                "2026-10-27T21:00:00+09:00",
                "PENDING",
                null
        );
        Assert.assertEquals(HomeContracts.DoseFollowUp.ASK, HomeContracts.followUp(morning, now));
        Assert.assertEquals(HomeContracts.STATUS_MISSED, HomeContracts.scheduleStatus(morning, now));
        Assert.assertEquals(HomeContracts.DoseFollowUp.NONE, HomeContracts.followUp(lunch, now));
        Assert.assertEquals(HomeContracts.DoseFollowUp.NONE, HomeContracts.followUp(dinner, now));
        Assert.assertEquals(morning.scheduledDoseId, HomeContracts.catchUpDose(
                java.util.Arrays.asList(dinner, lunch, morning),
                now
        ).scheduledDoseId);

        java.time.Instant afterClose = java.time.OffsetDateTime.parse("2026-10-27T11:01:00+09:00").toInstant();
        Assert.assertEquals(HomeContracts.DoseFollowUp.NONE, HomeContracts.followUp(morning, afterClose));

        HomeContracts.Dose delayed = dose(
                4L,
                "MORNING",
                "2026-10-27T08:00:00+09:00",
                "2026-10-27T11:00:00+09:00",
                "DELAYED",
                "2026-10-27T09:40:00+09:00"
        );
        Assert.assertEquals(HomeContracts.DoseFollowUp.RECORDED, HomeContracts.followUp(delayed, now));
        Assert.assertEquals(HomeContracts.STATUS_DELAYED, HomeContracts.scheduleStatus(delayed, now));
    }

    private static HomeContracts.Dose dose(
            long id,
            String slot,
            String scheduled,
            String close,
            String status,
            String taken
    ) throws Exception {
        String takenField = taken == null ? "null" : "\"" + taken + "\"";
        return HomeContracts.readDose(new JSONObject(
                "{"
                        + "\"scheduled_dose_id\":" + id + ",\"prescription_id\":4,\"hospital_name\":\"병원\","
                        + "\"timing_slot\":\"" + slot + "\",\"scheduled_at\":\"" + scheduled + "\","
                        + "\"normal_until\":\"2026-10-27T08:30:00+09:00\",\"close_at\":\"" + close + "\","
                        + "\"status\":\"" + status + "\",\"taken_at\":" + takenField + ","
                        + "\"decision_source\":null,\"corrected\":false,"
                        + "\"medications\":[{\"medication_id\":1,\"drug_name\":\"약\",\"dose_per_intake\":1}]"
                        + "}"
        ));
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
        java.util.List<SettingsContracts.ShareMedicine> shared = SettingsContracts.shareMedicines(
                java.util.Arrays.asList(
                        medication,
                        HomeContracts.readMedication(new JSONObject(
                                "{"
                                        + "\"medication_id\":10,\"prescription_id\":4,\"hospital_name\":null,\"drug_id\":null,"
                                        + "\"drug_name\":\"종료약\",\"manufacturer\":null,\"match_status\":\"UNMATCHED\","
                                        + "\"dose_per_intake\":1,\"is_prn\":false,\"times_per_day\":2,"
                                        + "\"timing_slots\":[\"MORNING\",\"DINNER\"],\"meal_relation\":null,\"meal_offset_min\":null,"
                                        + "\"total_days\":7,\"start_date\":\"2026-09-01\",\"end_date\":\"2026-09-07\","
                                        + "\"ended_on\":\"2026-09-03\",\"status\":\"ENDED\""
                                        + "}"
                        ))
                )
        );
        Assert.assertEquals(2, shared.size());
        Assert.assertEquals("직접입력", shared.get(0).name);
        Assert.assertEquals("1회 0.5 · 아침", shared.get(0).detail);
        Assert.assertFalse(shared.get(0).ended);
        Assert.assertTrue(shared.get(0).statusKnown);
        Assert.assertEquals("종료약", shared.get(1).name);
        Assert.assertEquals("2026-09-03", shared.get(1).endDate);
        Assert.assertTrue(shared.get(1).ended);
        Assert.assertEquals("1회 1 · 아침, 저녁", shared.get(1).detail);
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
    public void bedtimeIsSentInOrderAndScheduleKeepsServerWindows() throws Exception {
        EnvelopeDraft ready = draft();
        MedicineDraft medicine = ready.medicines().get(0);
        medicine.rounds().add(DoseRound.BEDTIME);
        medicine.rounds().add(DoseRound.EVENING);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.setAmount(DoseAmount.TABLET_HALF);
        medicine.timings().add(DoseTiming.AFTER_MEAL_30);
        medicine.setConfirmed(true);
        ready.alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        ready.alarmMinutes().put(DoseRound.EVENING, 16 * 60);
        ready.alarmMinutes().put(DoseRound.BEDTIME, 22 * 60);
        ready.setSlotIndex(0);
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(ready);
        Assert.assertEquals(HomeRegistration.Block.NONE, prepared.block);
        Assert.assertEquals(1, prepared.compartmentNo.intValue());
        JSONArray medications = prepared.prescription.getJSONArray("medications");
        JSONArray slots = medications.getJSONObject(0).getJSONArray("timing_slots");
        Assert.assertEquals(3, slots.length());
        Assert.assertEquals("MORNING", slots.getString(0));
        Assert.assertEquals("DINNER", slots.getString(1));
        Assert.assertEquals("BEDTIME", slots.getString(2));
        Assert.assertEquals(3, medications.getJSONObject(0).getInt("times_per_day"));
        Assert.assertEquals(0.5d, medications.getJSONObject(0).getDouble("dose_per_intake"), 0.001d);
        Assert.assertEquals("MANUAL", prepared.prescription.getString("source"));
        JSONArray settings = prepared.schedule.getJSONArray("settings");
        Assert.assertEquals("08:00", settings.getJSONObject(0).getString("dose_time"));
        Assert.assertEquals("16:00", settings.getJSONObject(1).getString("dose_time"));
        Assert.assertEquals("BEDTIME", settings.getJSONObject(2).getString("timing_slot"));
        Assert.assertEquals("22:00", settings.getJSONObject(2).getString("dose_time"));
        Assert.assertFalse(prepared.schedule.toString().contains("normal_window_min"));
        Assert.assertFalse(prepared.schedule.toString().contains("close_after_min"));

        JSONObject bedtime = HomeContracts.scheduleBody(
                java.util.Collections.singletonList(HomeContracts.SLOT_BEDTIME),
                java.util.Collections.singletonList("23:59")
        );
        Assert.assertEquals("23:59", bedtime.getJSONArray("settings").getJSONObject(0).getString("dose_time"));
        try {
            HomeContracts.scheduleBody(
                    java.util.Collections.singletonList(HomeContracts.SLOT_DINNER),
                    java.util.Collections.singletonList("21:00")
            );
            Assert.fail();
        } catch (ApiException exception) {
            Assert.assertFalse(exception.network());
        }

        HomeContracts.ScheduleSettings read = HomeContracts.readSchedule(new JSONObject(
                "{"
                        + "\"prescription_id\":4,"
                        + "\"settings\":["
                        + "{\"timing_slot\":\"BEDTIME\",\"dose_time\":\"23:30\",\"normal_window_min\":29,\"close_after_min\":30}"
                        + "]"
                        + "}"
        ));
        Assert.assertEquals(29L, read.settings.get(0).normalWindowMin);
        Assert.assertEquals(30L, read.settings.get(0).closeAfterMin);
        Assert.assertEquals("23:30", read.settings.get(0).doseTime);
    }

    @Test
    public void mealRelationKeepsTheChosenClock() throws Exception {
        Assert.assertEquals("08:00", doseTime(DoseTiming.BEFORE_MEAL, null));
        Assert.assertEquals("08:00", doseTime(DoseTiming.WITH_MEAL, null));
        Assert.assertEquals("08:00", doseTime(DoseTiming.AFTER_MEAL_30, null));
        Assert.assertEquals("08:00", doseTime(null, "WITH_MEAL"));
    }

    @Test
    public void confirmedMedicinesRegisterWhenAnEarlierSlotComesLater() throws Exception {
        EnvelopeDraft ready = draft();
        MedicineDraft evening = ready.medicines().get(0);
        evening.setName("저녁약");
        evening.rounds().add(DoseRound.EVENING);
        evening.timings().add(DoseTiming.AFTER_MEAL_30);
        evening.setConfirmed(true);
        MedicineDraft morning = new MedicineDraft();
        morning.setName("아침약");
        morning.setStart(LocalDate.parse("2026-10-27"));
        morning.setEnd(LocalDate.parse("2026-11-02"));
        morning.rounds().add(DoseRound.MORNING);
        morning.timings().add(DoseTiming.BEFORE_MEAL);
        morning.setConfirmed(true);
        ready.medicines().add(morning);
        MedicineDraft lunch = new MedicineDraft();
        lunch.setName("점심약");
        lunch.setStart(LocalDate.parse("2026-10-27"));
        lunch.setEnd(LocalDate.parse("2026-11-02"));
        lunch.rounds().add(DoseRound.LUNCH);
        lunch.timings().add(DoseTiming.WITH_MEAL);
        lunch.setConfirmed(true);
        ready.medicines().add(lunch);
        ready.alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        ready.alarmMinutes().put(DoseRound.LUNCH, 13 * 60);
        ready.alarmMinutes().put(DoseRound.EVENING, 18 * 60);
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(ready);
        Assert.assertEquals(HomeRegistration.Block.NONE, prepared.block);
        JSONArray settings = prepared.schedule.getJSONArray("settings");
        Assert.assertEquals("MORNING", settings.getJSONObject(0).getString("timing_slot"));
        Assert.assertEquals("08:00", settings.getJSONObject(0).getString("dose_time"));
        Assert.assertEquals("LUNCH", settings.getJSONObject(1).getString("timing_slot"));
        Assert.assertEquals("13:00", settings.getJSONObject(1).getString("dose_time"));
        Assert.assertEquals("DINNER", settings.getJSONObject(2).getString("timing_slot"));
        Assert.assertEquals("18:00", settings.getJSONObject(2).getString("dose_time"));
    }

    @Test
    public void mealOffsetThatBreaksTheGapKeepsTheChosenClock() throws Exception {
        EnvelopeDraft ready = draft();
        MedicineDraft medicine = ready.medicines().get(0);
        medicine.rounds().add(DoseRound.EVENING);
        medicine.rounds().add(DoseRound.BEDTIME);
        medicine.timings().add(DoseTiming.AFTER_MEAL_30);
        medicine.setConfirmed(true);
        ready.alarmMinutes().put(DoseRound.EVENING, 18 * 60);
        ready.alarmMinutes().put(DoseRound.BEDTIME, 22 * 60);
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(ready);
        Assert.assertEquals(HomeRegistration.Block.NONE, prepared.block);
        JSONArray settings = prepared.schedule.getJSONArray("settings");
        Assert.assertEquals("18:00", settings.getJSONObject(0).getString("dose_time"));
        Assert.assertEquals("22:00", settings.getJSONObject(1).getString("dose_time"));
    }

    private static String doseTime(DoseTiming timing, String mealRelation) throws Exception {
        EnvelopeDraft ready = draft();
        MedicineDraft medicine = ready.medicines().get(0);
        medicine.rounds().add(DoseRound.MORNING);
        medicine.setConfirmed(true);
        if (timing != null) {
            medicine.timings().add(timing);
        }
        if (mealRelation != null) {
            medicine.setMealRelation(mealRelation, null);
        }
        ready.alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(ready);
        Assert.assertEquals(HomeRegistration.Block.NONE, prepared.block);
        return prepared.schedule.getJSONArray("settings").getJSONObject(0).getString("dose_time");
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
        JSONArray deviceTerms = new JSONArray("["
                + "{\"terms_type\":\"TOS\",\"terms_version_id\":1,\"version\":\"1\",\"content\":\"a\",\"is_required\":true},"
                + "{\"terms_type\":\"DEVICE\",\"terms_version_id\":14,\"version\":\"3\",\"content\":\"device\",\"is_required\":true}"
                + "]");
        Assert.assertEquals(Long.valueOf(14L), HomeContracts.deviceTermsVersion(deviceTerms));
        Assert.assertNull(HomeContracts.deviceTermsVersion(terms));
        JSONArray duplicateDevice = new JSONArray("["
                + "{\"terms_type\":\"DEVICE\",\"terms_version_id\":14,\"version\":\"3\",\"content\":\"a\",\"is_required\":true},"
                + "{\"terms_type\":\"DEVICE\",\"terms_version_id\":15,\"version\":\"4\",\"content\":\"b\",\"is_required\":true}"
                + "]");
        Assert.assertNull(HomeContracts.deviceTermsVersion(duplicateDevice));
        Assert.assertEquals("2026-10-27T08:10:00+09:00", HomeContracts.takenAt(
                java.time.OffsetDateTime.parse("2026-10-27T08:10:00+09:00").toInstant().toEpochMilli()
        ));
    }

    @Test
    public void directNameOmitsDrugId() throws Exception {
        JSONObject typed = HomeContracts.medicationBody(
                null,
                "직접약",
                "1",
                false,
                java.util.Collections.singletonList(HomeContracts.SLOT_MORNING),
                "AFTER_MEAL",
                30,
                7,
                "2026-10-27"
        );
        Assert.assertFalse(typed.has("drug_id"));
        Assert.assertEquals("직접약", typed.getString("input_name"));
        Assert.assertFalse(typed.has("is_prn"));

        JSONObject prn = HomeContracts.medicationBody(
                null,
                "필요시약",
                null,
                true,
                java.util.Collections.emptyList(),
                null,
                null,
                3,
                "2026-10-27"
        );
        Assert.assertFalse(prn.has("drug_id"));
        Assert.assertEquals("필요시약", prn.getString("input_name"));
        Assert.assertTrue(prn.getBoolean("is_prn"));
        Assert.assertEquals(0, prn.getJSONArray("timing_slots").length());
    }

    @Test
    public void timingSlotStaysActiveAfterTakenAndIgnoresEmptyPrn() throws Exception {
        LocalDate today = LocalDate.parse("2026-10-27");
        HomeContracts.Dose taken = dose(
                11L,
                "MORNING",
                "2026-10-27T08:00:00+09:00",
                "2026-10-27T11:00:00+09:00",
                "NORMAL",
                "2026-10-27T08:05:00+09:00"
        );
        Assert.assertTrue(HomeContracts.timingSlotActive(
                HomeContracts.SLOT_MORNING,
                HomeContracts.STATE_DONE,
                true,
                java.util.Collections.emptyList(),
                true,
                null,
                false,
                today
        ));
        Assert.assertTrue(HomeContracts.timingSlotActive(
                HomeContracts.SLOT_MORNING,
                HomeContracts.STATE_NONE,
                true,
                java.util.Collections.singletonList(taken),
                true,
                null,
                false,
                today
        ));
        Assert.assertFalse(HomeContracts.timingSlotActive(
                HomeContracts.SLOT_LUNCH,
                HomeContracts.STATE_NONE,
                true,
                java.util.Collections.singletonList(taken),
                true,
                java.util.Collections.singletonList(activeMorning(today.toString(), today.plusDays(6).toString(), false)),
                true,
                today
        ));
        Assert.assertFalse(HomeContracts.timingSlotActive(
                HomeContracts.SLOT_MORNING,
                null,
                false,
                null,
                false,
                java.util.Collections.singletonList(activeMorning("2026-11-01", "2026-11-07", false)),
                true,
                today
        ));
        Assert.assertTrue(HomeContracts.timingSlotActive(
                HomeContracts.SLOT_MORNING,
                null,
                false,
                null,
                false,
                java.util.Collections.singletonList(activeMorning(today.toString(), today.plusDays(6).toString(), false)),
                true,
                today
        ));
        Assert.assertFalse(HomeContracts.timingSlotActive(
                HomeContracts.SLOT_MORNING,
                null,
                false,
                null,
                false,
                java.util.Collections.singletonList(activeMorning(today.toString(), today.plusDays(6).toString(), true)),
                true,
                today
        ));
    }

    private static HomeContracts.Medication activeMorning(String start, String end, boolean prn) throws Exception {
        String slots = prn ? "[]" : "[\"MORNING\"]";
        return HomeContracts.readMedication(new JSONObject(
                "{"
                        + "\"medication_id\":9,\"prescription_id\":4,\"hospital_name\":null,\"drug_id\":null,"
                        + "\"drug_name\":\"직접입력\",\"manufacturer\":null,\"match_status\":\"UNMATCHED\","
                        + "\"dose_per_intake\":1,\"is_prn\":" + prn + ",\"times_per_day\":" + (prn ? 0 : 1) + ","
                        + "\"timing_slots\":" + slots + ",\"meal_relation\":null,\"meal_offset_min\":null,"
                        + "\"total_days\":7,\"start_date\":\"" + start + "\",\"end_date\":\"" + end + "\","
                        + "\"ended_on\":null,\"status\":\"ACTIVE\""
                        + "}"
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
