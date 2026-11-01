package com.example.yakyeon2.api;

import android.content.Context;

import com.example.yakyeon2.data.AccountRoles;
import com.example.yakyeon2.data.AuthTokenStore;

import org.json.JSONObject;

public final class SessionResponse {

    private SessionResponse() {
    }

    public static void save(Context context, JSONObject json) throws ApiException {
        if (json == null) {
            throw ApiException.response(200);
        }
        String access = text(json, "access_token");
        String refresh = text(json, "refresh_token");
        String tokenType = text(json, "token_type");
        if (!"Bearer".equals(tokenType)) {
            throw ApiException.response(200);
        }
        if (!json.has("expires_in") || json.isNull("expires_in")) {
            throw ApiException.response(200);
        }
        long expiresIn = json.optLong("expires_in", 0L);
        if (expiresIn <= 0L) {
            throw ApiException.response(200);
        }
        if (!json.has("user_id") || json.isNull("user_id")) {
            throw ApiException.response(200);
        }
        long userId = json.optLong("user_id", 0L);
        if (userId <= 0L) {
            throw ApiException.response(200);
        }
        String accountType = text(json, "account_type");
        if (AccountRoles.toLocal(accountType) == null) {
            throw ApiException.response(200);
        }
        AuthTokenStore.saveSession(context, access, refresh, tokenType, expiresIn, userId, accountType);
    }

    private static String text(JSONObject json, String key) throws ApiException {
        if (!json.has(key) || json.isNull(key)) {
            throw ApiException.response(200);
        }
        String value = json.optString(key, "");
        if (value.isEmpty()) {
            throw ApiException.response(200);
        }
        return value;
    }
}
