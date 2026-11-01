package com.example.yakyeon2.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class AuthTokenStore {

    private static final String FILE = "yakyeon_auth";
    private static final String ACCESS = "access_token";
    private static final String REFRESH = "refresh_token";
    private static final String TOKEN_TYPE = "token_type";
    private static final String EXPIRES_AT = "expires_at";
    private static final String USER_ID = "user_id";
    private static final String ACCOUNT_TYPE = "account_type";
    private static final String PROFILE_LOGIN_ID = "profile_login_id";
    private static final String PROFILE_NAME = "profile_name";
    private static final String PROFILE_BIRTH = "profile_birth_date";
    private static final String PROFILE_PHONE = "profile_phone_masked";
    private static final String FCM_TOKEN = "fcm_token";
    private static final long SKEW_MS = 60_000L;

    private AuthTokenStore() {
    }

    public static void saveSession(
            Context context,
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds,
            long userId,
            String accountType
    ) {
        long expiresAt = expiresInSeconds > 0
                ? System.currentTimeMillis() + expiresInSeconds * 1000L
                : 0L;
        preferences(context).edit()
                .putString(ACCESS, accessToken)
                .putString(REFRESH, refreshToken)
                .putString(TOKEN_TYPE, tokenType == null || tokenType.isEmpty() ? "Bearer" : tokenType)
                .putLong(EXPIRES_AT, expiresAt)
                .putLong(USER_ID, userId)
                .putString(ACCOUNT_TYPE, accountType == null ? "" : accountType)
                .commit();
    }

    public static void saveProfile(Context context, ProfileSnapshot profile) {
        if (profile == null) {
            return;
        }
        preferences(context).edit()
                .putLong(USER_ID, profile.userId)
                .putString(ACCOUNT_TYPE, profile.accountType == null ? "" : profile.accountType)
                .putString(PROFILE_LOGIN_ID, profile.loginId == null ? "" : profile.loginId)
                .putString(PROFILE_NAME, profile.name == null ? "" : profile.name)
                .putString(PROFILE_BIRTH, profile.birthDate == null ? "" : profile.birthDate)
                .putString(PROFILE_PHONE, profile.phoneMasked == null ? "" : profile.phoneMasked)
                .commit();
    }

    public static ProfileSnapshot profile(Context context) {
        SharedPreferences preferences = preferences(context);
        String name = preferences.getString(PROFILE_NAME, "");
        String birth = preferences.getString(PROFILE_BIRTH, "");
        String phone = preferences.getString(PROFILE_PHONE, "");
        String loginId = preferences.getString(PROFILE_LOGIN_ID, "");
        if (name.isEmpty() && birth.isEmpty() && phone.isEmpty() && loginId.isEmpty()) {
            return null;
        }
        return new ProfileSnapshot(
                preferences.getLong(USER_ID, 0L),
                loginId,
                name,
                birth,
                phone,
                preferences.getString(ACCOUNT_TYPE, "")
        );
    }

    public static String accessToken(Context context) {
        return preferences(context).getString(ACCESS, "");
    }

    public static String refreshToken(Context context) {
        return preferences(context).getString(REFRESH, "");
    }

    public static String tokenType(Context context) {
        String type = preferences(context).getString(TOKEN_TYPE, "Bearer");
        return type == null || type.isEmpty() ? "Bearer" : type;
    }

    public static String accountType(Context context) {
        return preferences(context).getString(ACCOUNT_TYPE, "");
    }

    public static boolean hasSession(Context context) {
        return !refreshToken(context).isEmpty() || !accessToken(context).isEmpty();
    }

    public static boolean hasRefreshToken(Context context) {
        return !refreshToken(context).isEmpty();
    }

    public static boolean hasUsableAccess(Context context) {
        if (accessToken(context).isEmpty()) {
            return false;
        }
        long expiresAt = preferences(context).getLong(EXPIRES_AT, 0L);
        return expiresAt > System.currentTimeMillis() + SKEW_MS;
    }

    public static void saveFcmToken(Context context, String fcmToken) {
        preferences(context).edit()
                .putString(FCM_TOKEN, fcmToken == null ? "" : fcmToken)
                .commit();
    }

    public static String fcmToken(Context context) {
        String token = preferences(context).getString(FCM_TOKEN, "");
        return token == null ? "" : token;
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
