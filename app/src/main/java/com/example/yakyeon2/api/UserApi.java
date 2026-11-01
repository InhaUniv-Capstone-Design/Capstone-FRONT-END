package com.example.yakyeon2.api;

import android.content.Context;

import com.example.yakyeon2.data.ProfileSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class UserApi {

    private UserApi() {
    }

    public static ProfileSnapshot profile(Context context) throws ApiException {
        return readProfile(ApiClient.call(context, "GET", "/users/me/profile", null, true));
    }

    public static ProfileSnapshot updateName(Context context, String name) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return readProfile(ApiClient.call(context, "PATCH", "/users/me/profile", body, true));
    }

    public static List<NotificationSync.Item> notificationSettings(Context context) throws ApiException {
        JSONObject response = ApiClient.call(
                context,
                "GET",
                "/users/me/notification-settings",
                null,
                true
        );
        return readSettings(response);
    }

    public static List<NotificationSync.Item> updateNotificationSettings(
            Context context,
            List<NotificationSync.Item> changes
    ) throws ApiException {
        JSONObject body = new JSONObject();
        JSONArray settings = new JSONArray();
        try {
            for (NotificationSync.Item change : changes) {
                JSONObject item = new JSONObject();
                item.put("setting_key", change.key);
                item.put("enabled", change.enabled);
                settings.put(item);
            }
            body.put("settings", settings);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return readSettings(ApiClient.call(context, "PATCH", "/users/me/notification-settings", body, true));
    }

    private static ProfileSnapshot readProfile(JSONObject response) throws ApiException {
        if (response == null) {
            throw ApiException.response(200);
        }
        if (!response.has("user_id") || response.isNull("user_id")) {
            throw ApiException.response(200);
        }
        long userId = response.optLong("user_id", 0L);
        if (userId <= 0L) {
            throw ApiException.response(200);
        }
        String accountType = requiredText(response, "account_type");
        if (com.example.yakyeon2.data.AccountRoles.toLocal(accountType) == null) {
            throw ApiException.response(200);
        }
        String birthDate = requiredText(response, "birth_date");
        if (!birthDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw ApiException.response(200);
        }
        return new ProfileSnapshot(
                userId,
                requiredText(response, "login_id"),
                requiredText(response, "name"),
                birthDate,
                requiredText(response, "phone_masked"),
                accountType
        );
    }

    private static String requiredText(JSONObject response, String key) throws ApiException {
        if (!response.has(key) || response.isNull(key)) {
            throw ApiException.response(200);
        }
        String value = response.optString(key, "");
        if (value.isEmpty()) {
            throw ApiException.response(200);
        }
        return value;
    }

    static List<NotificationSync.Item> readSettings(JSONObject response) throws ApiException {
        if (response == null) {
            throw ApiException.response(200);
        }
        JSONArray settings = response.optJSONArray("settings");
        if (settings == null) {
            throw ApiException.response(200);
        }
        boolean[] enabled = new boolean[NotificationSync.keys().length];
        boolean[] seen = new boolean[NotificationSync.keys().length];
        for (int index = 0; index < settings.length(); index++) {
            JSONObject item = settings.optJSONObject(index);
            if (item == null || !item.has("setting_key") || item.isNull("setting_key")
                    || !item.has("enabled") || item.isNull("enabled")) {
                throw ApiException.response(200);
            }
            int keyIndex = NotificationSync.indexOf(item.optString("setting_key", ""));
            if (keyIndex < 0 || seen[keyIndex]) {
                throw ApiException.response(200);
            }
            try {
                enabled[keyIndex] = item.getBoolean("enabled");
            } catch (JSONException exception) {
                throw ApiException.response(200);
            }
            seen[keyIndex] = true;
        }
        List<NotificationSync.Item> items = new ArrayList<>();
        String[] keys = NotificationSync.keys();
        for (int index = 0; index < keys.length; index++) {
            if (!seen[index]) {
                throw ApiException.response(200);
            }
            items.add(new NotificationSync.Item(keys[index], enabled[index]));
        }
        return items;
    }
}
