package com.example.yakyeon2.fcm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.yakyeon2.R;
import com.example.yakyeon2.data.AuthTokenStore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class YakyeonMessagingService extends FirebaseMessagingService {

    private static final String PREFS = "yakyeon_push_seen";
    private static final String RECENT = "recent";

    @Override
    public void onCreate() {
        super.onCreate();
        YakyeonNotifications.ensureChannel(this);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        FcmRegistrar.onNewToken(this, token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        RemoteMessage.Notification notification = message.getNotification();
        Log.i("BoxTaken", "onMessageReceived notification=" + (notification != null)
                + " dataKeys=" + dataKeyNames(data));
        if (notification != null && data.isEmpty()) {
            Log.d(FcmRegistrar.TAG, "알림 메시지 수신");
        } else if (notification != null) {
            Log.d(FcmRegistrar.TAG, "알림+데이터 메시지 수신");
        } else {
            Log.d(FcmRegistrar.TAG, "데이터 메시지 수신");
        }
        String dataTitle = data.get("title");
        String dataBody = data.get("body");
        boolean serverCopy = hasText(dataTitle) || hasText(dataBody);
        String title = serverCopy ? dataTitle : (notification == null ? null : notification.getTitle());
        String body = serverCopy ? dataBody : (notification == null ? null : notification.getBody());
        PushMessages.Incoming incoming = PushMessages.classify(data);
        boolean signedIn = AuthTokenStore.hasSession(this);
        Log.i("BoxTaken", "classify kind=" + incoming.kind
                + " type=" + safeType(data)
                + " status=" + safeField(data, "status")
                + " taken_at=" + safeField(data, "taken_at")
                + " dedupe=" + incoming.dedupeKey
                + " suppress=" + incoming.suppressBecauseTaken
                + " hasSession=" + signedIn);
        if (incoming.suppressBecauseTaken) {
            Log.d(FcmRegistrar.TAG, "이미 복용한 일정은 미복용 알림을 표시하지 않음");
            Log.i("BoxTaken", "stop suppressBecauseTaken kind=" + incoming.kind);
            return;
        }
        List<String> recent = recentKeys(this);
        if (!PushMessages.accept(recent, incoming.dedupeKey)) {
            Log.d(FcmRegistrar.TAG, "같은 알림은 다시 표시하지 않음");
            Log.i("BoxTaken", "stop duplicate dedupe=" + incoming.dedupeKey);
            return;
        }
        remember(this, recent);
        if (BoxTakenRefresh.requestsReload(incoming.kind, signedIn)) {
            Log.i("BoxTaken", "post BoxTakenRefresh.deliver");
            new Handler(Looper.getMainLooper()).post(BoxTakenRefresh::deliver);
        } else {
            Log.i("BoxTaken", "skip deliver kind=" + incoming.kind + " hasSession=" + signedIn);
        }
        String shownTitle = title;
        String shownBody = body;
        if (!serverCopy) {
            shownBody = copyFor(incoming, body);
            shownTitle = incoming.kind == PushMessages.Kind.PASSTHROUGH ? title : null;
        }
        if (shownBody == null && !hasText(shownTitle)) {
            Log.d(FcmRegistrar.TAG, "표시할 제목과 내용이 없음");
            return;
        }
        YakyeonNotifications.show(this, shownTitle, shownBody, data.get("notification_id"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String dataKeyNames(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        StringBuilder names = new StringBuilder();
        for (String key : data.keySet()) {
            if (names.length() > 0) {
                names.append(',');
            }
            names.append(key);
        }
        return names.toString();
    }

    private static String safeType(Map<String, String> data) {
        String type = safeField(data, "type");
        if (!type.isEmpty()) {
            return type;
        }
        type = safeField(data, "notification_type");
        if (!type.isEmpty()) {
            return "notification_type:" + type;
        }
        type = safeField(data, "event");
        if (!type.isEmpty()) {
            return "event:" + type;
        }
        type = safeField(data, "kind");
        if (!type.isEmpty()) {
            return "kind:" + type;
        }
        type = safeField(data, "setting_key");
        return type.isEmpty() ? "" : "setting_key:" + type;
    }

    private static String safeField(Map<String, String> data, String key) {
        if (data == null || key == null) {
            return "";
        }
        String value = data.get(key);
        return value == null ? "" : value.trim();
    }

    private String copyFor(PushMessages.Incoming incoming, String serverBody) {
        switch (incoming.kind) {
            case DOSE_REMINDER:
                return getString(R.string.push_dose_reminder);
            case MISSED_DOSE:
            case BOX_MISSED:
                return getString(R.string.push_missed);
            case BOX_TAKEN:
                return getString(R.string.push_box_taken);
            case DEVICE_FAULT:
                return getString(R.string.push_device_fault);
            case PATIENT_LINKED:
                return getString(R.string.push_patient_linked);
            case END_SOON:
                if (serverBody != null && !serverBody.trim().isEmpty()) {
                    return serverBody;
                }
                return getString(R.string.push_end_soon_generic);
            case PASSTHROUGH:
            default:
                return serverBody;
        }
    }

    private static List<String> recentKeys(Context context) {
        String stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(RECENT, "");
        List<String> keys = new ArrayList<>();
        if (stored == null || stored.isEmpty()) {
            return keys;
        }
        keys.addAll(Arrays.asList(stored.split("\n")));
        return keys;
    }

    private static void remember(Context context, List<String> keys) {
        StringBuilder stored = new StringBuilder();
        for (String key : keys) {
            if (key == null || key.isEmpty()) {
                continue;
            }
            if (stored.length() > 0) {
                stored.append('\n');
            }
            stored.append(key);
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(RECENT, stored.toString())
                .apply();
    }
}
