package com.example.yakyeon2.fcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.yakyeon2.api.ApiClient;
import com.example.yakyeon2.api.ApiException;
import com.example.yakyeon2.api.AuthApi;
import com.example.yakyeon2.data.AuthTokenStore;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.core.app.NotificationManagerCompat;

public final class FcmRegistrar {

    public static final String TAG = "FCM";
    private static final String PREFS = "yakyeon_fcm";
    private static final String DEVICE = "device_token";
    private static final String REGISTERED = "registered_token";
    private static final String PERMITTED = "registered_permitted";

    private FcmRegistrar() {
    }

    public static void requestToken(Context context) {
        Context app = context.getApplicationContext();
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "토큰 발급 실패", task.getException());
                return;
            }
            String token = task.getResult();
            if (token == null || token.isEmpty()) {
                Log.e(TAG, "토큰 발급 실패", new IllegalStateException("empty token"));
                return;
            }
            Log.d(TAG, "발급된 토큰: " + token);
            storeAndSync(app, token);
        });
    }

    public static void onNewToken(Context context, String token) {
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "토큰 갱신 실패", new IllegalStateException("empty token"));
            return;
        }
        Log.d(TAG, "토큰 갱신: " + token);
        storeAndSync(context.getApplicationContext(), token);
    }

    public static void syncStored(Context context) {
        Context app = context.getApplicationContext();
        ApiClient.execute(() -> registerIfPossible(app));
    }

    /**
     * Best-effort delete. A failure does not stop logout.
     * Must run off the main thread.
     */
    public static void deleteStored(Context context) {
        Context app = context.getApplicationContext();
        String token = deviceTokenValue(app);
        if (token.isEmpty() || !AuthTokenStore.hasSession(app)) {
            return;
        }
        try {
            AuthApi.deletePushToken(app, token);
            markUnregistered(app);
            Log.d(TAG, "토큰 서버 삭제 성공");
        } catch (ApiException exception) {
            if (exception.status() == 404) {
                markUnregistered(app);
                Log.d(TAG, "서버에 등록된 토큰 없음");
                return;
            }
            Log.e(TAG, "토큰 서버 삭제 실패", exception);
        }
    }

    public static void noteSessionEnded(Context context) {
        markUnregistered(context.getApplicationContext());
    }

    private static void storeAndSync(Context app, String token) {
        rememberDeviceToken(app, token);
        syncStored(app);
    }

    private static void registerIfPossible(Context app) {
        String token = deviceTokenValue(app);
        if (token.isEmpty()) {
            Log.d(TAG, "저장된 토큰이 없어 서버 등록을 건너뜀");
            return;
        }
        if (!AuthTokenStore.hasSession(app)) {
            Log.d(TAG, "로그인 상태가 아니어서 서버 등록을 건너뜀");
            return;
        }
        if (!AuthTokenStore.hasUsableAccess(app)) {
            Log.d(TAG, "액세스 토큰 갱신 전이라 서버 등록을 미룸");
            return;
        }
        boolean permitted = NotificationManagerCompat.from(app).areNotificationsEnabled();
        if (token.equals(registeredToken(app)) && samePermission(app, permitted)) {
            return;
        }
        AuthTokenStore.saveFcmToken(app, token);
        try {
            AuthApi.registerPushToken(app, token, permitted);
            prefs(app).edit()
                    .putString(REGISTERED, token)
                    .putBoolean(PERMITTED, permitted)
                    .commit();
            Log.d(TAG, "토큰 서버 등록 성공");
        } catch (ApiException exception) {
            Log.e(TAG, "토큰 서버 등록 실패", exception);
        }
    }

    private static boolean samePermission(Context app, boolean permitted) {
        SharedPreferences stored = prefs(app);
        return stored.contains(PERMITTED) && stored.getBoolean(PERMITTED, false) == permitted;
    }

    private static void rememberDeviceToken(Context app, String token) {
        prefs(app).edit().putString(DEVICE, token).commit();
        if (AuthTokenStore.hasSession(app)) {
            AuthTokenStore.saveFcmToken(app, token);
        }
    }

    private static void markUnregistered(Context app) {
        prefs(app).edit().remove(REGISTERED).remove(PERMITTED).commit();
    }

    public static String deviceToken(Context context) {
        return deviceTokenValue(context.getApplicationContext());
    }

    private static String deviceTokenValue(Context app) {
        String token = prefs(app).getString(DEVICE, "");
        if (token == null || token.isEmpty()) {
            token = AuthTokenStore.fcmToken(app);
        }
        return token == null ? "" : token;
    }

    private static String registeredToken(Context app) {
        String token = prefs(app).getString(REGISTERED, "");
        return token == null ? "" : token;
    }

    private static SharedPreferences prefs(Context app) {
        return app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
