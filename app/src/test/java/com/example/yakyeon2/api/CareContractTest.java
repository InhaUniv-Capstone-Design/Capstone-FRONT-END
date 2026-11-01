package com.example.yakyeon2.api;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CareContractTest {

    @Test
    public void summaryKeepsUncheckedSeparateFromFlagged() throws Exception {
        CareContracts.Summary summary = CareContracts.readSummary(new JSONObject()
                .put("total", 4)
                .put("flagged", 0)
                .put("unchecked", 2));
        assertEquals(4L, summary.total);
        assertEquals(0L, summary.flagged);
        assertEquals(2L, summary.unchecked);
        assertTrue(CareContracts.showsUncheckedNotice(summary));
        assertTrue(CareContracts.needsSafetyDisclaimer(summary));
    }

    @Test
    public void graphKeepsDurSeqAsTextAndRejectsANullGroupMember() throws Exception {
        CareContracts.GraphDocumentModel graph = CareContracts.readGraph(graphJson());
        assertEquals(2, graph.nodes.size());
        assertFalse(graph.nodes.get(1).checked);
        assertEquals("200520363", graph.edges.get(0).durSeq);
        assertEquals("12::15::200520363", graph.edges.get(0).canvasId());
        assertEquals("/interaction-edges/200520363", CareContracts.edgePath(graph.edges.get(0).durSeq));
        assertEquals("/interaction-edges/12a", CareContracts.edgePath("12a"));
        assertEquals("/interaction-edges/1%2F2", CareContracts.edgePath("1/2"));
        String canvas = CareContracts.canvasJson(graph);
        assertTrue(canvas.contains("\"checked\":false"));
        assertTrue(canvas.contains("\"id\":\"200520363\"") || canvas.contains("12::15::200520363"));
        JSONArray groups = graphJson().getJSONArray("groups");
        groups.getJSONArray(0).put(JSONObject.NULL);
        try {
            CareContracts.readGraph(graphJson().put("groups", groups));
            fail();
        } catch (ApiException exception) {
            assertEquals(ApiFailure.Kind.RESPONSE, exception.failure());
        }
    }

    @Test
    public void interactionsAndEdgeDetailKeepSourceFields() throws Exception {
        List<CareContracts.InteractionRow> rows = CareContracts.readInteractions(new JSONArray()
                .put(interaction()));
        assertEquals(1, rows.size());
        assertEquals(12L, rows.get(0).medicationAId);
        assertEquals("200520363", rows.get(0).durSeq);
        CareContracts.EdgeDetail detail = CareContracts.readEdgeDetail(new JSONObject()
                .put("reason_text", "수산화마그네슘과 병용하면 흡수가 줄어듭니다.")
                .put("source_name", "약학정보원")
                .put("source_base_date", "2024-09-30")
                .put("precaution", "병용 시 흡수 저하 가능.")
                .put("disclaimer", "본 정보는 복약 참고용이며 의료적 판단이나 진단을 대체하지 않습니다."));
        assertTrue(detail.disclaimer.contains("대체하지 않습니다"));
        assertEquals("2024-09-30", detail.sourceBaseDate);
        try {
            CareContracts.readEdgeDetail(new JSONObject()
                    .put("reason_text", "이유")
                    .put("source_name", "약학정보원")
                    .put("source_base_date", "2024-09-30")
                    .put("precaution", "주의"));
            fail();
        } catch (ApiException exception) {
            assertEquals(ApiFailure.Kind.RESPONSE, exception.failure());
        }
    }

    @Test
    public void pillboxKeepsNullCountsAndDoesNotTreatMissingReportsAsEmpty() throws Exception {
        CareContracts.Pillbox pillbox = CareContracts.readPillbox(pillboxJson());
        assertEquals(3, pillbox.compartments.size());
        assertEquals(1L, pillbox.compartments.get(0).compartmentNo);
        assertEquals(CareContracts.ACTIVE, pillbox.compartments.get(0).state);
        assertNull(pillbox.compartments.get(0).loadedCount);
        assertEquals(Long.valueOf(2L), pillbox.compartments.get(0).remainingCount);
        assertTrue(pillbox.compartments.get(0).lowStock);
        assertNull(pillbox.compartments.get(0).pouchPresent);
        assertNull(pillbox.compartments.get(0).alertActive);
        assertEquals(CareContracts.OPENED, pillbox.compartments.get(0).attention);
        assertTrue(CareContracts.showsOpened(pillbox.compartments.get(0)));
        assertTrue(CareContracts.showsLowStock(pillbox.compartments.get(0)));
        assertFalse(CareContracts.treatsMissingPouchReportAsEmpty(pillbox.compartments.get(0)));
        assertFalse(CareContracts.treatsMissingAlertAsOff(pillbox.compartments.get(0)));
        CareContracts.Compartment pending = pillbox.compartments.get(1);
        assertEquals(CareContracts.PENDING_LOAD, pending.state);
        assertNull(pending.remainingCount);
        assertFalse(CareContracts.showsRemaining(pending));
        assertNull(pending.attention);
        assertFalse(CareContracts.showsOpened(pending));
        assertEquals(CareContracts.EMPTY, pillbox.compartments.get(2).state);
        assertEquals(2, CareContracts.occupiedCompartments(pillbox));
    }

    @Test
    public void writeBodiesStayInsideTheSpecRanges() throws Exception {
        JSONObject register = CareContracts.registerDeviceBody("SERIAL", "secret-value", null);
        assertEquals("SERIAL", register.getString("serial_no"));
        assertEquals("secret-value", register.getString("secret"));
        assertFalse(register.has("max_compartments"));
        assertEquals(2L, CareContracts.registerDeviceBody("SERIAL", "secret-value", 2L).getLong("max_compartments"));
        assertEquals(1L, CareContracts.compartmentBody(1L).getLong("compartment_no"));
        assertEquals(100L, CareContracts.loadBody(100L).getLong("loaded_count"));
        assertEquals(1L, CareContracts.refillBody(1L).getLong("added_count"));
        assertRejected(() -> CareContracts.compartmentBody(0L));
        assertRejected(() -> CareContracts.compartmentBody(4L));
        assertRejected(() -> CareContracts.loadBody(0L));
        assertRejected(() -> CareContracts.loadBody(101L));
        assertRejected(() -> CareContracts.refillBody(0L));
        assertRejected(() -> CareContracts.registerDeviceBody("", "secret", null));
    }

    @Test
    public void groupsMustContainAtLeastTwoIdsAndDurSeqStaysText() throws Exception {
        JSONObject singleton = graphJson();
        singleton.put("groups", new JSONArray().put(new JSONArray().put(12)));
        assertRejected(() -> CareContracts.readGraph(singleton));
        JSONObject padded = graphJson();
        padded.getJSONArray("edges").getJSONObject(0).put("dur_seq", " 200520363 ");
        assertEquals("200520363", CareContracts.readGraph(padded).edges.get(0).durSeq);
        assertEquals("/interaction-edges/200520363", CareContracts.edgePath(" 200520363 "));
        assertRejected(() -> CareContracts.edgePath("123456789012345678901"));
        CareContracts.Summary summary = CareContracts.readSummary(new JSONObject()
                .put("total", 1)
                .put("flagged", 1)
                .put("unchecked", 1));
        CareContracts.requireConsistentCounts(
                summary,
                CareContracts.readGraph(graphJson()),
                CareContracts.readInteractions(new JSONArray().put(interaction()))
        );
        CareContracts.Summary mismatched = CareContracts.readSummary(new JSONObject()
                .put("total", 1)
                .put("flagged", 1)
                .put("unchecked", 0));
        CareContracts.GraphDocumentModel graph = CareContracts.readGraph(graphJson());
        java.util.List<CareContracts.InteractionRow> rows = CareContracts.readInteractions(
                new JSONArray().put(interaction())
        );
        assertRejected(() -> CareContracts.requireConsistentCounts(mismatched, graph, rows));
    }

    @Test
    public void guardianShareAndWriteRulesStaySeparate() throws Exception {
        SettingsContracts.ManagedPatient self = patient("SELF", null, null);
        SettingsContracts.ManagedPatient omitted = patient("GUARDIAN", "READ_ONLY", null);
        SettingsContracts.ManagedPatient withheld = patient("GUARDIAN", "READ_WRITE", new JSONArray().put("DOSE_RECORD"));
        SettingsContracts.ManagedPatient granted = patient(
                "GUARDIAN",
                "READ_WRITE",
                new JSONArray().put("DUR_INFO").put("DEVICE_EVENT")
        );
        assertTrue(CareContracts.writesAllowed(self));
        assertFalse(CareContracts.shareWithheld(self, CareContracts.DUR_INFO));
        assertFalse(CareContracts.shareWithheld(omitted, CareContracts.DUR_INFO));
        assertFalse(CareContracts.writesAllowed(omitted));
        assertTrue(CareContracts.shareWithheld(withheld, CareContracts.DUR_INFO));
        assertTrue(CareContracts.shareWithheld(withheld, CareContracts.DEVICE_EVENT));
        assertFalse(CareContracts.shareWithheld(granted, CareContracts.DUR_INFO));
        assertTrue(CareContracts.writesAllowed(granted));
        assertEquals(
                "이미 다른 약봉지가 배정된 칸입니다.",
                ApiMessages.fallback(ApiCodes.COMPARTMENT_OCCUPIED)
        );
        assertEquals(
                "이미 약통 칸에 배정된 약봉지입니다.",
                ApiMessages.fallback(ApiCodes.PRESCRIPTION_ALREADY_ASSIGNED)
        );
        assertEquals(
                "이미 충전이 확인된 칸입니다.",
                ApiMessages.fallback(ApiCodes.MAPPING_NOT_PENDING_LOAD)
        );
        assertEquals(
                "충전이 확인된 칸만 추가로 채울 수 있습니다.",
                ApiMessages.fallback(ApiCodes.MAPPING_NOT_ACTIVE)
        );
    }

    private static void assertRejected(Attempt attempt) {
        try {
            attempt.run();
            fail();
        } catch (ApiException exception) {
            assertFalse(exception.network());
        }
    }

    private interface Attempt {
        void run() throws ApiException;
    }

    private static SettingsContracts.ManagedPatient patient(String role, String scope, JSONArray shared)
            throws Exception {
        JSONObject item = new JSONObject()
                .put("patient_id", 4)
                .put("name", "김복약")
                .put("birth_date", "1962-04-18")
                .put("has_account", true)
                .put("my_role", role);
        if (scope != null) {
            item.put("permission_scope", scope);
            item.put("link_id", 9);
        }
        if (shared != null) {
            item.put("shared_items", shared);
        }
        return SettingsContracts.readManagedPatients(new JSONArray().put(item)).get(0);
    }

    private static JSONObject graphJson() throws Exception {
        return new JSONObject()
                .put("nodes", new JSONArray()
                        .put(new JSONObject()
                                .put("medication_id", 12)
                                .put("drug_name", "타이레놀정 500mg")
                                .put("checked", true))
                        .put(new JSONObject()
                                .put("medication_id", 15)
                                .put("drug_name", "알마겔정")
                                .put("checked", false)))
                .put("edges", new JSONArray().put(new JSONObject()
                        .put("med_a", 12)
                        .put("med_b", 15)
                        .put("dur_seq", "200520363")
                        .put("reason_text", "수산화마그네슘과 병용하면 흡수가 줄어듭니다.")
                        .put("source_name", "약학정보원")))
                .put("groups", new JSONArray().put(new JSONArray().put(12).put(15)));
    }

    private static JSONObject interaction() throws Exception {
        return new JSONObject()
                .put("medication_a_id", 12)
                .put("medication_a_name", "타이레놀정 500mg")
                .put("medication_b_id", 15)
                .put("medication_b_name", "알마겔정")
                .put("dur_seq", "200520363")
                .put("reason_text", "수산화마그네슘과 병용하면 흡수가 줄어듭니다.")
                .put("source_name", "약학정보원")
                .put("source_base_date", "2024-09-30");
    }

    private static JSONObject pillboxJson() throws Exception {
        return new JSONObject()
                .put("device", new JSONObject()
                        .put("device_id", 3)
                        .put("serial_no", "PB-001")
                        .put("is_online", true)
                        .put("max_compartments", 3))
                .put("compartments", new JSONArray()
                        .put(compartment(3, "EMPTY", null))
                        .put(compartment(2, "PENDING_LOAD", null))
                        .put(new JSONObject()
                                .put("compartment_no", 1)
                                .put("state", "ACTIVE")
                                .put("mapping_id", 42)
                                .put("prescription_id", 7)
                                .put("hospital_name", "서울대학교병원")
                                .put("medications", new JSONArray().put(new JSONObject()
                                        .put("medication_id", 12)
                                        .put("drug_name", "타이레놀정 500mg")))
                                .put("remaining_count", 2)
                                .put("low_stock", true)
                                .put("attention", "OPENED")));
    }

    private static JSONObject compartment(int number, String state, String attention) throws Exception {
        JSONObject item = new JSONObject()
                .put("compartment_no", number)
                .put("state", state)
                .put("medications", new JSONArray())
                .put("low_stock", false);
        if (attention != null) {
            item.put("attention", attention);
        }
        return item;
    }
}
