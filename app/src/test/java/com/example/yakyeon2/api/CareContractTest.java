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
        assertTrue(CareContracts.hasEmptyCompartment(pillbox));
        assertEquals(1L, CareContracts.compartmentForPrescription(pillbox, 7L).compartmentNo);
        assertNull(CareContracts.compartmentForPrescription(pillbox, 99L));
        assertFalse(CareContracts.assignmentAmbiguous(pillbox, 7L));
        assertTrue(CareContracts.compartmentSelectable(pillbox.compartments.get(0), 7L));
        assertFalse(CareContracts.compartmentSelectable(pillbox.compartments.get(0), 8L));
        assertFalse(CareContracts.compartmentSelectable(pillbox.compartments.get(1), 7L));
        assertTrue(CareContracts.compartmentSelectable(pillbox.compartments.get(2), 7L));
    }

    @Test
    public void assignmentBodyUsesTheDocumentedFields() throws Exception {
        JSONObject body = new JSONObject()
                .put("mapping_id", 9)
                .put("device_id", 3)
                .put("compartment_no", 2)
                .put("prescription_id", 7)
                .put("status", "PENDING_LOAD")
                .put("loaded_count", JSONObject.NULL)
                .put("remaining_count", JSONObject.NULL)
                .put("low_stock", false);
        CareContracts.Assignment assignment = CareContracts.readAssignment(body);
        assertEquals(9L, assignment.mappingId);
        assertEquals(3L, assignment.deviceId);
        assertEquals(2L, assignment.compartmentNo);
        assertEquals(7L, assignment.prescriptionId);
        assertEquals(CareContracts.PENDING_LOAD, assignment.status);
        assertNull(assignment.loadedCount);
        assertNull(assignment.remainingCount);
        JSONObject active = new JSONObject()
                .put("mapping_id", 9)
                .put("device_id", 3)
                .put("compartment_no", 2)
                .put("prescription_id", 7)
                .put("status", "ACTIVE")
                .put("loaded_count", 0)
                .put("remaining_count", 0);
        CareContracts.Assignment loaded = CareContracts.readAssignment(active);
        assertTrue(CareContracts.assignmentCompleted(loaded, 7L, 2L));
        assertEquals(Long.valueOf(0L), loaded.loadedCount);
        assertEquals(Long.valueOf(0L), loaded.remainingCount);
        assertFalse(CareContracts.assignmentCompleted(assignment, 7L, 2L));
        JSONObject negative = new JSONObject(active.toString()).put("loaded_count", -1);
        assertRejected(() -> CareContracts.readAssignment(negative));
        JSONObject unknown = new JSONObject(body.toString()).put("status", "LOADED");
        assertRejected(() -> CareContracts.readAssignment(unknown));
        JSONObject outOfRange = new JSONObject(body.toString()).put("compartment_no", 4);
        assertRejected(() -> CareContracts.readAssignment(outOfRange));
        JSONObject missingDevice = new JSONObject(body.toString());
        missingDevice.remove("device_id");
        assertRejected(() -> CareContracts.readAssignment(missingDevice));
        JSONObject second = pillboxJson();
        second.getJSONArray("compartments").getJSONObject(0)
                .put("state", "ACTIVE")
                .put("prescription_id", 7)
                .put("mapping_id", 43);
        CareContracts.Pillbox ambiguous = CareContracts.readPillbox(second);
        assertTrue(CareContracts.assignmentAmbiguous(ambiguous, 7L));
        assertNull(CareContracts.compartmentForPrescription(ambiguous, 7L));
    }

    @Test
    public void activeAssignmentKeepsResponseCounts() throws Exception {
        CareContracts.Pillbox pillbox = CareContracts.readPillbox(pillboxJson());
        CareContracts.Assignment pending = CareContracts.readAssignment(new JSONObject()
                .put("mapping_id", 9)
                .put("device_id", 3)
                .put("compartment_no", 2)
                .put("prescription_id", 8)
                .put("status", "PENDING_LOAD"));
        assertEquals(CareContracts.PENDING_LOAD, CareContracts.applyActiveAssignment(pillbox, pending, null)
                .compartments.get(1).state);
        CareContracts.Assignment active = CareContracts.readAssignment(new JSONObject()
                .put("mapping_id", 11)
                .put("device_id", 3)
                .put("compartment_no", 3)
                .put("prescription_id", 7)
                .put("status", "ACTIVE")
                .put("loaded_count", 6)
                .put("remaining_count", 6));
        CareContracts.Pillbox updated = CareContracts.applyActiveAssignment(pillbox, active, 1L);
        CareContracts.Compartment assigned = CareContracts.compartmentByNo(updated, 3L);
        assertEquals(CareContracts.ACTIVE, assigned.state);
        assertEquals(Long.valueOf(6L), assigned.loadedCount);
        assertEquals(Long.valueOf(6L), assigned.remainingCount);
        assertEquals(Long.valueOf(7L), assigned.prescriptionId);
        assertEquals(CareContracts.EMPTY, CareContracts.compartmentByNo(updated, 1L).state);
        assertNull(CareContracts.compartmentByNo(updated, 1L).loadedCount);
        assertEquals(3L, CareContracts.compartmentForPrescription(updated, 7L).compartmentNo);
        CareContracts.Pillbox source = CareContracts.readPillbox(pillboxJson());
        JSONObject pendingJson = pillboxJson();
        pendingJson.getJSONArray("compartments").getJSONObject(1)
                .put("mapping_id", 8)
                .put("prescription_id", 9);
        CareContracts.Pillbox withPending = CareContracts.readPillbox(pendingJson);
        CareContracts.Compartment pendingSlot = CareContracts.compartmentByNo(withPending, 2L);
        assertTrue(CareContracts.canRelease(pendingSlot));
        assertTrue(CareContracts.canRelease(CareContracts.compartmentByNo(source, 1L)));
        assertFalse(CareContracts.canRelease(CareContracts.compartmentByNo(source, 2L)));
        assertFalse(CareContracts.canRelease(CareContracts.compartmentByNo(source, 3L)));
        CareContracts.Compartment keptActive = CareContracts.compartmentByNo(withPending, 1L);
        CareContracts.Compartment keptEmpty = CareContracts.compartmentByNo(withPending, 3L);
        CareContracts.Pillbox cleared = CareContracts.clearMapping(withPending, 8L);
        CareContracts.Compartment emptied = CareContracts.compartmentByNo(cleared, 2L);
        assertEquals(CareContracts.EMPTY, emptied.state);
        assertNull(emptied.mappingId);
        assertNull(emptied.prescriptionId);
        assertTrue(emptied.medications.isEmpty());
        assertNull(emptied.remainingCount);
        assertEquals(keptActive, CareContracts.compartmentByNo(cleared, 1L));
        assertEquals(keptEmpty, CareContracts.compartmentByNo(cleared, 3L));
        assertEquals(Long.valueOf(42L), CareContracts.compartmentByNo(cleared, 1L).mappingId);
        assertEquals(1, CareContracts.occupiedCompartments(CareContracts.clearMapping(source, 42L)));
        CareContracts.Assignment asNeeded = CareContracts.readAssignment(new JSONObject()
                .put("mapping_id", 12)
                .put("device_id", 3)
                .put("compartment_no", 2)
                .put("prescription_id", 8)
                .put("status", "ACTIVE")
                .put("loaded_count", 0)
                .put("remaining_count", 0));
        CareContracts.Compartment prn = CareContracts.compartmentByNo(
                CareContracts.applyActiveAssignment(pillbox, asNeeded, null),
                2L
        );
        assertEquals(CareContracts.ACTIVE, prn.state);
        assertEquals(Long.valueOf(0L), prn.loadedCount);
        assertEquals(Long.valueOf(0L), prn.remainingCount);
    }

    @Test
    public void writeBodiesStayInsideTheSpecRanges() throws Exception {
        JSONObject register = CareContracts.registerDeviceBody("SERIAL", "secret-value", null);
        assertEquals("SERIAL", register.getString("serial_no"));
        assertEquals("secret-value", register.getString("secret"));
        assertFalse(register.has("max_compartments"));
        assertEquals(2L, CareContracts.registerDeviceBody("SERIAL", "secret-value", 2L).getLong("max_compartments"));
        assertEquals(1L, CareContracts.compartmentBody(1L).getLong("compartment_no"));
        assertFalse(CareContracts.compartmentBody(1L).has("loaded_count"));
        assertFalse(CareContracts.compartmentBody(1L, null).has("loaded_count"));
        assertEquals(12L, CareContracts.compartmentBody(2L, 12L).getLong("loaded_count"));
        assertEquals(1500L, CareContracts.compartmentBody(1L, 1500L).getLong("loaded_count"));
        assertEquals(100L, CareContracts.loadBody(100L).getLong("loaded_count"));
        assertEquals(1L, CareContracts.refillBody(1L).getLong("added_count"));
        assertFalse(CareContracts.refillBody(100L).has("loaded_count"));
        assertRejected(() -> CareContracts.compartmentBody(0L));
        assertRejected(() -> CareContracts.compartmentBody(4L));
        assertRejected(() -> CareContracts.compartmentBody(1L, 0L));
        assertRejected(() -> CareContracts.compartmentBody(1L, 1501L));
        assertRejected(() -> CareContracts.loadBody(0L));
        assertRejected(() -> CareContracts.loadBody(101L));
        assertRejected(() -> CareContracts.refillBody(0L));
        assertRejected(() -> CareContracts.refillBody(101L));
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
        SettingsContracts.ManagedPatient empty = patient("GUARDIAN", "READ_WRITE", new JSONArray());
        assertTrue(CareContracts.shareWithheld(empty, CareContracts.PERSONAL_INFO));
        assertTrue(CareContracts.shareWithheld(empty, HomeContracts.MED_INFO));
        assertTrue(CareContracts.shareWithheld(empty, HomeContracts.DOSE_RECORD));
        assertTrue(CareContracts.shareSummaryWithheld(empty));
        SettingsContracts.ManagedPatient summary = patient(
                "GUARDIAN",
                "READ_WRITE",
                new JSONArray().put("PERSONAL_INFO").put("MED_INFO").put("DUR_INFO")
        );
        assertFalse(CareContracts.shareSummaryWithheld(summary));
        assertTrue(CareContracts.shareSummaryWithheld(granted));
        assertFalse(CareContracts.shareSummaryWithheld(omitted));
        assertEquals(
                "복약자가 공유를 허용하지 않은 정보입니다.",
                ApiMessages.fallback(ApiCodes.SHARE_NOT_GRANTED)
        );
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
