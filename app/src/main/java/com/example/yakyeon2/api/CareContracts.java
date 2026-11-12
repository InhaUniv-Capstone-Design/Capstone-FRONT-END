package com.example.yakyeon2.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parsers for the interaction graph and the pillbox tab.
 * {@code dur_seq} stays a string. Omitted alarm and stock fields stay null.
 */
public final class CareContracts {

    public static final String PERSONAL_INFO = "PERSONAL_INFO";
    public static final String DUR_INFO = "DUR_INFO";
    public static final String DEVICE_EVENT = "DEVICE_EVENT";
    public static final String READ_ONLY = "READ_ONLY";
    public static final String READ_WRITE = "READ_WRITE";
    public static final String EMPTY = "EMPTY";
    public static final String PENDING_LOAD = "PENDING_LOAD";
    public static final String ACTIVE = "ACTIVE";
    public static final String OPENED = "OPENED";

    private CareContracts() {
    }

    public static final class Summary {
        public final long total;
        public final long flagged;
        public final long unchecked;

        private Summary(long total, long flagged, long unchecked) {
            this.total = total;
            this.flagged = flagged;
            this.unchecked = unchecked;
        }
    }

    public static final class GraphNode {
        public final long medicationId;
        public final String drugName;
        public final boolean checked;

        private GraphNode(long medicationId, String drugName, boolean checked) {
            this.medicationId = medicationId;
            this.drugName = drugName;
            this.checked = checked;
        }
    }

    public static final class GraphEdge {
        public final long medA;
        public final long medB;
        public final String durSeq;
        public final String reasonText;
        public final String sourceName;

        private GraphEdge(long medA, long medB, String durSeq, String reasonText, String sourceName) {
            this.medA = medA;
            this.medB = medB;
            this.durSeq = durSeq;
            this.reasonText = reasonText;
            this.sourceName = sourceName;
        }

        public String canvasId() {
            return medA + "::" + medB + "::" + durSeq;
        }
    }

    public static final class GraphDocumentModel {
        public final List<GraphNode> nodes;
        public final List<GraphEdge> edges;
        public final List<List<Long>> groups;

        private GraphDocumentModel(
                List<GraphNode> nodes,
                List<GraphEdge> edges,
                List<List<Long>> groups
        ) {
            this.nodes = nodes;
            this.edges = edges;
            this.groups = groups;
        }
    }

    public static final class InteractionRow {
        public final long medicationAId;
        public final String medicationAName;
        public final long medicationBId;
        public final String medicationBName;
        public final String durSeq;
        public final String reasonText;
        public final String sourceName;
        public final String sourceBaseDate;

        private InteractionRow(
                long medicationAId,
                String medicationAName,
                long medicationBId,
                String medicationBName,
                String durSeq,
                String reasonText,
                String sourceName,
                String sourceBaseDate
        ) {
            this.medicationAId = medicationAId;
            this.medicationAName = medicationAName;
            this.medicationBId = medicationBId;
            this.medicationBName = medicationBName;
            this.durSeq = durSeq;
            this.reasonText = reasonText;
            this.sourceName = sourceName;
            this.sourceBaseDate = sourceBaseDate;
        }
    }

    public static final class EdgeDetail {
        public final String reasonText;
        public final String sourceName;
        public final String sourceBaseDate;
        public final String precaution;
        public final String disclaimer;

        private EdgeDetail(
                String reasonText,
                String sourceName,
                String sourceBaseDate,
                String precaution,
                String disclaimer
        ) {
            this.reasonText = reasonText;
            this.sourceName = sourceName;
            this.sourceBaseDate = sourceBaseDate;
            this.precaution = precaution;
            this.disclaimer = disclaimer;
        }
    }

    public static final class PillMedicine {
        public final long medicationId;
        public final String drugName;

        private PillMedicine(long medicationId, String drugName) {
            this.medicationId = medicationId;
            this.drugName = drugName;
        }
    }

    public static final class PillDevice {
        public final long deviceId;
        public final String serialNo;
        public final boolean online;
        public final long maxCompartments;

        private PillDevice(long deviceId, String serialNo, boolean online, long maxCompartments) {
            this.deviceId = deviceId;
            this.serialNo = serialNo;
            this.online = online;
            this.maxCompartments = maxCompartments;
        }
    }

    public static final class Compartment {
        public final long compartmentNo;
        public final String state;
        public final Long mappingId;
        public final Long prescriptionId;
        public final String hospitalName;
        public final List<PillMedicine> medications;
        public final Long loadedCount;
        public final Long remainingCount;
        public final boolean lowStock;
        public final Boolean pouchPresent;
        public final Boolean alertActive;
        public final String attention;

        private Compartment(
                long compartmentNo,
                String state,
                Long mappingId,
                Long prescriptionId,
                String hospitalName,
                List<PillMedicine> medications,
                Long loadedCount,
                Long remainingCount,
                boolean lowStock,
                Boolean pouchPresent,
                Boolean alertActive,
                String attention
        ) {
            this.compartmentNo = compartmentNo;
            this.state = state;
            this.mappingId = mappingId;
            this.prescriptionId = prescriptionId;
            this.hospitalName = hospitalName;
            this.medications = medications;
            this.loadedCount = loadedCount;
            this.remainingCount = remainingCount;
            this.lowStock = lowStock;
            this.pouchPresent = pouchPresent;
            this.alertActive = alertActive;
            this.attention = attention;
        }
    }

    public static final class Pillbox {
        public final PillDevice device;
        public final List<Compartment> compartments;

        private Pillbox(PillDevice device, List<Compartment> compartments) {
            this.device = device;
            this.compartments = compartments;
        }
    }

    public static final class Assignment {
        public final long mappingId;
        public final long deviceId;
        public final long compartmentNo;
        public final long prescriptionId;
        public final String status;
        public final Long loadedCount;
        public final Long remainingCount;

        private Assignment(
                long mappingId,
                long deviceId,
                long compartmentNo,
                long prescriptionId,
                String status,
                Long loadedCount,
                Long remainingCount
        ) {
            this.mappingId = mappingId;
            this.deviceId = deviceId;
            this.compartmentNo = compartmentNo;
            this.prescriptionId = prescriptionId;
            this.status = status;
            this.loadedCount = loadedCount;
            this.remainingCount = remainingCount;
        }
    }

    public static final class GraphBundle {
        public final Summary summary;
        public final GraphDocumentModel graph;
        public final List<InteractionRow> interactions;

        public GraphBundle(Summary summary, GraphDocumentModel graph, List<InteractionRow> interactions) {
            this.summary = summary;
            this.graph = graph;
            this.interactions = interactions;
        }
    }

    public static boolean writesAllowed(SettingsContracts.ManagedPatient patient) {
        if (patient == null) {
            return false;
        }
        if (SettingsContracts.SELF.equals(patient.myRole)) {
            return true;
        }
        return READ_WRITE.equals(patient.permissionScope);
    }

    /**
     * A missing {@code shared_items} field is left to the server.
     * A present list withholds every item it does not contain, including an empty list
     * from a link created before the consent web.
     */
    public static boolean shareWithheld(SettingsContracts.ManagedPatient patient, String item) {
        if (patient == null || SettingsContracts.SELF.equals(patient.myRole)) {
            return false;
        }
        if (patient.sharedItems == null) {
            return false;
        }
        return !patient.sharedItems.contains(item);
    }

    /** Share summary needs personal info, medication info, and interaction info together. */
    public static boolean shareSummaryWithheld(SettingsContracts.ManagedPatient patient) {
        return shareWithheld(patient, PERSONAL_INFO)
                || shareWithheld(patient, HomeContracts.MED_INFO)
                || shareWithheld(patient, DUR_INFO);
    }

    /**
     * {@code summary.total} counts medicines taken today and may be smaller than {@code nodes}.
     * {@code unchecked} matches nodes with {@code checked=false}.
     * {@code flagged}, edges, and interactions are the same displayed interaction count.
     */
    public static void requireConsistentCounts(
            Summary summary,
            GraphDocumentModel graph,
            List<InteractionRow> interactions
    ) throws ApiException {
        if (summary == null || graph == null || graph.nodes == null || graph.edges == null || interactions == null) {
            throw ApiException.response(200);
        }
        long unchecked = 0L;
        for (GraphNode node : graph.nodes) {
            if (node == null) {
                throw ApiException.response(200);
            }
            if (!node.checked) {
                unchecked++;
            }
        }
        if (summary.unchecked != unchecked
                || summary.flagged != graph.edges.size()
                || summary.flagged != interactions.size()
                || summary.total < 0L
                || summary.flagged < 0L) {
            throw ApiException.response(200);
        }
    }

    public static boolean showsUncheckedNotice(Summary summary) {
        return summary != null && summary.unchecked > 0L;
    }

    public static boolean needsSafetyDisclaimer(Summary summary) {
        return summary != null && summary.flagged == 0L;
    }

    public static String canvasJson(GraphDocumentModel graph) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"nodes\":[");
        if (graph != null) {
            for (int index = 0; index < graph.nodes.size(); index++) {
                if (index > 0) {
                    builder.append(',');
                }
                GraphNode node = graph.nodes.get(index);
                String id = Long.toString(node.medicationId);
                builder.append("{\"id\":").append(jsonQuote(id));
                builder.append(",\"name\":").append(jsonQuote(node.drugName));
                builder.append(",\"related\":").append(related(graph.edges, node.medicationId) ? "true" : "false");
                builder.append(",\"checked\":").append(node.checked ? "true" : "false");
                builder.append('}');
            }
        }
        builder.append("],\"edges\":[");
        if (graph != null) {
            for (int index = 0; index < graph.edges.size(); index++) {
                if (index > 0) {
                    builder.append(',');
                }
                GraphEdge edge = graph.edges.get(index);
                builder.append("{\"id\":").append(jsonQuote(edge.canvasId()));
                builder.append(",\"source\":").append(jsonQuote(Long.toString(edge.medA)));
                builder.append(",\"target\":").append(jsonQuote(Long.toString(edge.medB)));
                builder.append('}');
            }
        }
        builder.append("]}");
        return builder.toString();
    }

    public static boolean showsOpened(Compartment compartment) {
        return compartment != null && OPENED.equals(compartment.attention);
    }

    public static boolean showsLowStock(Compartment compartment) {
        return compartment != null && compartment.lowStock && !EMPTY.equals(compartment.state);
    }

    public static boolean showsRemaining(Compartment compartment) {
        return compartment != null && compartment.remainingCount != null && !EMPTY.equals(compartment.state);
    }

    public static boolean treatsMissingPouchReportAsEmpty(Compartment compartment) {
        return false;
    }

    public static boolean treatsMissingAlertAsOff(Compartment compartment) {
        return false;
    }

    public static boolean compartmentNoAllowed(long compartmentNo) {
        return compartmentNo >= 1L && compartmentNo <= 3L;
    }

    public static boolean pouchCountAllowed(long count) {
        return count >= 1L && count <= 100L;
    }

    public static boolean loadedCountAllowed(long count) {
        return count >= 1L && count <= 1500L;
    }

    public static Summary readSummary(JSONObject response) throws ApiException {
        JsonReads.object(response);
        return new Summary(
                JsonReads.number(response, "total"),
                JsonReads.number(response, "flagged"),
                JsonReads.number(response, "unchecked")
        );
    }

    public static GraphDocumentModel readGraph(JSONObject response) throws ApiException {
        JsonReads.object(response);
        return new GraphDocumentModel(
                readNodes(requiredArray(response, "nodes")),
                readEdges(requiredArray(response, "edges")),
                readGroups(requiredArray(response, "groups"))
        );
    }

    public static List<InteractionRow> readInteractions(JSONArray array) throws ApiException {
        if (array == null) {
            throw ApiException.response(200);
        }
        List<InteractionRow> rows = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            rows.add(new InteractionRow(
                    JsonReads.positiveId(item, "medication_a_id"),
                    JsonReads.text(item, "medication_a_name"),
                    JsonReads.positiveId(item, "medication_b_id"),
                    JsonReads.text(item, "medication_b_name"),
                    durSeq(item),
                    JsonReads.text(item, "reason_text"),
                    JsonReads.text(item, "source_name"),
                    JsonReads.text(item, "source_base_date")
            ));
        }
        return rows;
    }

    public static EdgeDetail readEdgeDetail(JSONObject response) throws ApiException {
        JsonReads.object(response);
        return new EdgeDetail(
                JsonReads.text(response, "reason_text"),
                JsonReads.text(response, "source_name"),
                JsonReads.text(response, "source_base_date"),
                JsonReads.text(response, "precaution"),
                JsonReads.text(response, "disclaimer")
        );
    }

    public static String edgePath(String durSeq) throws ApiException {
        String trimmed = durSeq == null ? "" : durSeq.trim();
        if (trimmed.isEmpty() || trimmed.length() > 20) {
            throw ApiException.local();
        }
        try {
            return "/interaction-edges/" + URLEncoder.encode(trimmed, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw ApiException.local();
        }
    }

    public static GraphEdge findEdge(List<GraphEdge> edges, String canvasId) {
        if (edges == null || canvasId == null) {
            return null;
        }
        for (GraphEdge edge : edges) {
            if (canvasId.equals(edge.canvasId())) {
                return edge;
            }
        }
        return null;
    }

    public static Pillbox readPillbox(JSONObject response) throws ApiException {
        JsonReads.object(response);
        JSONObject device = response.optJSONObject("device");
        if (device == null) {
            throw ApiException.response(200);
        }
        PillDevice summary = new PillDevice(
                JsonReads.positiveId(device, "device_id"),
                JsonReads.text(device, "serial_no"),
                JsonReads.bool(device, "is_online"),
                JsonReads.number(device, "max_compartments")
        );
        JSONArray compartments = requiredArray(response, "compartments");
        List<Compartment> rows = new ArrayList<>();
        for (int index = 0; index < compartments.length(); index++) {
            JSONObject item = compartments.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            rows.add(readCompartment(item));
        }
        Collections.sort(rows, (left, right) -> Long.compare(left.compartmentNo, right.compartmentNo));
        return new Pillbox(summary, rows);
    }

    public static JSONObject registerDeviceBody(String serialNo, String secret, Long maxCompartments)
            throws ApiException {
        if (serialNo == null || serialNo.isEmpty() || secret == null || secret.isEmpty()) {
            throw ApiException.local();
        }
        if (maxCompartments != null && !compartmentNoAllowed(maxCompartments)) {
            throw ApiException.local();
        }
        JSONObject body = new JSONObject();
        try {
            body.put("serial_no", serialNo);
            body.put("secret", secret);
            if (maxCompartments != null) {
                body.put("max_compartments", maxCompartments);
            }
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return body;
    }

    public static JSONObject compartmentBody(long compartmentNo) throws ApiException {
        return compartmentBody(compartmentNo, null);
    }

    public static JSONObject compartmentBody(long compartmentNo, Long loadedCount) throws ApiException {
        if (!compartmentNoAllowed(compartmentNo)) {
            throw ApiException.local();
        }
        if (loadedCount != null && !loadedCountAllowed(loadedCount)) {
            throw ApiException.local();
        }
        JSONObject body = singleNumber("compartment_no", compartmentNo);
        if (loadedCount == null) {
            return body;
        }
        try {
            body.put("loaded_count", loadedCount);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return body;
    }

    public static JSONObject loadBody(long loadedCount) throws ApiException {
        if (!pouchCountAllowed(loadedCount)) {
            throw ApiException.local();
        }
        return singleNumber("loaded_count", loadedCount);
    }

    public static JSONObject refillBody(long addedCount) throws ApiException {
        if (!pouchCountAllowed(addedCount)) {
            throw ApiException.local();
        }
        return singleNumber("added_count", addedCount);
    }

    public static int occupiedCompartments(Pillbox pillbox) {
        if (pillbox == null) {
            return 0;
        }
        int count = 0;
        for (Compartment compartment : pillbox.compartments) {
            if (!EMPTY.equals(compartment.state)) {
                count++;
            }
        }
        return count;
    }

    public static Assignment readAssignment(JSONObject response) throws ApiException {
        JsonReads.object(response);
        long compartmentNo = JsonReads.number(response, "compartment_no");
        if (!compartmentNoAllowed(compartmentNo)) {
            throw ApiException.response(201);
        }
        String status = JsonReads.text(response, "status");
        if (!ACTIVE.equals(status) && !PENDING_LOAD.equals(status)) {
            throw ApiException.response(201);
        }
        return new Assignment(
                JsonReads.positiveId(response, "mapping_id"),
                JsonReads.positiveId(response, "device_id"),
                compartmentNo,
                JsonReads.positiveId(response, "prescription_id"),
                status,
                pouchCount(response, "loaded_count"),
                pouchCount(response, "remaining_count")
        );
    }

    public static boolean assignmentCompleted(Assignment assignment, long prescriptionId, long compartmentNo) {
        return assignment != null
                && ACTIVE.equals(assignment.status)
                && assignment.prescriptionId == prescriptionId
                && assignment.compartmentNo == compartmentNo;
    }

    public static Pillbox applyActiveAssignment(
            Pillbox pillbox,
            Assignment assignment,
            Long releasedCompartmentNo
    ) {
        if (pillbox == null || assignment == null || !ACTIVE.equals(assignment.status)) {
            return pillbox;
        }
        List<Compartment> rows = new ArrayList<>();
        boolean found = false;
        for (Compartment compartment : pillbox.compartments) {
            boolean target = compartment.compartmentNo == assignment.compartmentNo;
            boolean released = releasedCompartmentNo != null
                    && compartment.compartmentNo == releasedCompartmentNo
                    && !target;
            boolean stalePrescription = !target
                    && compartment.prescriptionId != null
                    && compartment.prescriptionId == assignment.prescriptionId;
            if (target) {
                found = true;
                rows.add(assignedCompartment(compartment, assignment));
            } else if (released || stalePrescription) {
                rows.add(emptyCompartment(compartment));
            } else {
                rows.add(compartment);
            }
        }
        if (!found) {
            rows.add(new Compartment(
                    assignment.compartmentNo,
                    assignment.status,
                    assignment.mappingId,
                    assignment.prescriptionId,
                    null,
                    Collections.emptyList(),
                    assignment.loadedCount,
                    assignment.remainingCount,
                    false,
                    null,
                    null,
                    null
            ));
            Collections.sort(rows, (left, right) -> Long.compare(left.compartmentNo, right.compartmentNo));
        }
        return new Pillbox(pillbox.device, rows);
    }

    public static boolean assignmentAmbiguous(Pillbox pillbox, long prescriptionId) {
        if (pillbox == null || prescriptionId <= 0L) {
            return false;
        }
        int matches = 0;
        for (Compartment compartment : pillbox.compartments) {
            if (compartment.prescriptionId != null && compartment.prescriptionId == prescriptionId) {
                matches++;
            }
        }
        return matches > 1;
    }

    public static Compartment compartmentForPrescription(Pillbox pillbox, long prescriptionId) {
        if (pillbox == null || prescriptionId <= 0L || assignmentAmbiguous(pillbox, prescriptionId)) {
            return null;
        }
        for (Compartment compartment : pillbox.compartments) {
            if (compartment.prescriptionId != null && compartment.prescriptionId == prescriptionId) {
                return compartment;
            }
        }
        return null;
    }

    public static Compartment compartmentByNo(Pillbox pillbox, long compartmentNo) {
        if (pillbox == null) {
            return null;
        }
        for (Compartment compartment : pillbox.compartments) {
            if (compartment.compartmentNo == compartmentNo) {
                return compartment;
            }
        }
        return null;
    }

    public static Compartment compartmentForMapping(Pillbox pillbox, long mappingId) {
        if (pillbox == null || mappingId <= 0L) {
            return null;
        }
        for (Compartment compartment : pillbox.compartments) {
            if (compartment.mappingId != null && compartment.mappingId == mappingId) {
                return compartment;
            }
        }
        return null;
    }

    public static boolean canRelease(Compartment compartment) {
        if (compartment == null || compartment.mappingId == null || compartment.mappingId <= 0L) {
            return false;
        }
        return ACTIVE.equals(compartment.state) || PENDING_LOAD.equals(compartment.state);
    }

    /**
     * Drops one mapping and marks that compartment empty. Other compartments stay as they were.
     * Used only after a successful release when a fresh pillbox read is unavailable.
     */
    public static Pillbox clearMapping(Pillbox pillbox, long mappingId) {
        if (pillbox == null || mappingId <= 0L) {
            return pillbox;
        }
        List<Compartment> rows = new ArrayList<>();
        boolean changed = false;
        for (Compartment compartment : pillbox.compartments) {
            if (compartment.mappingId != null && compartment.mappingId == mappingId) {
                rows.add(emptyCompartment(compartment));
                changed = true;
            } else {
                rows.add(compartment);
            }
        }
        if (!changed) {
            return pillbox;
        }
        return new Pillbox(pillbox.device, rows);
    }

    public static boolean hasEmptyCompartment(Pillbox pillbox) {
        if (pillbox == null) {
            return false;
        }
        for (Compartment compartment : pillbox.compartments) {
            if (EMPTY.equals(compartment.state)) {
                return true;
            }
        }
        return false;
    }

    public static boolean compartmentSelectable(Compartment compartment, long prescriptionId) {
        if (compartment == null || !compartmentNoAllowed(compartment.compartmentNo)) {
            return false;
        }
        if (EMPTY.equals(compartment.state)) {
            return true;
        }
        return compartment.prescriptionId != null && compartment.prescriptionId == prescriptionId;
    }

    private static Compartment readCompartment(JSONObject item) throws ApiException {
        String state = JsonReads.text(item, "state");
        if (!EMPTY.equals(state) && !PENDING_LOAD.equals(state) && !ACTIVE.equals(state)) {
            throw ApiException.response(200);
        }
        long compartmentNo = JsonReads.number(item, "compartment_no");
        if (compartmentNo < 1L) {
            throw ApiException.response(200);
        }
        JSONArray medications = item.optJSONArray("medications");
        if (medications == null) {
            throw ApiException.response(200);
        }
        List<PillMedicine> medicines = new ArrayList<>();
        for (int index = 0; index < medications.length(); index++) {
            JSONObject medicine = medications.optJSONObject(index);
            if (medicine == null) {
                throw ApiException.response(200);
            }
            medicines.add(new PillMedicine(
                    JsonReads.positiveId(medicine, "medication_id"),
                    JsonReads.text(medicine, "drug_name")
            ));
        }
        return new Compartment(
                compartmentNo,
                state,
                JsonReads.optionalPositiveId(item, "mapping_id"),
                JsonReads.optionalPositiveId(item, "prescription_id"),
                JsonReads.optionalText(item, "hospital_name"),
                medicines,
                JsonReads.optionalNumber(item, "loaded_count"),
                JsonReads.optionalNumber(item, "remaining_count"),
                JsonReads.bool(item, "low_stock"),
                JsonReads.optionalBool(item, "pouch_present"),
                JsonReads.optionalBool(item, "alert_active"),
                JsonReads.optionalText(item, "attention")
        );
    }

    private static List<GraphNode> readNodes(JSONArray array) throws ApiException {
        List<GraphNode> nodes = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            nodes.add(new GraphNode(
                    JsonReads.positiveId(item, "medication_id"),
                    JsonReads.text(item, "drug_name"),
                    JsonReads.bool(item, "checked")
            ));
        }
        return nodes;
    }

    private static List<GraphEdge> readEdges(JSONArray array) throws ApiException {
        List<GraphEdge> edges = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                throw ApiException.response(200);
            }
            edges.add(new GraphEdge(
                    JsonReads.positiveId(item, "med_a"),
                    JsonReads.positiveId(item, "med_b"),
                    durSeq(item),
                    JsonReads.text(item, "reason_text"),
                    JsonReads.text(item, "source_name")
            ));
        }
        return edges;
    }

    private static String durSeq(JSONObject item) throws ApiException {
        String value = JsonReads.text(item, "dur_seq");
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 20) {
            throw ApiException.response(200);
        }
        return trimmed;
    }

    private static List<List<Long>> readGroups(JSONArray array) throws ApiException {
        List<List<Long>> groups = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONArray group = array.optJSONArray(index);
            if (group == null) {
                throw ApiException.response(200);
            }
            if (group.length() < 2) {
                throw ApiException.response(200);
            }
            List<Long> ids = new ArrayList<>();
            for (int inner = 0; inner < group.length(); inner++) {
                Object value = group.opt(inner);
                if (!(value instanceof Number)) {
                    throw ApiException.response(200);
                }
                long id = ((Number) value).longValue();
                if (id <= 0L) {
                    throw ApiException.response(200);
                }
                ids.add(id);
            }
            groups.add(ids);
        }
        return groups;
    }

    private static JSONArray requiredArray(JSONObject json, String key) throws ApiException {
        if (json == null || !json.has(key) || json.isNull(key)) {
            throw ApiException.response(200);
        }
        JSONArray array = json.optJSONArray(key);
        if (array == null) {
            throw ApiException.response(200);
        }
        return array;
    }

    private static boolean related(List<GraphEdge> edges, long medicationId) {
        if (edges == null) {
            return false;
        }
        for (GraphEdge edge : edges) {
            if (edge.medA == medicationId || edge.medB == medicationId) {
                return true;
            }
        }
        return false;
    }

    private static String jsonQuote(String value) {
        String text = value == null ? "" : value;
        StringBuilder builder = new StringBuilder(text.length() + 2);
        builder.append('"');
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                default:
                    if (character < 0x20 || character == '\u2028' || character == '\u2029' || character == '<') {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                    break;
            }
        }
        builder.append('"');
        return builder.toString();
    }

    private static Long pouchCount(JSONObject response, String key) throws ApiException {
        Long value = JsonReads.optionalNumber(response, key);
        if (value != null && value < 0L) {
            throw ApiException.response(201);
        }
        return value;
    }

    private static Compartment assignedCompartment(Compartment source, Assignment assignment) {
        return new Compartment(
                source.compartmentNo,
                assignment.status,
                assignment.mappingId,
                assignment.prescriptionId,
                source.hospitalName,
                source.medications,
                assignment.loadedCount != null ? assignment.loadedCount : source.loadedCount,
                assignment.remainingCount != null ? assignment.remainingCount : source.remainingCount,
                source.lowStock,
                source.pouchPresent,
                source.alertActive,
                source.attention
        );
    }

    private static Compartment emptyCompartment(Compartment source) {
        if (EMPTY.equals(source.state)) {
            return source;
        }
        return new Compartment(
                source.compartmentNo,
                EMPTY,
                null,
                null,
                null,
                Collections.emptyList(),
                null,
                null,
                false,
                null,
                null,
                null
        );
    }

    private static JSONObject singleNumber(String key, long value) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put(key, value);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        return body;
    }
}
