package com.example.yakyeon2.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class AuthContracts {

    public static final String TOS = "TOS";
    public static final String PRIVACY = "PRIVACY";
    public static final String SERVICE_NOTICE = "SERVICE_NOTICE";
    public static final String SENSITIVE_HEALTH = "SENSITIVE_HEALTH";

    private static final String[] SCREEN_TYPES = {TOS, PRIVACY, SERVICE_NOTICE, SENSITIVE_HEALTH};

    private AuthContracts() {
    }

    public enum TermsFit {
        READY,
        MISSING,
        EXTRA_REQUIRED
    }

    public static final class PreparedTerms {
        public final TermsFit fit;
        public final long tosId;
        public final long privacyId;
        public final long noticeId;
        public final long sensitiveId;
        public final String tos;
        public final String privacy;
        public final String notice;
        public final String sensitive;

        private PreparedTerms(
                TermsFit fit,
                long tosId,
                long privacyId,
                long noticeId,
                long sensitiveId,
                String tos,
                String privacy,
                String notice,
                String sensitive
        ) {
            this.fit = fit;
            this.tosId = tosId;
            this.privacyId = privacyId;
            this.noticeId = noticeId;
            this.sensitiveId = sensitiveId;
            this.tos = tos;
            this.privacy = privacy;
            this.notice = notice;
            this.sensitive = sensitive;
        }
    }

    public static final class PhoneConfirm {
        public final boolean verified;
        public final String token;
        public final Long tokenExpiresIn;

        private PhoneConfirm(boolean verified, String token, Long tokenExpiresIn) {
            this.verified = verified;
            this.token = token;
            this.tokenExpiresIn = tokenExpiresIn;
        }
    }

    /**
     * Phone verification start. {@code messageText} is present only when the server
     * expects the user to text that code to {@code messageTo}. A response without it
     * is the older code-entry flow.
     */
    public static final class PhoneVerification {
        public final long verificationId;
        public final long expiresIn;
        public final String messageText;
        public final String messageTo;

        private PhoneVerification(long verificationId, long expiresIn, String messageText, String messageTo) {
            this.verificationId = verificationId;
            this.expiresIn = expiresIn;
            this.messageText = messageText;
            this.messageTo = messageTo;
        }

        public boolean outboundSms() {
            return messageText != null;
        }
    }

    public static String displayMessageTo(String digits) {
        if (digits != null && digits.length() == 8) {
            return digits.substring(0, 4) + " " + digits.substring(4);
        }
        return digits == null ? "" : digits;
    }

    public static boolean callsServerLogout(String refreshToken) {
        return refreshToken != null && !refreshToken.isEmpty();
    }

    public static String phoneDigits(String raw) {
        if (raw == null) {
            return null;
        }
        StringBuilder digits = new StringBuilder();
        for (int index = 0; index < raw.length(); index++) {
            char c = raw.charAt(index);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        String phone = digits.toString();
        if (!phone.matches("010\\d{8}")) {
            return null;
        }
        return phone;
    }

    public static String isoDate(String display) {
        if (display == null) {
            return null;
        }
        String trimmed = display.trim();
        String iso = trimmed.replace('.', '-').replace('/', '-');
        if (!iso.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return null;
        }
        try {
            return LocalDate.parse(iso).toString();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    public static boolean isVerificationCode(String code) {
        return code != null && code.matches("\\d{6}");
    }

    public static PreparedTerms readTerms(JSONArray array) throws ApiException {
        if (array == null) {
            throw ApiException.response(200);
        }
        long[] ids = new long[SCREEN_TYPES.length];
        String[] bodies = new String[SCREEN_TYPES.length];
        boolean[] seen = new boolean[SCREEN_TYPES.length];
        boolean extraRequired = false;
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            String type = JsonReads.text(item, "terms_type");
            long id = JsonReads.positiveId(item, "terms_version_id");
            JsonReads.text(item, "version");
            String content = JsonReads.text(item, "content");
            boolean required = JsonReads.bool(item, "is_required");
            int slot = screenSlot(type);
            if (slot < 0) {
                if (required) {
                    extraRequired = true;
                }
                continue;
            }
            if (seen[slot]) {
                throw ApiException.response(200);
            }
            seen[slot] = true;
            ids[slot] = id;
            bodies[slot] = content;
        }
        boolean missing = false;
        for (boolean present : seen) {
            if (!present) {
                missing = true;
                break;
            }
        }
        TermsFit fit = TermsFit.READY;
        if (extraRequired) {
            fit = TermsFit.EXTRA_REQUIRED;
        } else if (missing) {
            fit = TermsFit.MISSING;
        }
        return new PreparedTerms(
                fit,
                ids[0],
                ids[1],
                ids[2],
                ids[3],
                bodies[0],
                bodies[1],
                bodies[2],
                bodies[3]
        );
    }

    public static JSONObject phoneBody(String phone) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("phone", phone);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return body;
    }

    public static long readVerificationId(JSONObject response) throws ApiException {
        return readPhoneVerification(response).verificationId;
    }

    public static PhoneVerification readPhoneVerification(JSONObject response) throws ApiException {
        JsonReads.object(response);
        long expires = JsonReads.number(response, "expires_in");
        if (expires < 1L) {
            throw ApiException.response(200);
        }
        long id = JsonReads.positiveId(response, "verification_id");
        String messageText = JsonReads.optionalText(response, "message_text");
        String messageTo = JsonReads.optionalText(response, "message_to");
        if (messageText == null && messageTo == null) {
            return new PhoneVerification(id, expires, null, null);
        }
        if (messageText == null || messageTo == null
                || !messageText.matches("\\d{6}")
                || !messageTo.matches("\\d+")) {
            throw ApiException.response(200);
        }
        return new PhoneVerification(id, expires, messageText, messageTo);
    }

    public static JSONObject confirmBody(String code) throws ApiException {
        return confirmBody(code, null);
    }

    public static JSONObject confirmBody(String code, String phone) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("code", code);
            if (phone != null && !phone.isEmpty()) {
                body.put("phone", phone);
            }
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return body;
    }

    public static PhoneConfirm readPhoneConfirm(JSONObject response) throws ApiException {
        JsonReads.object(response);
        boolean verified = JsonReads.bool(response, "verified");
        if (!verified) {
            return new PhoneConfirm(false, null, null);
        }
        String token = JsonReads.text(response, "verification_token");
        long expires = JsonReads.number(response, "token_expires_in");
        if (expires < 1L) {
            throw ApiException.response(200);
        }
        return new PhoneConfirm(true, token, expires);
    }

    public static String checkIdPath(String loginId) throws ApiException {
        try {
            return "/auth/check-id?login_id=" + URLEncoder.encode(loginId, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException exception) {
            throw ApiException.local();
        }
    }

    public static boolean readAvailable(JSONObject response) throws ApiException {
        return JsonReads.bool(JsonReads.object(response), "available");
    }

    public static JSONObject signupBody(
            String loginId,
            String password,
            String passwordConfirm,
            String accountType,
            String name,
            String birthDate,
            String phone,
            long verificationId,
            String verificationToken,
            PreparedTerms terms
    ) throws ApiException {
        JSONObject body = new JSONObject();
        JSONArray agreements = new JSONArray();
        try {
            body.put("login_id", loginId);
            body.put("password", password);
            body.put("password_confirm", passwordConfirm);
            body.put("account_type", accountType);
            body.put("name", name);
            body.put("birth_date", birthDate);
            body.put("phone", phone);
            body.put("verification_id", verificationId);
            body.put("verification_token", verificationToken);
            agreements.put(agreement(terms.tosId));
            agreements.put(agreement(terms.privacyId));
            agreements.put(agreement(terms.noticeId));
            agreements.put(agreement(terms.sensitiveId));
            body.put("agreements", agreements);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return body;
    }

    public static long readSignupUserId(JSONObject response) throws ApiException {
        return JsonReads.positiveId(JsonReads.object(response), "user_id");
    }

    public static JSONObject pushTokenBody(String fcmToken) throws ApiException {
        if (fcmToken == null || fcmToken.isEmpty()) {
            throw ApiException.local();
        }
        JSONObject body = new JSONObject();
        try {
            body.put("fcm_token", fcmToken);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return body;
    }

    private static JSONObject agreement(long termsVersionId) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("terms_version_id", termsVersionId);
        item.put("agreed", true);
        return item;
    }

    private static int screenSlot(String type) {
        for (int index = 0; index < SCREEN_TYPES.length; index++) {
            if (SCREEN_TYPES[index].equals(type)) {
                return index;
            }
        }
        return -1;
    }
}
