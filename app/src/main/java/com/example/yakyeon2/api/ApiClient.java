package com.example.yakyeon2.api;

import android.content.Context;

import com.example.yakyeon2.data.AuthTokenStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ApiClient {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 20_000;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "yakyeon-api");
        thread.setDaemon(true);
        return thread;
    });
    private static final RefreshGate REFRESH_GATE = new RefreshGate();

    private ApiClient() {
    }

    public static void execute(Runnable work) {
        EXECUTOR.execute(work);
    }

    public static JSONObject call(
            Context context,
            String method,
            String path,
            JSONObject body,
            boolean authenticated
    ) throws ApiException {
        return parseObject(exchange(context, method, path, body, authenticated, true));
    }

    public static JSONArray callArray(
            Context context,
            String method,
            String path,
            JSONObject body,
            boolean authenticated
    ) throws ApiException {
        return parseArray(exchange(context, method, path, body, authenticated, true));
    }

    public static JSONObject callMultipart(
            Context context,
            String path,
            String field,
            String filename,
            String mime,
            byte[] file,
            boolean authenticated
    ) throws ApiException {
        if (field == null || field.isEmpty() || filename == null || filename.isEmpty()
                || mime == null || mime.isEmpty() || file == null) {
            throw ApiException.local();
        }
        String boundary = "yakyeon" + System.nanoTime();
        byte[] payload = multipart(boundary, field, filename, mime, file);
        return parseObject(exchangeMultipart(
                context,
                path,
                "multipart/form-data; boundary=" + boundary,
                payload,
                authenticated,
                true
        ));
    }

    public static void refreshSession(Context context) throws ApiException {
        refreshSession(context, AuthTokenStore.accessToken(context));
    }

    /**
     * {@code accessUsed} is the access token that just failed. If another refresh already
     * replaced it, this call waits for that result and does not send a second refresh.
     * A response that cannot be parsed does not clear the saved session. Network failures
     * and HTTP 5xx do not clear it either. Only {@code INVALID_REFRESH_TOKEN} and
     * {@code REFRESH_TOKEN_REUSED} do.
     */
    static void refreshSession(Context context, String accessUsed) throws ApiException {
        RefreshGate.Outcome outcome;
        try {
            outcome = REFRESH_GATE.enter(() -> sameAccess(context, accessUsed));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw ApiException.local();
        }
        if (!outcome.leader) {
            if (outcome.error != null) {
                throw outcome.error;
            }
            return;
        }
        try {
            performRefresh(context);
            REFRESH_GATE.succeed();
        } catch (ApiException exception) {
            REFRESH_GATE.fail(exception);
            throw exception;
        }
    }

    private static boolean sameAccess(Context context, String accessUsed) {
        String current = AuthTokenStore.accessToken(context);
        if (accessUsed == null) {
            return current == null || current.isEmpty();
        }
        return accessUsed.equals(current);
    }

    private static void performRefresh(Context context) throws ApiException {
        String refreshToken = AuthTokenStore.refreshToken(context);
        if (refreshToken.isEmpty()) {
            throw ApiException.local();
        }
        JSONObject body = new JSONObject();
        try {
            body.put("refresh_token", refreshToken);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        HttpResult result = send("POST", "/auth/refresh", body, null);
        if (result.status >= 200 && result.status < 300) {
            SessionResponse.save(context, parseObject(result.body));
            return;
        }
        ApiException error = errorFrom(result);
        if (error.endsSession()) {
            AuthTokenStore.clear(context);
        }
        throw error;
    }

    private static String exchange(
            Context context,
            String method,
            String path,
            JSONObject body,
            boolean authenticated,
            boolean allowRefresh
    ) throws ApiException {
        String authorization = null;
        String accessUsed = "";
        if (authenticated) {
            accessUsed = AuthTokenStore.accessToken(context);
            if (!accessUsed.isEmpty()) {
                authorization = AuthTokenStore.tokenType(context) + " " + accessUsed;
            }
        }
        HttpResult result = send(method, path, body, authorization);
        if (result.status >= 200 && result.status < 300) {
            return result.body;
        }
        ApiException error = errorFrom(result);
        if (allowRefresh && authenticated && error.hasCode(ApiCodes.UNAUTHENTICATED)) {
            if (AuthTokenStore.refreshToken(context).isEmpty()) {
                AuthTokenStore.clear(context);
                throw error.asSessionEnded();
            }
            refreshSession(context, accessUsed);
            return exchange(context, method, path, body, true, false);
        }
        if (!allowRefresh && authenticated && error.hasCode(ApiCodes.UNAUTHENTICATED)) {
            AuthTokenStore.clear(context);
            throw error.asSessionEnded();
        }
        throw error;
    }

    private static HttpResult send(String method, String path, JSONObject body, String authorization)
            throws ApiException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(ApiConfig.BASE_URL + path).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            if (authorization != null && !authorization.isEmpty()) {
                connection.setRequestProperty("Authorization", authorization);
            }
            if (body != null) {
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new HttpResult(status, read(stream));
        } catch (IOException exception) {
            throw ApiException.disconnected();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String exchangeMultipart(
            Context context,
            String path,
            String contentType,
            byte[] payload,
            boolean authenticated,
            boolean allowRefresh
    ) throws ApiException {
        String authorization = null;
        String accessUsed = "";
        if (authenticated) {
            accessUsed = AuthTokenStore.accessToken(context);
            if (!accessUsed.isEmpty()) {
                authorization = AuthTokenStore.tokenType(context) + " " + accessUsed;
            }
        }
        HttpResult result = sendRaw("POST", path, payload, contentType, authorization);
        if (result.status >= 200 && result.status < 300) {
            return result.body;
        }
        ApiException error = errorFrom(result);
        if (allowRefresh && authenticated && error.hasCode(ApiCodes.UNAUTHENTICATED)) {
            if (AuthTokenStore.refreshToken(context).isEmpty()) {
                AuthTokenStore.clear(context);
                throw error.asSessionEnded();
            }
            refreshSession(context, accessUsed);
            return exchangeMultipart(context, path, contentType, payload, true, false);
        }
        if (!allowRefresh && authenticated && error.hasCode(ApiCodes.UNAUTHENTICATED)) {
            AuthTokenStore.clear(context);
            throw error.asSessionEnded();
        }
        throw error;
    }

    private static HttpResult sendRaw(
            String method,
            String path,
            byte[] payload,
            String contentType,
            String authorization
    ) throws ApiException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(ApiConfig.BASE_URL + path).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            if (authorization != null && !authorization.isEmpty()) {
                connection.setRequestProperty("Authorization", authorization);
            }
            if (payload != null) {
                connection.setRequestProperty("Content-Type", contentType);
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(payload.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(payload);
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new HttpResult(status, read(stream));
        } catch (IOException exception) {
            throw ApiException.disconnected();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static byte[] multipart(
            String boundary,
            String field,
            String filename,
            String mime,
            byte[] file
    ) throws ApiException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            String header = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + filename + "\"\r\n"
                    + "Content-Type: " + mime + "\r\n\r\n";
            output.write(header.getBytes(StandardCharsets.UTF_8));
            output.write(file);
            output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return output.toByteArray();
        } catch (IOException exception) {
            throw ApiException.local();
        }
    }

    private static JSONObject parseObject(String body) throws ApiException {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(body);
        } catch (JSONException exception) {
            throw ApiException.response(200);
        }
    }

    private static JSONArray parseArray(String body) throws ApiException {
        if (body == null || body.trim().isEmpty()) {
            throw ApiException.response(200);
        }
        try {
            return new JSONArray(body);
        } catch (JSONException exception) {
            throw ApiException.response(200);
        }
    }

    private static ApiException errorFrom(HttpResult result) {
        if (result.status >= 500) {
            ParsedError parsed = readError(result.body);
            if (parsed == null) {
                return ApiException.http(result.status, null, "");
            }
            return ApiException.http(result.status, parsed.code, parsed.message);
        }
        ParsedError parsed = readError(result.body);
        if (parsed == null || parsed.code == null) {
            return ApiException.response(result.status);
        }
        return ApiException.http(result.status, parsed.code, parsed.message);
    }

    private static ParsedError readError(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(body);
            String code = json.has("code") && !json.isNull("code") ? json.optString("code", "") : "";
            String message = json.optString("message", "");
            if (code.isEmpty()) {
                return null;
            }
            return new ParsedError(code, message);
        } catch (JSONException exception) {
            return null;
        }
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream input = stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[2048];
            int count;
            while ((count = input.read(chunk)) >= 0) {
                buffer.write(chunk, 0, count);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static final class HttpResult {
        private final int status;
        private final String body;

        private HttpResult(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private static final class ParsedError {
        private final String code;
        private final String message;

        private ParsedError(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
