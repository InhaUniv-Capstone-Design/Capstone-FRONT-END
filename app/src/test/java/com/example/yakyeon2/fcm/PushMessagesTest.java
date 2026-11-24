package com.example.yakyeon2.fcm;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PushMessagesTest {

    @Test
    public void eightKindsUseThePayloadType() {
        assertEquals(PushMessages.Kind.DOSE_REMINDER, classify("DOSE_REMINDER").kind);
        assertEquals(PushMessages.Kind.MISSED_DOSE, classify("MISSED").kind);
        assertEquals(PushMessages.Kind.END_SOON, classify("END_SOON").kind);
        assertEquals(Integer.valueOf(3), classify("END_SOON").daysLeft);
        assertEquals(Integer.valueOf(1), classify("END_SOON_1").daysLeft);
        assertEquals(PushMessages.Kind.BOX_TAKEN, classify("BUTTON_PRESS").kind);
        assertEquals(PushMessages.Kind.BOX_MISSED, classify("DEVICE_MISSED").kind);
        assertEquals(PushMessages.Kind.DEVICE_FAULT, classify("DEVICE_ERROR").kind);
        assertEquals(PushMessages.Kind.PATIENT_LINKED, classify("PATIENT_LINKED").kind);
    }

    @Test
    public void endSoonKeepsTheHospitalFromThePayload() {
        Map<String, String> data = new HashMap<>();
        data.put("type", "END_SOON");
        data.put("days_left", "1");
        data.put("hospital_name", "한의원");
        PushMessages.Incoming incoming = PushMessages.classify(data);
        assertEquals("한의원", incoming.hospitalName);
        assertEquals(Integer.valueOf(1), incoming.daysLeft);
        assertFalse(incoming.suppressBecauseTaken);
    }

    @Test
    public void missedNoticeIsSuppressedAfterTheDoseIsTaken() {
        Map<String, String> data = new HashMap<>();
        data.put("type", "MISSED");
        data.put("status", "NORMAL");
        data.put("notification_id", "15");
        PushMessages.Incoming incoming = PushMessages.classify(data);
        assertTrue(incoming.suppressBecauseTaken);

        data.put("type", "DEVICE_MISSED");
        data.put("status", "PENDING");
        data.put("taken_at", "2026-11-08T12:40:00+09:00");
        assertTrue(PushMessages.classify(data).suppressBecauseTaken);
    }

    @Test
    public void takenConfirmationIsNotSuppressed() {
        Map<String, String> data = new HashMap<>();
        data.put("type", "BUTTON_PRESS");
        data.put("status", "NORMAL");
        assertFalse(PushMessages.classify(data).suppressBecauseTaken);
    }

    @Test
    public void unknownPayloadIsLeftForTheServerText() {
        Map<String, String> data = new HashMap<>();
        data.put("type", "GUARDIAN_OPEN");
        data.put("body", "약통이 열렸습니다.");
        assertEquals(PushMessages.Kind.PASSTHROUGH, PushMessages.classify(data).kind);
        assertTrue(PushMessages.hidesOpenNotice(data, data.get("body")));
    }

    @Test
    public void intakeDetectedReloadsAndOtherNoticesDoNot() {
        Map<String, String> intake = new HashMap<>();
        intake.put("notif_type", "INTAKE_DETECTED");
        intake.put("notification_id", "8");
        assertEquals(PushMessages.Kind.BOX_TAKEN, PushMessages.classify(intake).kind);

        Map<String, String> opened = new HashMap<>();
        opened.put("notif_type", "DEVICE_OPENED");
        assertEquals(PushMessages.Kind.PASSTHROUGH, PushMessages.classify(opened).kind);
        assertTrue(PushMessages.hidesOpenNotice(opened));
        assertEquals(PushMessages.Kind.DOSE_REMINDER, classify("DOSE_REMINDER").kind);

        assertTrue(PushMessages.confirmsIntake("복용이 확인됐어요"));
        assertTrue(PushMessages.confirmsIntake(null, " 복용이 확인됐어요 "));
        assertFalse(PushMessages.confirmsIntake("약 드셨나요?"));
        assertFalse(PushMessages.confirmsIntake("약 먹을 시간이에요"));
        assertFalse(PushMessages.confirmsIntake("약통이 열렸습니다."));
        assertFalse(PushMessages.confirmsIntake((String[]) null));
    }

    @Test
    public void irTriggerRefreshesThePillboxWithoutCountingAsIntake() {
        Map<String, String> sensed = new HashMap<>();
        sensed.put("type", "IR_TRIGGER");
        assertEquals(PushMessages.Kind.PASSTHROUGH, PushMessages.classify(sensed).kind);
        assertTrue(PushMessages.refreshesPillbox(sensed));

        sensed.put("type", "DEVICE");
        sensed.put("signal_type", "ir_trigger");
        assertEquals(PushMessages.Kind.PASSTHROUGH, PushMessages.classify(sensed).kind);
        assertTrue(PushMessages.refreshesPillbox(sensed));

        Map<String, String> button = new HashMap<>();
        button.put("type", "BUTTON_PRESS");
        assertFalse(PushMessages.refreshesPillbox(button));

        Map<String, String> opened = new HashMap<>();
        opened.put("notif_type", "DEVICE_OPENED");
        assertFalse(PushMessages.refreshesPillbox(opened));
        assertTrue(PushMessages.hidesOpenNotice(opened));
        assertFalse(PushMessages.refreshesPillbox(null));
        assertTrue(PushMessages.hidesOpenNotice(sensed));
        Map<String, String> silent = new HashMap<>();
        silent.put("type", "IR_TRIGGER");
        assertTrue(PushMessages.refreshesPillbox(silent));
        assertTrue(PushMessages.hidesOpenNotice(silent));
        assertTrue(PushMessages.hidesOpenNotice(silent, "약통이 열렸습니다."));
        assertTrue(PushMessages.hidesOpenNotice(null, "약통이 열렸습니다."));
        assertFalse(PushMessages.hidesOpenNotice(button, "복용이 확인됐어요"));
    }

    @Test
    public void newerOpenCopyStaysHiddenAndIntakeConfirmationStaysVisible() {
        assertTrue(PushMessages.mentionsOpen("약통이 열렸어요"));
        assertTrue(PushMessages.mentionsOpen("약통이 열렸지만 복용 확인 버튼은 눌리지 않았어요."));
        assertTrue(PushMessages.hidesOpenNotice(null, "약통이 열렸어요"));
        assertTrue(PushMessages.hidesOpenNotice(
                null,
                "약통이 열렸어요",
                "약통이 열렸지만 복용 확인 버튼은 눌리지 않았어요."
        ));

        Map<String, String> opened = new HashMap<>();
        opened.put("title", "약통이 열렸어요");
        opened.put("body", "약통이 열렸지만 복용 확인 버튼은 눌리지 않았어요.");
        assertTrue(PushMessages.hidesOpenNotice(opened));

        assertFalse(PushMessages.mentionsOpen("복용이 확인됐어요", "약통의 복용 버튼이 눌렸어요."));
        assertFalse(PushMessages.hidesOpenNotice(null, "복용이 확인됐어요", "약통의 복용 버튼이 눌렸어요."));
        Map<String, String> taken = new HashMap<>();
        taken.put("title", "복용이 확인됐어요");
        taken.put("body", "약통의 복용 버튼이 눌렸어요.");
        assertFalse(PushMessages.hidesOpenNotice(taken));
    }

    @Test
    public void theSameNotificationIsAcceptedOnce() {
        Map<String, String> data = new HashMap<>();
        data.put("type", "DOSE_REMINDER");
        data.put("notification_id", "9");
        String key = PushMessages.classify(data).dedupeKey;
        List<String> recent = new ArrayList<>();
        assertTrue(PushMessages.accept(recent, key));
        assertFalse(PushMessages.accept(recent, key));
    }

    private static PushMessages.Incoming classify(String type) {
        Map<String, String> data = new HashMap<>();
        data.put("type", type);
        return PushMessages.classify(data);
    }
}
