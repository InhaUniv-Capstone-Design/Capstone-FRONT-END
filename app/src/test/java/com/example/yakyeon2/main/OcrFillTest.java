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
    public void bedtimeAndPrnAutoCheckLinkedTimings() {
        MedicineDraft medicine = new MedicineDraft();
        medicine.rounds().add(DoseRound.BEDTIME);
        medicine.rounds().add(DoseRound.MORNING);
        MedScreens.ensureAutoTimings(medicine);
        Assert.assertTrue(medicine.timings().contains(DoseTiming.BEDTIME));
        Assert.assertFalse(medicine.timings().contains(DoseTiming.ALWAYS));
        Assert.assertTrue(medicine.rounds().contains(DoseRound.MORNING));

        medicine.setPrn(true);
        MedScreens.ensureAutoTimings(medicine);
        Assert.assertTrue(medicine.isPrn());
        Assert.assertTrue(medicine.rounds().isEmpty());
        Assert.assertFalse(medicine.timings().contains(DoseTiming.BEDTIME));
        Assert.assertTrue(medicine.timings().contains(DoseTiming.ALWAYS));

        medicine.setPrn(false);
        medicine.rounds().add(DoseRound.EVENING);
        MedScreens.ensureAutoTimings(medicine);
        Assert.assertFalse(medicine.timings().contains(DoseTiming.ALWAYS));
        Assert.assertFalse(medicine.timings().contains(DoseTiming.BEDTIME));
        Assert.assertTrue(medicine.rounds().contains(DoseRound.EVENING));

        medicine.timings().add(DoseTiming.BEFORE_MEAL);
        medicine.setMealRelation("BEFORE_MEAL", 0);
        medicine.rounds().clear();
        medicine.rounds().add(DoseRound.BEDTIME);
        MedScreens.ensureAutoTimings(medicine);
        Assert.assertFalse(medicine.timings().contains(DoseTiming.BEFORE_MEAL));
        Assert.assertNull(medicine.getMealRelation());
        Assert.assertTrue(medicine.timings().contains(DoseTiming.BEDTIME));
    }

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
        Assert.assertFalse(medicine.rounds().contains(DoseRound.MORNING));
        Assert.assertFalse(medicine.timings().contains(DoseTiming.BEDTIME));
        Assert.assertNull(medicine.getMealRelation());
    }

    @Test
    public void missingSlotsDoNotGuessFromTimesPerDay() throws Exception {
        MedicineDraft medicine = fill("", "null", "null", 2);
        Assert.assertTrue(medicine.rounds().isEmpty());
        Assert.assertEquals(Long.valueOf(2L), lastRecognized().timesPerDay);
        Assert.assertNull(lastRecognized().timingSlots);
    }

    @Test
    public void emptySlotsFollowDailyCountAndUnknownSlotsStayLiteral() throws Exception {
        MedicineDraft drafted = fill("\"timing_slots\":[]", "null", "null", 2);
        Assert.assertTrue(drafted.rounds().contains(DoseRound.MORNING));
        Assert.assertTrue(drafted.rounds().contains(DoseRound.EVENING));
        Assert.assertEquals(2, drafted.rounds().size());
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
    public void withMealSelectsTogetherWithoutOffset() throws Exception {
        MedicineDraft medicine = fill("\"timing_slots\":[]", "\"WITH_MEAL\"", "null", 1);
        Assert.assertTrue(medicine.timings().contains(DoseTiming.WITH_MEAL));
        Assert.assertFalse(medicine.timings().contains(DoseTiming.BEFORE_MEAL));
        Assert.assertFalse(medicine.timings().contains(DoseTiming.AFTER_MEAL_30));
        Assert.assertFalse(medicine.timings().contains(DoseTiming.BEDTIME));
        Assert.assertEquals("WITH_MEAL", medicine.getMealRelation());
        Assert.assertNull(medicine.getMealOffsetMin());
        Assert.assertTrue(MedScreens.timingSelected(medicine, DoseTiming.WITH_MEAL));
        Assert.assertTrue(medicine.rounds().contains(DoseRound.MORNING));
        Assert.assertEquals(1, medicine.rounds().size());
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
        Assert.assertFalse(store.draft().isFromOcr());
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

    @Test
    public void nullDoseLeavesOnlyTheAmountBlank() throws Exception {
        MedicineDraft medicine = fillNullable(
                "null",
                "2",
                "5",
                "[\"MORNING\",\"DINNER\"]",
                "\"BEFORE_MEAL\"",
                "null"
        );
        Assert.assertEquals("아모디핀정 5mg", medicine.getName());
        Assert.assertNull(medicine.getDoseText());
        Assert.assertNull(medicine.getAmount());
        Assert.assertTrue(medicine.rounds().contains(DoseRound.MORNING));
        Assert.assertTrue(medicine.rounds().contains(DoseRound.EVENING));
        Assert.assertEquals(2, medicine.rounds().size());
        Assert.assertEquals(medicine.getStart().plusDays(4), medicine.getEnd());
        Assert.assertTrue(medicine.timings().contains(DoseTiming.BEFORE_MEAL));
        Assert.assertNull(medicine.getMealOffsetMin());
    }

    @Test
    public void nullTimesPerDayDoesNotInventACount() throws Exception {
        MedicineDraft medicine = fillNullable("1", "null", "5", "[]", "\"AFTER_MEAL\"", "30");
        Assert.assertEquals("1", medicine.getDoseText());
        Assert.assertTrue(medicine.rounds().isEmpty());
        Assert.assertEquals(medicine.getStart().plusDays(4), medicine.getEnd());
        Assert.assertTrue(medicine.timings().contains(DoseTiming.AFTER_MEAL_30));
        Assert.assertEquals(Integer.valueOf(30), medicine.getMealOffsetMin());
        Assert.assertNull(readNullable("1", "null", "5", "[]", "\"AFTER_MEAL\"", "30").timesPerDay);
    }

    @Test
    public void nullTotalDaysLeavesTheEndDateBlank() throws Exception {
        MedicineDraft medicine = fillNullable("1", "1", "null", "[\"LUNCH\"]", "null", "null");
        Assert.assertEquals("1", medicine.getDoseText());
        Assert.assertNull(medicine.getEnd());
        Assert.assertTrue(medicine.rounds().contains(DoseRound.LUNCH));
        Assert.assertEquals(1, medicine.rounds().size());
        Assert.assertTrue(medicine.timings().isEmpty());
        Assert.assertNull(medicine.getMealRelation());
    }

    @Test
    public void namedDrugWithNoDosingStillBecomesADraft() throws Exception {
        MedicineDraft medicine = fillNullable("null", "null", "null", "[]", "null", "null");
        Assert.assertEquals("아모디핀정 5mg", medicine.getName());
        Assert.assertNull(medicine.getDoseText());
        Assert.assertNull(medicine.getAmount());
        Assert.assertNull(medicine.getEnd());
        Assert.assertTrue(medicine.rounds().isEmpty());
        Assert.assertTrue(medicine.timings().isEmpty());
        Assert.assertNull(medicine.getMealRelation());
        Assert.assertNull(medicine.getMealOffsetMin());
        Assert.assertFalse(medicine.isPrn());
    }

    @Test
    public void emptySlotsUseDailyCountAsDraftSelection() throws Exception {
        assertDraftRounds(1, DoseRound.MORNING);
        assertDraftRounds(2, DoseRound.MORNING, DoseRound.EVENING);
        assertDraftRounds(3, DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING);
        assertDraftRounds(4, DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING, DoseRound.BEDTIME);
        MedicineDraft four = fillNullable("1", "4", "3", "[]", "null", "null");
        Assert.assertFalse(four.timings().contains(DoseTiming.BEDTIME));
        Assert.assertTrue(fillNullable("1", "0", "3", "[]", "null", "null").rounds().isEmpty());
        Assert.assertTrue(fillNullable("1", "null", "3", "[]", "null", "null").rounds().isEmpty());
    }

    @Test
    public void returnedSlotsWinOverTheDailyCountDraft() throws Exception {
        MedicineDraft medicine = fillNullable("1", "3", "3", "[\"BEDTIME\"]", "null", "null");
        Assert.assertTrue(medicine.rounds().contains(DoseRound.BEDTIME));
        Assert.assertEquals(1, medicine.rounds().size());
        Assert.assertFalse(medicine.timings().contains(DoseTiming.BEDTIME));
        Assert.assertNull(medicine.getMealRelation());
    }

    @Test
    public void afterMealWithoutOffsetSelectsAfterAndSavesWithoutThirty() throws Exception {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        store.beginDraft();
        MedScreens.fillOcr(store, HomeContracts.readOcr(response(
                "\"timing_slots\":[\"MORNING\"]",
                "\"AFTER_MEAL\"",
                "null",
                1
        )));
        MedicineDraft medicine = store.draft().medicines().get(0);
        Assert.assertTrue(medicine.timings().contains(DoseTiming.AFTER_MEAL_30));
        Assert.assertTrue(MedScreens.timingSelected(medicine, DoseTiming.AFTER_MEAL_30));
        Assert.assertEquals("AFTER_MEAL", medicine.getMealRelation());
        Assert.assertNull(medicine.getMealOffsetMin());
        medicine.setConfirmed(true);
        store.draft().alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(store.draft());
        Assert.assertEquals(HomeRegistration.Block.NONE, prepared.block);
        JSONObject item = prepared.prescription.getJSONArray("medications").getJSONObject(0);
        Assert.assertEquals("AFTER_MEAL", item.getString("meal_relation"));
        Assert.assertFalse(item.has("meal_offset_min"));
    }

    @Test
    public void offsetIsNotSentWithoutMealRelation() throws Exception {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        store.beginDraft();
        MedScreens.fillOcr(store, HomeContracts.readOcr(response(
                "\"timing_slots\":[\"MORNING\"]",
                "\"AFTER_MEAL\"",
                "null",
                1
        )));
        MedicineDraft medicine = store.draft().medicines().get(0);
        medicine.clearMealRelation();
        medicine.setMealRelation(null, 30);
        medicine.timings().clear();
        medicine.setConfirmed(true);
        store.draft().alarmMinutes().put(DoseRound.MORNING, 8 * 60);
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(store.draft());
        Assert.assertEquals(HomeRegistration.Block.NONE, prepared.block);
        JSONObject item = prepared.prescription.getJSONArray("medications").getJSONObject(0);
        Assert.assertFalse(item.has("meal_relation"));
        Assert.assertFalse(item.has("meal_offset_min"));
    }

    @Test
    public void withMealSavesWithoutOffset() throws Exception {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        store.beginDraft();
        MedScreens.fillOcr(store, HomeContracts.readOcr(response(
                "\"timing_slots\":[\"DINNER\"]",
                "\"WITH_MEAL\"",
                "null",
                1
        )));
        MedicineDraft medicine = store.draft().medicines().get(0);
        Assert.assertTrue(medicine.timings().contains(DoseTiming.WITH_MEAL));
        Assert.assertNull(medicine.getMealOffsetMin());
        medicine.setConfirmed(true);
        store.draft().alarmMinutes().put(DoseRound.EVENING, 18 * 60);
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(store.draft());
        Assert.assertEquals(HomeRegistration.Block.NONE, prepared.block);
        JSONObject item = prepared.prescription.getJSONArray("medications").getJSONObject(0);
        Assert.assertEquals("WITH_MEAL", item.getString("meal_relation"));
        Assert.assertFalse(item.has("meal_offset_min"));
        JSONArray slots = item.getJSONArray("timing_slots");
        Assert.assertEquals(1, slots.length());
        Assert.assertEquals("DINNER", slots.getString(0));
    }

    @Test
    public void sameRegimenOnThreeDrugsCanBeEditedSeparately() throws Exception {
        String shared = "\"dose_per_intake\":1,\"times_per_day\":3,\"total_days\":3,"
                + "\"timing_slots\":[],\"meal_relation\":\"AFTER_MEAL\",\"meal_offset_min\":null";
        JSONObject response = new JSONObject("{"
                + "\"hospital_name\":\"내과의원\",\"recognized\":["
                + "{\"drug_name\":\"약A\"," + shared + "},"
                + "{\"drug_name\":\"약B\"," + shared + "},"
                + "{\"drug_name\":\"약C\"," + shared + "}"
                + "]}");
        MedicationStore store = new MedicationStore(new YakyeonSession());
        store.beginDraft();
        MedScreens.fillOcr(store, HomeContracts.readOcr(response));
        Assert.assertEquals(3, store.draft().medicines().size());
        MedicineDraft first = store.draft().medicines().get(0);
        MedicineDraft second = store.draft().medicines().get(1);
        MedicineDraft third = store.draft().medicines().get(2);
        Assert.assertEquals("약A", first.getName());
        Assert.assertEquals("약B", second.getName());
        Assert.assertEquals("약C", third.getName());
        Assert.assertNull(first.getMealOffsetMin());
        Assert.assertTrue(first.timings().contains(DoseTiming.AFTER_MEAL_30));
        Assert.assertTrue(second.rounds().contains(DoseRound.MORNING));
        Assert.assertTrue(second.rounds().contains(DoseRound.LUNCH));
        Assert.assertTrue(second.rounds().contains(DoseRound.EVENING));
        first.setDoseText("2");
        first.rounds().remove(DoseRound.LUNCH);
        first.clearMealRelation();
        first.timings().clear();
        Assert.assertEquals("1", second.getDoseText());
        Assert.assertEquals("1", third.getDoseText());
        Assert.assertTrue(second.rounds().contains(DoseRound.LUNCH));
        Assert.assertTrue(third.rounds().contains(DoseRound.LUNCH));
        Assert.assertEquals("AFTER_MEAL", second.getMealRelation());
        Assert.assertEquals("AFTER_MEAL", third.getMealRelation());
        Assert.assertNull(second.getMealOffsetMin());
        Assert.assertTrue(third.timings().contains(DoseTiming.AFTER_MEAL_30));
    }

    @Test
    public void partialRecognitionKeepsBothDrafts() throws Exception {
        JSONObject response = new JSONObject("{"
                + "\"recognized\":["
                + "{\"drug_name\":\"아모디핀정 5mg\",\"dose_per_intake\":null,\"times_per_day\":3,"
                + "\"total_days\":null,\"timing_slots\":[],\"meal_relation\":null,\"meal_offset_min\":null},"
                + "{\"drug_name\":\"에스오메프라졸캡슐\",\"dose_per_intake\":1,\"times_per_day\":3,\"total_days\":7,"
                + "\"timing_slots\":[\"MORNING\",\"LUNCH\",\"DINNER\"],\"meal_relation\":\"AFTER_MEAL\","
                + "\"meal_offset_min\":30}"
                + "]}");
        HomeContracts.OcrDraft parsed = HomeContracts.readOcr(response);
        Assert.assertEquals(2, parsed.recognized.size());
        Assert.assertNull(parsed.recognized.get(0).dosePerIntake);
        Assert.assertEquals(Long.valueOf(3L), parsed.recognized.get(0).timesPerDay);
        Assert.assertNull(parsed.recognized.get(0).totalDays);
        Assert.assertTrue(parsed.recognized.get(0).timingSlots.isEmpty());
        Assert.assertNull(parsed.recognized.get(0).mealRelation);
        Assert.assertEquals(Long.valueOf(30L), parsed.recognized.get(1).mealOffsetMin);
        MedicationStore store = new MedicationStore(new YakyeonSession());
        store.beginDraft();
        MedScreens.fillOcr(store, parsed);
        Assert.assertEquals(2, store.draft().medicines().size());
        MedicineDraft first = store.draft().medicines().get(0);
        MedicineDraft second = store.draft().medicines().get(1);
        Assert.assertEquals("아모디핀정 5mg", first.getName());
        Assert.assertNull(first.getDoseText());
        Assert.assertNull(first.getEnd());
        Assert.assertTrue(first.rounds().contains(DoseRound.MORNING));
        Assert.assertTrue(first.rounds().contains(DoseRound.LUNCH));
        Assert.assertTrue(first.rounds().contains(DoseRound.EVENING));
        Assert.assertFalse(first.rounds().contains(DoseRound.BEDTIME));
        Assert.assertTrue(first.timings().isEmpty());
        Assert.assertNull(first.getMealRelation());
        Assert.assertEquals("에스오메프라졸캡슐", second.getName());
        Assert.assertEquals("1", second.getDoseText());
        Assert.assertEquals(second.getStart().plusDays(6), second.getEnd());
        Assert.assertEquals(3, second.rounds().size());
        Assert.assertTrue(second.timings().contains(DoseTiming.AFTER_MEAL_30));
        Assert.assertEquals("AFTER_MEAL", second.getMealRelation());
        Assert.assertEquals(Integer.valueOf(30), second.getMealOffsetMin());
        first.rounds().clear();
        first.setDoseText("9");
        Assert.assertEquals(3, second.rounds().size());
        Assert.assertEquals("1", second.getDoseText());
    }

    private static void assertDraftRounds(int times, DoseRound... expected) throws Exception {
        MedicineDraft medicine = fillNullable("1", Integer.toString(times), "3", "[]", "null", "null");
        Assert.assertEquals(expected.length, medicine.rounds().size());
        for (DoseRound round : expected) {
            Assert.assertTrue(medicine.rounds().contains(round));
        }
    }

    private static HomeContracts.RecognizedMedication readNullable(
            String dose,
            String times,
            String days,
            String slots,
            String meal,
            String offset
    ) throws Exception {
        return HomeContracts.readOcr(nullableResponse(dose, times, days, slots, meal, offset)).recognized.get(0);
    }

    private static MedicineDraft fillNullable(
            String dose,
            String times,
            String days,
            String slots,
            String meal,
            String offset
    ) throws Exception {
        MedicationStore store = new MedicationStore(new YakyeonSession());
        store.beginDraft();
        MedScreens.fillOcr(store, HomeContracts.readOcr(nullableResponse(dose, times, days, slots, meal, offset)));
        return store.draft().medicines().get(0);
    }

    private static JSONObject nullableResponse(
            String dose,
            String times,
            String days,
            String slots,
            String meal,
            String offset
    ) throws Exception {
        return new JSONObject("{"
                + "\"hospital_name\":\"내과의원\",\"pharmacy_name\":null,\"prescribed_date\":\"2024-06-19\","
                + "\"recognized\":[{"
                + "\"drug_name\":\"아모디핀정 5mg\","
                + "\"dose_per_intake\":" + dose + ","
                + "\"times_per_day\":" + times + ","
                + "\"total_days\":" + days + ","
                + "\"timing_slots\":" + slots + ","
                + "\"meal_relation\":" + meal + ","
                + "\"meal_offset_min\":" + offset
                + "}]}");
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
