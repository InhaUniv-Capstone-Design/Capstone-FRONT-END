package com.example.yakyeon2.api;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Reads the account notification list and marks one row read.
 * The lock-screen payload only carries a title, a body, and this id.
 */
public final class NotificationInbox {

    public static final class Item {
        public final String id;
        public final String title;
        public final String body;

        private Item(String id, String title, String body) {
            this.id = id;
            this.title = title;
            this.body = body;
        }
    }

    private NotificationInbox() {
    }

    public static Item find(Context context, String notificationId) throws ApiException {
        if (notificationId == null || notificationId.trim().isEmpty()) {
            throw ApiException.local();
        }
        String wanted = notificationId.trim();
        JSONArray array = ApiClient.callArray(context, "GET", "/users/me/notifications", null, true);
        if (array == null) {
            throw ApiException.response(200);
        }
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                continue;
            }
            String id = idOf(item);
            if (!wanted.equals(id)) {
                continue;
            }
            return new Item(
                    id,
                    JsonReads.optionalText(item, "title"),
                    JsonReads.optionalText(item, "body")
            );
        }
        return null;
    }

    public static void markRead(Context context, String notificationId) throws ApiException {
        String id = notificationId == null ? "" : notificationId.trim();
        if (id.isEmpty()) {
            throw ApiException.local();
        }
        ApiClient.call(context, "POST", "/notifications/" + encode(id) + "/read", null, true);
    }

    private static String idOf(JSONObject item) {
        if (item == null || !item.has("notification_id") || item.isNull("notification_id")) {
            return "";
        }
        Object value = item.opt("notification_id");
        if (value instanceof Number) {
            long id = ((Number) value).longValue();
            return id <= 0L ? "" : Long.toString(id);
        }
        if (value instanceof String) {
            return ((String) value).trim();
        }
        return "";
    }

    private static String encode(String value) throws ApiException {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw ApiException.local();
        }
    }
}
