package com.example.yakyeon2.api;

import android.content.Context;

import org.json.JSONObject;

import java.util.List;

public final class CareApi {

    private CareApi() {
    }

    public static CareContracts.Summary summary(Context context, long patientId) throws ApiException {
        return CareContracts.readSummary(ApiClient.call(
                context,
                "GET",
                "/patients/" + patientId + "/interactions/summary",
                null,
                true
        ));
    }

    public static CareContracts.GraphDocumentModel graph(Context context, long patientId) throws ApiException {
        return CareContracts.readGraph(ApiClient.call(
                context,
                "GET",
                "/patients/" + patientId + "/interaction-graph",
                null,
                true
        ));
    }

    public static List<CareContracts.InteractionRow> interactions(Context context, long patientId)
            throws ApiException {
        return CareContracts.readInteractions(ApiClient.callArray(
                context,
                "GET",
                "/patients/" + patientId + "/interactions",
                null,
                true
        ));
    }

    public static CareContracts.EdgeDetail edge(Context context, String durSeq) throws ApiException {
        return CareContracts.readEdgeDetail(ApiClient.call(
                context,
                "GET",
                CareContracts.edgePath(durSeq),
                null,
                true
        ));
    }

    public static CareContracts.Pillbox pillbox(Context context, long patientId) throws ApiException {
        return CareContracts.readPillbox(ApiClient.call(
                context,
                "GET",
                "/patients/" + patientId + "/pillbox",
                null,
                true
        ));
    }

    public static JSONObject registerDevice(
            Context context,
            long patientId,
            String serialNo,
            String secret,
            Long maxCompartments
    ) throws ApiException {
        return ApiClient.call(
                context,
                "POST",
                "/patients/" + patientId + "/device",
                CareContracts.registerDeviceBody(serialNo, secret, maxCompartments),
                true
        );
    }

    public static JSONObject assignCompartment(Context context, long prescriptionId, long compartmentNo)
            throws ApiException {
        return ApiClient.call(
                context,
                "POST",
                "/prescriptions/" + prescriptionId + "/compartment",
                CareContracts.compartmentBody(compartmentNo),
                true
        );
    }

    public static JSONObject loadCompartment(Context context, long mappingId, long loadedCount)
            throws ApiException {
        return ApiClient.call(
                context,
                "POST",
                "/compartment-mappings/" + mappingId + "/load",
                CareContracts.loadBody(loadedCount),
                true
        );
    }

    public static JSONObject refillCompartment(Context context, long mappingId, long addedCount)
            throws ApiException {
        return ApiClient.call(
                context,
                "POST",
                "/compartment-mappings/" + mappingId + "/refill",
                CareContracts.refillBody(addedCount),
                true
        );
    }

    public static void releaseCompartment(Context context, long mappingId) throws ApiException {
        ApiClient.call(context, "DELETE", "/compartment-mappings/" + mappingId, null, true);
    }
}
