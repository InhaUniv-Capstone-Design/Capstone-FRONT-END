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
                .put("medications", new JSONArray().put(new JSONObject().put("drug_name", "메트포르민정")));
        SettingsContracts.ShareDocument document = SettingsContracts.readShare(response);
        assertTrue(document.text().contains("메트포르민정"));
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
    public void phoneDigitsAcceptOnly010ElevenDigits() {
        assertEquals("01012345678", AuthContracts.phoneDigits("010-1234-5678"));
        assertEquals("01012345678", AuthContracts.phoneDigits("010 1234 5678"));
        assertNull(AuthContracts.phoneDigits("01112345678"));
        assertNull(AuthContracts.phoneDigits("0111234567"));
        assertNull(AuthContracts.phoneDigits("010123456"));
        assertNull(AuthContracts.phoneDigits("010123456789"));
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
