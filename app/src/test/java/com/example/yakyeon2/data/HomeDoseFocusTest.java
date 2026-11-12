package com.example.yakyeon2.data;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HomeDoseFocusTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 8);

    @Test
    public void everyRemainingDoseAfterNowIsListed() {
        long now = at(13, 48);
        List<HomeDoseFocus.Entry> doses = Arrays.asList(
                dose("한의원", "MORNING", 8, 0, true),
                dose("한의원", "LUNCH", 12, 10, false),
                dose("내과", "LUNCH", 12, 30, false),
                dose("한의원", "LUNCH", 12, 0, true),
                dose("한의원", "DINNER", 17, 10, false),
                dose("내과", "DINNER", 17, 20, false),
                dose("내과", "BEDTIME", 22, 0, false)
        );

        HomeDoseFocus.Board board = HomeDoseFocus.focus(doses, now, ZONE);

        assertEquals(HomeDoseFocus.Outcome.ACTIVE, board.outcome);
        assertEquals(2, board.overdue.size());
        assertEquals(at(12, 10), board.overdue.get(0).scheduledAtMillis);
        assertEquals(at(12, 30), board.overdue.get(1).scheduledAtMillis);
        assertEquals(3, board.upcoming.size());
        assertEquals("한의원", board.upcoming.get(0).place);
        assertEquals(at(17, 10), board.upcoming.get(0).scheduledAtMillis);
        assertEquals(at(17, 20), board.upcoming.get(1).scheduledAtMillis);
        assertEquals(at(22, 0), board.upcoming.get(2).scheduledAtMillis);
    }

    @Test
    public void sameNearestTimeKeepsLaterDosesToo() {
        long now = at(13, 48);
        HomeDoseFocus.Board board = HomeDoseFocus.focus(Arrays.asList(
                dose("한의원", "DINNER", 17, 10, false),
                dose("내과", "DINNER", 17, 10, false),
                dose("내과", "BEDTIME", 22, 0, false)
        ), now, ZONE);

        assertEquals(3, board.upcoming.size());
        assertTrue(board.overdue.isEmpty());
    }

    @Test
    public void takenDosesAreNotRepeatedAsUpcoming() {
        long now = at(15, 0);
        HomeDoseFocus.Board board = HomeDoseFocus.focus(Arrays.asList(
                dose("한의원", "MORNING", 8, 0, true),
                dose("한의원", "LUNCH", 12, 10, true),
                dose("한의원", "DINNER", 17, 10, false),
                dose("내과", "BEDTIME", 22, 0, true)
        ), now, ZONE);

        assertEquals(1, board.upcoming.size());
        assertEquals(at(17, 10), board.upcoming.get(0).scheduledAtMillis);
        assertEquals(HomeDoseFocus.Outcome.ACTIVE, board.outcome);
    }

    @Test
    public void allDosesThroughBedtimeTakenShowsDone() {
        long now = at(22, 0);
        HomeDoseFocus.Board board = HomeDoseFocus.focus(Arrays.asList(
                dose("피부과", "MORNING", 8, 0, true),
                dose("한의원", "DINNER", 19, 30, true),
                dose("내과", "BEDTIME", 22, 0, true)
        ), now, ZONE);

        assertEquals(HomeDoseFocus.Outcome.DONE, board.outcome);
        assertTrue(board.overdue.isEmpty());
        assertTrue(board.upcoming.isEmpty());
    }

    @Test
    public void missedAndCompletedDosesFinishTheDay() {
        long now = at(22, 0);
        HomeDoseFocus.Board board = HomeDoseFocus.focus(Arrays.asList(
                new HomeDoseFocus.Entry("피부과", "MORNING", at(8, 0), false, true),
                dose("한의원", "DINNER", 19, 30, true),
                dose("내과", "BEDTIME", 22, 0, true)
        ), now, ZONE);

        assertEquals(HomeDoseFocus.Outcome.DONE, board.outcome);
        assertTrue(board.overdue.isEmpty());
        assertTrue(board.upcoming.isEmpty());
    }

    @Test
    public void anOpenDoseStillBlocksWhenAnotherWasMissed() {
        long now = at(18, 0);
        HomeDoseFocus.Board board = HomeDoseFocus.focus(Arrays.asList(
                new HomeDoseFocus.Entry("피부과", "MORNING", at(8, 0), false, true),
                dose("한의원", "DINNER", 19, 30, false)
        ), now, ZONE);

        assertEquals(HomeDoseFocus.Outcome.ACTIVE, board.outcome);
        assertTrue(board.overdue.isEmpty());
        assertEquals(1, board.upcoming.size());
    }

    @Test
    public void anUntakenEarlierDoseBlocksTheDoneMessage() {
        long now = at(22, 0);
        HomeDoseFocus.Board board = HomeDoseFocus.focus(Arrays.asList(
                dose("피부과", "MORNING", 8, 0, false),
                dose("한의원", "DINNER", 19, 30, true),
                dose("내과", "BEDTIME", 22, 0, true)
        ), now, ZONE);

        assertEquals(HomeDoseFocus.Outcome.ACTIVE, board.outcome);
        assertEquals(1, board.overdue.size());
        assertTrue(board.upcoming.isEmpty());
    }

    @Test
    public void finishedDayWithNoFutureDoseIsDoneBeforeBedtime() {
        long now = at(18, 0);
        HomeDoseFocus.Board board = HomeDoseFocus.focus(Collections.singletonList(
                dose("한의원", "MORNING", 8, 0, true)
        ), now, ZONE);

        assertEquals(HomeDoseFocus.Outcome.DONE, board.outcome);
    }

    @Test
    public void noDosesStayEmptyRatherThanDone() {
        HomeDoseFocus.Board board = HomeDoseFocus.focus(new ArrayList<>(), at(12, 0), ZONE);
        assertEquals(HomeDoseFocus.Outcome.EMPTY, board.outcome);
    }

    @Test
    public void doseAfterBedtimeIsOutsideTheWindow() {
        long now = at(21, 0);
        HomeDoseFocus.Board board = HomeDoseFocus.focus(Arrays.asList(
                dose("내과", "BEDTIME", 22, 0, true),
                dose("내과", "DINNER", 23, 30, false)
        ), now, ZONE);

        assertEquals(HomeDoseFocus.Outcome.DONE, board.outcome);
    }

    @Test
    public void withoutABedtimeDoseTheExistingLatestMinuteClosesTheDay() {
        long now = at(21, 0);
        long end = HomeDoseFocus.bedtimeMillis(Collections.emptyList(), now, ZONE);
        assertEquals(at(23, 59), end);

        HomeDoseFocus.Board board = HomeDoseFocus.focus(Collections.singletonList(
                dose("내과", "DINNER", 23, 30, false)
        ), now, ZONE);
        assertEquals(1, board.upcoming.size());
    }

    @Test
    public void scheduledTodayKeepsEveryDoseOnThatDay() {
        long now = at(13, 48);
        List<HomeDoseFocus.Entry> today = HomeDoseFocus.scheduledToday(Arrays.asList(
                dose("한의원", "MORNING", 8, 0, true),
                dose("한의원", "LUNCH", 12, 10, false),
                dose("내과", "LUNCH", 12, 30, false),
                dose("한의원", "DINNER", 17, 10, false),
                dose("내과", "BEDTIME", 22, 0, false),
                new HomeDoseFocus.Entry("어제", "DINNER", DAY.minusDays(1).atTime(19, 0).atZone(ZONE).toInstant().toEpochMilli(), false)
        ), now, ZONE);

        assertEquals(5, today.size());
        assertEquals(at(8, 0), today.get(0).scheduledAtMillis);
        assertTrue(today.get(0).taken);
        assertEquals(at(22, 0), today.get(4).scheduledAtMillis);
    }

    @Test
    public void aMissedDoseStaysOnItsOwnDayAndDoesNotFillTomorrow() {
        long yesterdayMissed = DAY.minusDays(1).atTime(19, 0).atZone(ZONE).toInstant().toEpochMilli();
        HomeDoseFocus.Board nextMorning = HomeDoseFocus.focus(Arrays.asList(
                new HomeDoseFocus.Entry("어제", "DINNER", yesterdayMissed, false),
                dose("한의원", "MORNING", 8, 0, false)
        ), at(7, 0), ZONE);

        assertEquals(HomeDoseFocus.Outcome.ACTIVE, nextMorning.outcome);
        assertTrue(nextMorning.overdue.isEmpty());
        assertEquals(1, nextMorning.upcoming.size());
        assertEquals("한의원", nextMorning.upcoming.get(0).place);

        HomeDoseFocus.Board onlyYesterday = HomeDoseFocus.focus(Collections.singletonList(
                new HomeDoseFocus.Entry("어제", "DINNER", yesterdayMissed, false)
        ), at(9, 0), ZONE);
        assertEquals(HomeDoseFocus.Outcome.EMPTY, onlyYesterday.outcome);
    }

    @Test
    public void anOldUntakenDoseDoesNotBecomeACompletedDay() {
        List<HomeDoseFocus.Entry> doses = Collections.singletonList(
                dose("한의원", "LUNCH", 12, 10, false)
        );
        HomeDoseFocus.Board during = HomeDoseFocus.focus(doses, at(14, 0), ZONE);
        HomeDoseFocus.Board later = HomeDoseFocus.focus(doses, at(16, 0), ZONE);

        assertEquals(1, during.overdue.size());
        assertEquals(HomeDoseFocus.Outcome.ACTIVE, later.outcome);
        assertEquals(1, later.overdue.size());
        assertTrue(later.upcoming.isEmpty());
    }

    private static HomeDoseFocus.Entry dose(
            String place,
            String slot,
            int hour,
            int minute,
            boolean taken
    ) {
        return new HomeDoseFocus.Entry(place, slot, at(hour, minute), taken);
    }

    private static long at(int hour, int minute) {
        return DAY.atTime(hour, minute).atZone(ZONE).toInstant().toEpochMilli();
    }
}
