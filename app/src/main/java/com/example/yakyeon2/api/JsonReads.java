package com.example.yakyeon2.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class JsonReads {

    private JsonReads() {
    }

    static JSONObject object(JSONObject json) throws ApiException {
        if (json == null) {
            throw ApiException.response(200);
        }
        return json;
    }

    static String text(JSONObject json, String key) throws ApiException {
        String value = optionalText(json, key);
        if (value == null || value.isEmpty()) {
            throw ApiException.response(200);
        }
        return value;
    }

    static String optionalText(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key)) {
            return null;
        }
        Object value = json.opt(key);
        if (!(value instanceof String)) {
            throw ApiException.response(200);
        }
        return (String) value;
    }

    static long positiveId(JSONObject json, String key) throws ApiException {
        long id = number(json, key);
        if (id <= 0L) {
            throw ApiException.response(200);
        }
        return id;
    }

    static Long optionalPositiveId(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key)) {
            return null;
        }
        long id = number(json, key);
        if (id <= 0L) {
            throw ApiException.response(200);
        }
        return id;
    }

    static long number(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key) || !(json.opt(key) instanceof Number)) {
            throw ApiException.response(200);
        }
        return ((Number) json.opt(key)).longValue();
    }

    static String decimal(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key) || !(json.opt(key) instanceof Number)) {
            throw ApiException.response(200);
        }
        return plainDecimal((Number) json.opt(key));
    }

    static String optionalDecimal(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key)) {
            return null;
        }
        if (!(json.opt(key) instanceof Number)) {
            throw ApiException.response(200);
        }
        return plainDecimal((Number) json.opt(key));
    }

    private static String plainDecimal(Number number) throws ApiException {
        try {
            return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException exception) {
            throw ApiException.response(200);
        }
    }

    static Long optionalNumber(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key)) {
            return null;
        }
        if (!(json.opt(key) instanceof Number)) {
            throw ApiException.response(200);
        }
        return ((Number) json.opt(key)).longValue();
    }

    static boolean bool(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key) || !(json.opt(key) instanceof Boolean)) {
            throw ApiException.response(200);
        }
        return (Boolean) json.opt(key);
    }

    static Boolean optionalBool(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key)) {
            return null;
        }
        if (!(json.opt(key) instanceof Boolean)) {
            throw ApiException.response(200);
        }
        return (Boolean) json.opt(key);
    }

    static List<String> optionalStrings(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key)) {
            return null;
        }
        Object value = json.opt(key);
        if (!(value instanceof JSONArray)) {
            throw ApiException.response(200);
        }
        JSONArray array = (JSONArray) value;
        List<String> items = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            Object item = array.opt(index);
            if (!(item instanceof String) || ((String) item).isEmpty()) {
                throw ApiException.response(200);
            }
            items.add((String) item);
        }
        return items;
    }
}
