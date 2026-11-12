package com.example.yakyeon2.main;

import com.example.yakyeon2.api.HomeContracts;
import com.example.yakyeon2.api.HomeRegistration;
import com.example.yakyeon2.data.DoseRound;
import com.example.yakyeon2.data.DoseTiming;
import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.MedicineDraft;
import com.example.yakyeon2.data.YakyeonSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class OcrFillTest {

    @Test
    public void morningAndDinnerCheckTwoRounds() throws Exception {
        MedicineDraft medicine = fill("\"timing_slots\":[\"MORNING\",\"DINNER\"]", "null", "null", 2);
        Assert.assertTrue(medicine.rounds().contains(DoseRound.MORNING));
        Assert.assertTrue(medicine.rounds().contains(DoseRound.EVENING));
        Assert.assertEquals(2, medicine.rounds().size());
        Assert.assertEquals("아모디핀정 5mg", medicine.getName());
        Assert.assertEquals("1", medicine.getDoseText());
        Assert.assertNotNull(medicine.getEnd());
    }

    @Test
    public void threeMealSlotsCheckThreeRounds() throws Exception {
        MedicineDraft medicine = fill(
                "\"timing_slots\":[\"MORNING\",\"LUNCH\",\"DINNER\"]",
                "null",
                "null",
                3
        );
        Assert.assertTrue(medicine.rounds().contains(DoseRound.MORNING));
        Assert.assertTrue(medicine.rounds().contains(DoseRound.LUNCH));
        Assert.assertTrue(medicine.rounds().contains(DoseRound.EVENING));
        Assert.assertEquals(3, medicine.rounds().size());
    }

    @Test
    public void bedtimeSlotChecksOneRound() throws Exception {
        MedicineDraft medicine = fill("\"timing_slots\":[\"BEDTIME\"]", "null", "null", 1);
        Assert.assertTrue(medicine.rounds().contains(DoseRound.BEDTIME));
        Assert.assertEquals(1, medicine.rounds().size());
    }

    @Test
    public void missingSlotsDoNotGuessFromTimesPerDay() throws Exception {
        MedicineDraft medicine = fill("", "null", "null", 2);
        Assert.assertTrue(medicine.rounds().isEmpty());
        Assert.assertEquals(2L, lastRecognized().timesPerDay);
        Assert.assertNull(lastRecognized().timingSlots);
    }

    @Test
    public void emptyOrUnknownSlotsStayUnchecked() throws Exception {
        Assert.assertTrue(fill("\"timing_slots\":[]", "null", "null", 2).rounds().isEmpty());
        Assert.assertTrue(fill("\"timing_slots\":null", "null", "null", 2).rounds().isEmpty());
        MedicineDraft mixed = fill(
                "\"timing_slots\":[\"NOON\", 1, \"MORNING\", \"MORNING\"]",
                "\"MIDNIGHT\"",
                "\"soon\"",
                2
        );
        Assert.assertTrue(mixed.rounds().contains(DoseRound.MORNING));
        Assert.assertEquals(1, mixed.rounds().size());
        Assert.assertTrue(mixed.timings().isEmpty());
        Assert.assertNull(mixed.getMealRelation());
        Assert.assertTrue(fill("\"timing_slots\":\"MORNING\"", "null", "null", 1).rounds().isEmpty());
    }

    @Test
    public void beforeMealChecksBefore() throws Exception {
        MedicineDraft medicine = fill("\"timing_slots\":[]", "\"BEFORE_MEAL\"", "null", 1);
        Assert.assertTrue(medicine.timings().contains(DoseTiming.BEFORE_MEAL));
        Assert.assertEquals("BEFORE_MEAL", medicine.getMealRelation());
    }

    @Test
    public void afterMealChecksOnlyWhenOffsetIsThirty() throws Exception {
        MedicineDraft matched = fill("\"timing_slots\":[]", "\"AFTER_MEAL\"", "30", 1);
        Assert.assertTrue(matched.timings().contains(DoseTiming.AFTER_MEAL_30));
        Assert.assertEquals(Integer.valueOf(30), matched.getMealOffsetMin());
        MedicineDraft other = fill("\"timing_slots\":[]", "\"AFTER_MEAL\"", "0", 1);
        Assert.assertTrue(other.timings().isEmpty());
        Assert.assertEquals("AFTER_MEAL", other.getMealRelation());
        Assert.assertEquals(Integer.valueOf(0), other.getMealOffsetMin());
    }

    @Test
    public void afterMealPhraseIsKeptForDisplayAndNotSentAsKorean() throws Exception {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        store.beginDraft();
        MedScreens.fillOcr(store, HomeContracts.readOcr(response(
                "\"timing_slots\":[\"MORNING\"]",
                "\"식후 30분\"",
                "30",
                1
        )));
        MedicineDraft medicine = store.draft().medicines().get(0);
        Assert.assertEquals("식후 30분", medicine.getMealRelation());
        Assert.assertEquals(Integer.valueOf(30), medicine.getMealOffsetMin());
        Assert.assertFalse(medicine.timings().contains(DoseTiming.AFTER_MEAL_30));
        Assert.assertTrue(MedScreens.timingSelected(medicine, DoseTiming.AFTER_MEAL_30));
        Assert.assertEquals("식후 30분", medicine.getMealRelation());
        medicine.setConfirmed(true);
        store.draft().alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(store.draft());
        Assert.assertEquals(HomeRegistration.Block.NONE, prepared.block);
        JSONObject item = prepared.prescription.getJSONArray("medications").getJSONObject(0);
        Assert.assertFalse(item.has("meal_relation"));
        Assert.assertFalse(item.has("meal_offset_min"));
        Assert.assertEquals("식후 30분", medicine.getMealRelation());
        Assert.assertEquals(Integer.valueOf(30), medicine.getMealOffsetMin());
    }

    @Test
    public void afterMealEnumStillSendsOffsetThirty() throws Exception {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        store.beginDraft();
        MedScreens.fillOcr(store, HomeContracts.readOcr(response(
                "\"timing_slots\":[\"MORNING\"]",
                "\"AFTER_MEAL\"",
                "30",
                1
        )));
        MedicineDraft medicine = store.draft().medicines().get(0);
        Assert.assertTrue(medicine.timings().contains(DoseTiming.AFTER_MEAL_30));
        medicine.setConfirmed(true);
        store.draft().alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(store.draft());
        JSONObject item = prepared.prescription.getJSONArray("medications").getJSONObject(0);
        Assert.assertEquals("AFTER_MEAL", item.getString("meal_relation"));
        Assert.assertEquals(30, item.getInt("meal_offset_min"));
        Assert.assertEquals("AFTER_MEAL", medicine.getMealRelation());
    }

    @Test
    public void beforeThirtyPhraseIsLeftUntouched() throws Exception {
        MedicineDraft medicine = fill("\"timing_slots\":[]", "\"식전 30분\"", "30", 1);
        Assert.assertNull(medicine.getMealRelation());
        Assert.assertFalse(medicine.timings().contains(DoseTiming.BEFORE_MEAL));
        Assert.assertFalse(MedScreens.timingSelected(medicine, DoseTiming.BEFORE_MEAL));
    }

    @Test
    public void withMealHasNoCheckbox() throws Exception {
        MedicineDraft medicine = fill("\"timing_slots\":[]", "\"WITH_MEAL\"", "null", 1);
        Assert.assertTrue(medicine.timings().isEmpty());
        Assert.assertEquals("WITH_MEAL", medicine.getMealRelation());
    }

    @Test
    public void manualDraftIsLeftAlone() throws Exception {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        store.beginDraft();
        MedicineDraft manual = store.addMedicine(java.time.LocalDate.parse("2026-11-10"));
        manual.setName("직접입력");
        manual.rounds().add(DoseRound.LUNCH);
        manual.timings().add(DoseTiming.BEFORE_MEAL);
        MedScreens.fillOcr(store, HomeContracts.readOcr(new JSONObject(
                "{\"hospital_name\":null,\"pharmacy_name\":null,\"prescribed_date\":null,\"recognized\":[]}"
        )));
        Assert.assertEquals(1, store.draft().medicines().size());
        Assert.assertEquals("직접입력", manual.getName());
        Assert.assertTrue(manual.rounds().contains(DoseRound.LUNCH));
        Assert.assertTrue(manual.timings().contains(DoseTiming.BEFORE_MEAL));
    }

    @Test
    public void registrationKeepsSlotsAndMealRelation() throws Exception {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        store.beginDraft();
        store.draft().setHospitalName("내과의원");
        MedScreens.fillOcr(store, HomeContracts.readOcr(response(
                "\"timing_slots\":[\"DINNER\",\"MORNING\"]",
                "\"BEFORE_MEAL\"",
                "null",
                2
        )));
        MedicineDraft medicine = store.draft().medicines().get(0);
        medicine.setConfirmed(true);
        store.draft().alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        store.draft().alarmMinutes().put(DoseRound.EVENING, 18 * 60);
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(store.draft());
        Assert.assertEquals(HomeRegistration.Block.NONE, prepared.block);
        JSONObject item = prepared.prescription.getJSONArray("medications").getJSONObject(0);
        JSONArray slots = item.getJSONArray("timing_slots");
        Assert.assertEquals("MORNING", slots.getString(0));
        Assert.assertEquals("DINNER", slots.getString(1));
        Assert.assertEquals(2, item.getInt("times_per_day"));
        Assert.assertEquals("BEFORE_MEAL", item.getString("meal_relation"));
        Assert.assertEquals("OCR", prepared.prescription.getString("source"));
    }

    private HomeContracts.RecognizedMedication lastRecognized() throws Exception {
        return HomeContracts.readOcr(response("", "null", "null", 2)).recognized.get(0);
    }

    private static MedicineDraft fill(String slotsField, String meal, String offset, int times) throws Exception {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        store.beginDraft();
        MedScreens.fillOcr(store, HomeContracts.readOcr(response(slotsField, meal, offset, times)));
        return store.draft().medicines().get(0);
    }

    private static JSONObject response(String slotsField, String meal, String offset, int times) throws Exception {
        String slots = slotsField == null || slotsField.isEmpty() ? "" : "," + slotsField;
        return new JSONObject("{"
                + "\"hospital_name\":\"내과의원\",\"pharmacy_name\":null,\"prescribed_date\":\"2024-06-19\","
                + "\"recognized\":[{"
                + "\"drug_name\":\"아모디핀정 5mg\",\"matched_drug_id\":null,\"candidates\":[],"
                + "\"dose_per_intake\":1,\"times_per_day\":" + times + ","
                + "\"total_days\":30,\"is_prn\":false,"
                + "\"meal_relation\":" + meal + ",\"meal_offset_min\":" + offset
                + slots
                + "}]}");
    }
}
