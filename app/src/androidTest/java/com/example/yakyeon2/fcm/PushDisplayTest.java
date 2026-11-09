package com.example.yakyeon2.fcm;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.yakyeon2.R;
import com.google.firebase.messaging.RemoteMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Delivers data messages through {@link YakyeonMessagingService} and checks the
 * shade. Timing (10 minutes before, 30 minutes after, 3 days and 1 day before
 * the end) is not scheduled on the device; the server is expected to send the
 * matching type when that moment arrives.
 */
@RunWith(AndroidJUnit4.class)
public class PushDisplayTest {

    private Context context;
    private NotificationManager notifications;

    @Before
    public void resetShade() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(context.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
        notifications = context.getSystemService(NotificationManager.class);
        notifications.cancelAll();
        context.getSharedPreferences("yakyeon_push_seen", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void eachNoticeShowsTheTemporaryCopy() throws Exception {
        deliver(serverPayload("101", "복약 알림", "약 먹을 시간이에요"));
        deliver(serverPayload("102", "복약 알림", "복용을 확인해 주세요"));
        deliver(payload("105", "BUTTON_PRESS", ""));
        Map<String, String> endSoon = payload("103", "END_SOON", "");
        endSoon.put("hospital_name", "서울병원");
        deliver(endSoon);

        List<String> bodies = postedBodies();
        assertTrue(bodies.contains("약 먹을 시간이에요"));
        assertTrue(bodies.contains("복용을 확인해 주세요"));
        assertTrue(bodies.contains(context.getString(R.string.push_box_taken)));
        assertTrue(bodies.contains(context.getString(R.string.push_end_soon_generic)));
        assertFalse(bodies.contains("서울병원"));
    }

    @Test
    public void missedNoticeStaysDownWhenTheDoseIsAlreadyTaken() throws Exception {
        Map<String, String> taken = payload("201", "MISSED", "약 드셨나요?");
        taken.put("status", "TAKEN");
        deliver(taken);
        Map<String, String> boxTaken = payload("202", "BOX_MISSED", "약 드셨나요?");
        boxTaken.put("taken", "true");
        deliver(boxTaken);
        assertTrue(postedBodies().isEmpty());
    }

    @Test
    public void endSoonWithoutServerTextUsesAGenericLine() throws Exception {
        Map<String, String> missing = payload("301", "END_SOON", "");
        missing.put("days_left", "3");
        missing.put("hospital_name", "서울병원");
        deliver(missing);
        List<String> bodies = postedBodies();
        assertEquals(1, bodies.size());
        assertEquals(context.getString(R.string.push_end_soon_generic), bodies.get(0));
        assertFalse(bodies.get(0).contains("서울병원"));
    }

    @Test
    public void theSameNotificationIsShownOnce() throws Exception {
        deliver(payload("401", "DOSE_REMINDER", ""));
        Map<String, String> replay = payload("401", "DEVICE_ERROR", "");
        deliver(replay);
        List<String> bodies = postedBodies();
        assertEquals(1, bodies.size());
        assertEquals(context.getString(R.string.push_dose_reminder), bodies.get(0));
    }

    private static Map<String, String> serverPayload(String id, String title, String body) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("notification_id", id);
        data.put("title", title);
        data.put("body", body);
        return data;
    }

    private static Map<String, String> payload(String id, String type, String body) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("notification_id", id);
        data.put("type", type);
        if (body != null && !body.isEmpty()) {
            data.put("body", body);
        }
        return data;
    }

    private void deliver(Map<String, String> data) throws Exception {
        YakyeonMessagingService service = new YakyeonMessagingService();
        Method attach = ContextWrapper.class.getDeclaredMethod("attachBaseContext", Context.class);
        attach.setAccessible(true);
        attach.invoke(service, context);
        service.onCreate();
        RemoteMessage.Builder builder = new RemoteMessage.Builder("push-display-test");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            builder.addData(entry.getKey(), entry.getValue());
        }
        service.onMessageReceived(builder.build());
    }

    private List<String> postedBodies() {
        List<String> bodies = new ArrayList<>();
        for (StatusBarNotification posted : notifications.getActiveNotifications()) {
            Notification notification = posted.getNotification();
            CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
            if (text != null) {
                bodies.add(text.toString());
            }
        }
        return bodies;
    }

    private static int count(List<String> bodies, String expected) {
        int found = 0;
        for (String body : bodies) {
            if (expected.equals(body)) {
                found++;
            }
        }
        return found;
    }
}
