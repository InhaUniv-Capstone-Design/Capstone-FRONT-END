package com.example.yakyeon2.api;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.List;

public final class HomeApi {

    private HomeApi() {
    }

    public static HomeContracts.HomeSummary homeSummary(Context context, long patientId) throws ApiException {
        return HomeContracts.readHomeSummary(ApiClient.call(
                context,
                "GET",
                "/patients/" + patientId + "/home-summary",
                null,
                true
        ));
    }

    public static List<HomeContracts.Medication> medications(Context context, long patientId, String status)
            throws ApiException {
        return HomeContracts.readMedications(ApiClient.callArray(
                context,
                "GET",
                HomeContracts.medicationsPath(patientId, status),
                null,
                true
        ));
    }

    public static List<HomeContracts.Prescription> prescriptions(Context context, long patientId) throws ApiException {
        return HomeContracts.readPrescriptions(ApiClient.callArray(
                context,
                "GET",
                HomeContracts.prescriptionsPath(patientId),
                null,
                true
        ));
    }

    public static List<HomeContracts.DrugSuggestion> autocomplete(Context context, String query, int limit)
            throws ApiException {
        return HomeContracts.readSuggestions(ApiClient.callArray(
                context,
                "GET",
                HomeContracts.autocompletePath(query, limit),
                null,
                true
        ));
    }

    public static HomeContracts.OcrDraft ocr(Context context, long patientId, String filename, String mime, byte[] image)
            throws ApiException {
        return HomeContracts.readOcr(ApiClient.callMultipart(
                context,
                HomeContracts.ocrPath(patientId),
                "image",
                filename,
                mime,
                image,
                true
        ));
    }

    public static HomeContracts.CreatedPrescription createPrescription(
            Context context,
            long patientId,
            JSONObject body
    ) throws ApiException {
        return HomeContracts.readCreated(ApiClient.call(
                context,
                "POST",
                "/patients/" + patientId + "/prescriptions",
                body,
                true
        ));
    }

    public static HomeContracts.Medication updateMedication(Context context, long medicationId, JSONObject body)
            throws ApiException {
        return HomeContracts.readMedication(ApiClient.call(
                context,
                "PATCH",
                "/medications/" + medicationId,
                body,
                true
        ));
    }

    public static void deleteMedication(Context context, long medicationId) throws ApiException {
        ApiClient.call(context, "DELETE", "/medications/" + medicationId, null, true);
    }

    public static HomeContracts.Medication endMedication(Context context, long medicationId) throws ApiException {
        HomeContracts.Medication medication = HomeContracts.readMedication(ApiClient.call(
                context,
                "POST",
                "/medications/" + medicationId + "/end",
                null,
                true
        ));
        if (!HomeContracts.STATUS_ENDED.equals(medication.status)) {
            throw ApiException.response(200);
        }
        return medication;
    }

    public static HomeContracts.EndedPrescription endPrescription(Context context, long prescriptionId)
            throws ApiException {
        return HomeContracts.readEnded(ApiClient.call(
                context,
                "POST",
                "/prescriptions/" + prescriptionId + "/end",
                null,
                true
        ));
    }

    public static List<HomeContracts.Dose> doses(Context context, long patientId, LocalDate date) throws ApiException {
        return HomeContracts.readDoses(ApiClient.callArray(
                context,
                "GET",
                HomeContracts.dosesPath(patientId, date),
                null,
                true
        ));
    }

    public static List<HomeContracts.Dose> doseHistory(
            Context context,
            long patientId,
            LocalDate from,
            LocalDate to,
            String status
    ) throws ApiException {
        return HomeContracts.readDoses(ApiClient.callArray(
                context,
                "GET",
                HomeContracts.historyPath(patientId, from, to, status),
                null,
                true
        ));
    }

    public static HomeContracts.Dose markTaken(Context context, long doseId, String takenAt) throws ApiException {
        return HomeContracts.readDose(ApiClient.call(
                context,
                "POST",
                "/doses/" + doseId + "/taken",
                HomeContracts.takenBody(takenAt),
                true
        ));
    }

    public static HomeContracts.Dose markNotTaken(Context context, long doseId) throws ApiException {
        return HomeContracts.readDose(ApiClient.call(
                context,
                "POST",
                "/doses/" + doseId + "/not-taken",
                null,
                true
        ));
    }

    public static HomeContracts.ScheduleSettings schedule(Context context, long prescriptionId) throws ApiException {
        return HomeContracts.readSchedule(ApiClient.call(
                context,
                "GET",
                "/prescriptions/" + prescriptionId + "/schedule-settings",
                null,
                true
        ));
    }

    public static HomeContracts.ScheduleSettings saveSchedule(Context context, long prescriptionId, JSONObject body)
            throws ApiException {
        return HomeContracts.readSchedule(ApiClient.call(
                context,
                "PUT",
                "/prescriptions/" + prescriptionId + "/schedule-settings",
                body,
                true
        ));
    }

    public static Long medicationTermsVersion(Context context) throws ApiException {
        return HomeContracts.medicationTermsVersion(termsCatalog(context));
    }

    public static Long deviceTermsVersion(Context context) throws ApiException {
        return HomeContracts.deviceTermsVersion(termsCatalog(context));
    }

    private static JSONArray termsCatalog(Context context) throws ApiException {
        return ApiClient.callArray(context, "GET", "/terms", null, false);
    }

    public static JSONObject assignCompartment(Context context, long prescriptionId, int compartmentNo)
            throws ApiException {
        return CareApi.assignCompartment(context, prescriptionId, compartmentNo);
    }
}
