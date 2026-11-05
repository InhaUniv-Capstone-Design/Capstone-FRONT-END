package com.example.yakyeon2.api;

import com.example.yakyeon2.data.AccountRoles;
import com.example.yakyeon2.data.AccountType;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ApiContractTest {

    @Test
    public void endpointAppendsTheApiPathOnce() throws Exception {
        String terms = ApiClient.endpoint("/terms").toString();
        String login = ApiClient.endpoint("auth/login").toString();
        String base = ApiConfig.BASE_URL.replaceAll("/+$", "");
        assertTrue(base.startsWith("https://"));
        assertEquals(base + "/terms", terms);
        assertEquals(base + "/auth/login", login);
        assertFalse(terms.contains("//terms"));
        assertFalse(base.contains("localhost"));
        assertFalse(base.contains("127.0.0.1"));
        assertFalse(base.contains("15.164.17.62"));
    }

    @Test
    public void logoutWithoutRefreshTokenDoesNotCallTheServer() {
        assertFalse(AuthContracts.callsServerLogout(null));
        assertFalse(AuthContracts.callsServerLogout(""));
        assertTrue(AuthContracts.callsServerLogout("refresh"));
    }

    @Test
    public void phoneConfirmFalseIsNotAnErrorAndHasNoToken() throws Exception {
        AuthContracts.PhoneConfirm rejected = AuthContracts.readPhoneConfirm(new JSONObject()
                .put("verified", false));
        assertFalse(rejected.verified);
        assertNull(rejected.token);
    }

    @Test
    public void signupBodyUsesScreenTermsAndDoesNotCreateSessionFields() throws Exception {
        AuthContracts.PreparedTerms terms = AuthContracts.readTerms(termsArray());
        JSONObject body = AuthContracts.signupBody(
                "user123",
                "abcde12!",
                "abcde12!",
                AccountRoles.toServer(AccountType.CAREGIVER),
                "이름",
                "1962-04-18",
                "01012345678",
                7L,
                "token",
                terms
        );
        assertEquals("GUARDIAN", body.getString("account_type"));
        assertEquals("1962-04-18", body.getString("birth_date"));
        assertFalse(body.has("access_token"));
        JSONArray agreements = body.getJSONArray("agreements");
        assertEquals(4, agreements.length());
        assertEquals(1L, agreements.getJSONObject(0).getLong("terms_version_id"));
        assertTrue(agreements.getJSONObject(0).getBoolean("agreed"));
        assertEquals(9L, AuthContracts.readSignupUserId(new JSONObject().put("user_id", 9)));
    }

    @Test
    public void extraRequiredTermBlocksSignupTerms() throws Exception {
        JSONArray array = termsArray();
        array.put(new JSONObject()
                .put("terms_version_id", 99)
                .put("terms_type", "DELEGATION")
                .put("version", "1.0")
                .put("content", "extra")
                .put("is_required", true));
        assertEquals(AuthContracts.TermsFit.EXTRA_REQUIRED, AuthContracts.readTerms(array).fit);
    }

    @Test
    public void optionalUnknownTermIsIgnored() throws Exception {
        JSONArray array = termsArray();
        array.put(new JSONObject()
                .put("terms_version_id", 99)
                .put("terms_type", "DELEGATION")
                .put("version", "1.0")
                .put("content", "extra")
                .put("is_required", false));
        assertEquals(AuthContracts.TermsFit.READY, AuthContracts.readTerms(array).fit);
    }

    @Test
    public void pushBodySendsOnlyTheTokenField() throws Exception {
        JSONObject body = AuthContracts.pushTokenBody("real-token");
        assertEquals("real-token", body.getString("fcm_token"));
        assertEquals(1, body.length());
    }

    @Test
    public void deviceSettingBodyContainsOnlyTheChangedField() throws Exception {
        JSONObject body = SettingsContracts.deviceSettingBody(SettingsContracts.BUZZER_ENABLED, false);
        assertEquals(1, body.length());
        assertFalse(body.getBoolean("buzzer_enabled"));
    }

    @Test
    public void deviceParserKeepsOmittedAlarmFieldsUnknown() throws Exception {
        SettingsContracts.DeviceSnapshot snapshot = SettingsContracts.readDevice(deviceJson());
        assertEquals("YA-BOX-0001", snapshot.serialNo);
        assertNull(snapshot.buzzerEnabled);
        assertNull(snapshot.ledEnabled);
        assertNull(snapshot.scheduleSyncedAt);
        assertNull(snapshot.scheduleInSync);
    }

    @Test
    public void shareQueryStopsAtNinetyDaysAndOmitsMedicationIds() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        assertFalse(SettingsContracts.shareRangeAllowed(from, from.plusDays(90)));
        assertTrue(SettingsContracts.shareRangeAllowed(from, from.plusDays(89)));
        String path = SettingsContracts.sharePath(4L, from, from.plusDays(6), true, false, true, false);
        assertTrue(path.startsWith("/patients/4/share-summary?"));
        assertTrue(path.contains("include_records=true"));
        assertTrue(path.contains("include_period=false"));
        assertFalse(path.contains("medication_ids"));
    }

    @Test
    public void shareParserShowsOnlySectionsTheServerSent() throws Exception {
        JSONObject response = new JSONObject()
                .put("patient", new JSONObject().put("name", "홍길동").put("birth_date", "1950-01-01"))
                .put("period", new JSONObject().put("from", "2026-10-01").put("to", "2026-10-29"))
                .put("medications", new JSONArray()
                        .put(new JSONObject()
                                .put("drug_name", "메트포르민정")
                                .put("dose_per_intake", 1)
                                .put("start_date", "2026-10-01")
                                .put("end_date", "2026-11-30")
                                .put("status", "ACTIVE")
                                .put("timing_slots", new JSONArray().put("MORNING")))
                        .put(new JSONObject()
                                .put("drug_name", "종료약")
                                .put("start_date", "2026-09-01")
                                .put("end_date", "2026-10-01")
                                .put("status", "ENDED")));
        SettingsContracts.ShareDocument document = SettingsContracts.readShare(response);
        assertTrue(document.text().contains("메트포르민정"));
        assertTrue(document.text().contains("종료약"));
        assertEquals(2, document.medications.size());
        assertFalse(document.medications.get(0).ended);
        assertEquals("1회 1 · 아침", document.medications.get(0).detail);
        assertTrue(document.medications.get(1).ended);
        assertNull(document.records);
        assertNull(document.adherence);
    }

    @Test
    public void notificationExampleWithOneItemIsRejected() throws Exception {
        JSONObject response = new JSONObject().put("settings", new JSONArray().put(new JSONObject()
                .put("setting_key", NotificationSync.DOSE_REMINDER)
                .put("enabled", true)));
        try {
            UserApi.readSettings(response);
        } catch (ApiException exception) {
            assertEquals(ApiFailure.Kind.RESPONSE, exception.failure());
            return;
        }
        throw new AssertionError("partial settings were accepted");
    }

    @Test
    public void managedPatientsDoNotRequireAUserId() throws Exception {
        JSONArray array = new JSONArray().put(new JSONObject()
                .put("patient_id", 3)
                .put("name", "홍길동")
                .put("birth_date", "1950-01-01")
                .put("has_account", true)
                .put("my_role", "SELF"));
        List<SettingsContracts.ManagedPatient> rows = SettingsContracts.readManagedPatients(array);
        assertEquals(SettingsContracts.Resolution.ONE, SettingsContracts.resolveSelf(rows).resolution);
        assertEquals(3L, SettingsContracts.resolveForSettings(rows, AccountRoles.PATIENT).patientId);
        assertEquals(
                SettingsContracts.Resolution.NONE,
                SettingsContracts.resolveForSettings(rows, AccountRoles.GUARDIAN).resolution
        );
        SettingsContracts.ResolvedPatient firstGuardian = SettingsContracts.resolveForSettings(
                java.util.Arrays.asList(rows.get(0), guardian(8L), guardian(9L)),
                AccountRoles.GUARDIAN
        );
        assertEquals(SettingsContracts.Resolution.ONE, firstGuardian.resolution);
        assertEquals(8L, firstGuardian.patientId);
    }

    @Test
    public void guardianShareFieldsStayOffTheSelfRow() throws Exception {
        JSONArray array = new JSONArray()
                .put(new JSONObject()
                        .put("patient_id", 1)
                        .put("name", "홍길동")
                        .put("birth_date", "1950-01-01")
                        .put("has_account", true)
                        .put("my_role", "SELF"))
                .put(new JSONObject()
                        .put("patient_id", 1)
                        .put("name", "홍길동")
                        .put("birth_date", "1950-01-01")
                        .put("has_account", true)
                        .put("my_role", "GUARDIAN")
                        .put("link_id", 4)
                        .put("permission_scope", "READ_WRITE")
                        .put("relationship", "자녀")
                        .put("shared_items", new JSONArray()
                                .put("PERSONAL_INFO")
                                .put("MED_INFO")
                                .put("PHOTO")
                                .put("DOSE_RECORD"))
                        .put("phone_masked", "010-****-5678"));
        List<SettingsContracts.ManagedPatient> rows = SettingsContracts.readManagedPatients(array);
        assertNull(rows.get(0).sharedItems);
        assertNull(rows.get(0).phoneMasked);
        assertNull(rows.get(0).relationship);
        assertEquals("자녀", rows.get(1).relationship);
        assertEquals("010-****-5678", rows.get(1).phoneMasked);
        assertEquals(4, rows.get(1).sharedItems.size());
        assertTrue(rows.get(1).sharedItems.contains("PHOTO"));

        JSONArray links = new JSONArray().put(new JSONObject()
                .put("link_id", 4)
                .put("guardian_name", "김*호")
                .put("guardian_login_id", "testguardian1")
                .put("status", "ACTIVE")
                .put("permission_scope", "READ_WRITE")
                .put("relationship", "자녀")
                .put("shared_items", new JSONArray().put("PERSONAL_INFO").put("MED_INFO")));
        SettingsContracts.GuardianLink active = SettingsContracts.readGuardianLinks(links).get(0);
        assertEquals("자녀", active.relationship);
        assertEquals(2, active.sharedItems.size());

        JSONArray inactive = new JSONArray().put(new JSONObject()
                .put("link_id", 5)
                .put("guardian_name", "김*호")
                .put("guardian_login_id", "testguardian1")
                .put("status", "REJECTED")
                .put("permission_scope", "READ_WRITE")
                .put("shared_items", new JSONArray()));
        SettingsContracts.GuardianLink rejected = SettingsContracts.readGuardianLinks(inactive).get(0);
        assertNull(rejected.relationship);
        assertTrue(rejected.sharedItems.isEmpty());
    }

    @Test
    public void agreementsMapWithdrawalWithoutServiceNotice() throws Exception {
        JSONArray array = new JSONArray()
                .put(agreement("TOS", 1, false))
                .put(agreement("MEDICATION", 2, true))
                .put(agreement("SERVICE_NOTICE", 3, true));
        List<SettingsContracts.AgreementRecord> records = SettingsContracts.readAgreements(array);
        assertTrue(SettingsContracts.marksFrom(records).isTerms());
        assertFalse(SettingsContracts.marksFrom(records).isDrugCollection());
        assertFalse(SettingsContracts.marksFrom(records).isDrugNotice());
        assertEquals(Long.valueOf(2L), SettingsContracts.versionId(records, "MEDICATION"));
    }

    @Test
    public void phoneVerificationWithoutMessageTextStaysCodeEntry() throws Exception {
        AuthContracts.PhoneVerification started = AuthContracts.readPhoneVerification(new JSONObject()
                .put("verification_id", 5)
                .put("expires_in", 300));
        assertFalse(started.outboundSms());
        assertEquals(5L, started.verificationId);
        assertEquals(300L, started.expiresIn);
        assertNull(started.messageText);
        assertNull(started.messageTo);
        JSONObject confirm = AuthContracts.confirmBody("000000");
        assertEquals("000000", confirm.getString("code"));
        assertFalse(confirm.has("phone"));
    }

    @Test
    public void phoneVerificationWithMessageTextIsOutboundSms() throws Exception {
        AuthContracts.PhoneVerification started = AuthContracts.readPhoneVerification(new JSONObject()
                .put("verification_id", 5)
                .put("expires_in", 300)
                .put("message_text", "106746")
                .put("message_to", "16663538"));
        assertTrue(started.outboundSms());
        assertEquals("106746", started.messageText);
        assertEquals("16663538", started.messageTo);
        assertEquals("1666 3538", AuthContracts.displayMessageTo(started.messageTo));
        assertEquals(5L, AuthContracts.readVerificationId(new JSONObject()
                .put("verification_id", 5)
                .put("expires_in", 300)
                .put("message_text", "106746")
                .put("message_to", "16663538")));
        JSONObject confirm = AuthContracts.confirmBody("106746", "01012345678");
        assertEquals("106746", confirm.getString("code"));
        assertEquals("01012345678", confirm.getString("phone"));
    }

    @Test
    public void phoneVerificationRejectsAPartialOutboundMessage() throws Exception {
        JSONObject textOnly = new JSONObject()
                .put("verification_id", 5)
                .put("expires_in", 300)
                .put("message_text", "106746");
        JSONObject destinationOnly = new JSONObject()
                .put("verification_id", 5)
                .put("expires_in", 300)
                .put("message_to", "16663538");
        JSONObject shortCode = new JSONObject()
                .put("verification_id", 5)
                .put("expires_in", 300)
                .put("message_text", "10674")
                .put("message_to", "16663538");
        assertEquals(ApiFailure.Kind.RESPONSE, rejectedVerification(textOnly));
        assertEquals(ApiFailure.Kind.RESPONSE, rejectedVerification(destinationOnly));
        assertEquals(ApiFailure.Kind.RESPONSE, rejectedVerification(shortCode));
    }

    @Test
    public void phoneDigitsAcceptOnly010ElevenDigits() {
        assertEquals("01012345678", AuthContracts.phoneDigits("010-1234-5678"));
        assertEquals("01012345678", AuthContracts.phoneDigits("010 1234 5678"));
        assertNull(AuthContracts.phoneDigits("01112345678"));
        assertNull(AuthContracts.phoneDigits("0111234567"));
        assertNull(AuthContracts.phoneDigits("010123456"));
        assertNull(AuthContracts.phoneDigits("010123456789"));
    }

    private static ApiFailure.Kind rejectedVerification(JSONObject response) throws Exception {
        try {
            AuthContracts.readPhoneVerification(response);
        } catch (ApiException exception) {
            return exception.failure();
        }
        throw new AssertionError("malformed phone verification was accepted");
    }

    private static SettingsContracts.ManagedPatient guardian(long id) throws Exception {
        JSONArray array = new JSONArray().put(new JSONObject()
                .put("patient_id", id)
                .put("name", "이순자")
                .put("birth_date", "1945-03-01")
                .put("has_account", false)
                .put("my_role", "GUARDIAN")
                .put("link_id", id));
        return SettingsContracts.readManagedPatients(array).get(0);
    }

    private static JSONObject agreement(String type, long id, boolean agreed) throws Exception {
        return new JSONObject()
                .put("terms_type", type)
                .put("terms_version_id", id)
                .put("version", "1.0")
                .put("agreed", agreed)
                .put("changed_at", "2026-10-27T09:00:00+09:00");
    }

    private static JSONObject deviceJson() throws Exception {
        return new JSONObject()
                .put("device_id", 1)
                .put("serial_no", "YA-BOX-0001")
                .put("status", "REGISTERED")
                .put("max_compartments", 3)
                .put("registered_at", "2026-10-27T20:55:37+09:00")
                .put("is_online", true)
                .put("last_seen_at", "2026-10-27T21:09:24+09:00")
                .put("firmware_version", "1.0.3")
                .put("ir_sensor_ok", true)
                .put("button_ok", true);
    }

    private static JSONArray termsArray() throws Exception {
        return new JSONArray()
                .put(term(1, "TOS"))
                .put(term(2, "PRIVACY"))
                .put(term(4, "SERVICE_NOTICE"))
                .put(term(5, "SENSITIVE_HEALTH"));
    }

    private static JSONObject term(long id, String type) throws Exception {
        return new JSONObject()
                .put("terms_version_id", id)
                .put("terms_type", type)
                .put("version", "1.0")
                .put("content", type + " 본문")
                .put("is_required", true);
    }
}
