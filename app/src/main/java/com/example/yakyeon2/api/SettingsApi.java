package com.example.yakyeon2.api;

import android.content.Context;

import org.json.JSONObject;

import java.time.LocalDate;
import java.util.List;

public final class SettingsApi {

    private SettingsApi() {
    }

    public static List<SettingsContracts.ManagedPatient> managedPatients(Context context) throws ApiException {
        return SettingsContracts.readManagedPatients(ApiClient.callArray(
                context,
                "GET",
                "/users/me/managed-patients",
                null,
                true
        ));
    }

    public static List<SettingsContracts.GuardianLink> guardianLinks(Context context, long patientId)
            throws ApiException {
        return SettingsContracts.readGuardianLinks(ApiClient.callArray(
                context,
                "GET",
                "/patients/" + patientId + "/guardian-links",
                null,
                true
        ));
    }

    public static void unlink(Context context, long linkId) throws ApiException {
        ApiClient.call(context, "DELETE", "/guardian-links/" + linkId, null, true);
    }

    public static List<SettingsContracts.AgreementRecord> agreements(Context context) throws ApiException {
        return SettingsContracts.readAgreements(ApiClient.callArray(
                context,
                "GET",
                "/users/me/agreements",
                null,
                true
        ));
    }

    public static void agree(Context context, long termsVersionId, boolean agreed) throws ApiException {
        ApiClient.call(
                context,
                "POST",
                "/users/me/agreements",
                SettingsContracts.agreementBody(termsVersionId, agreed),
                true
        );
    }

    public static void withdraw(Context context, String termsType) throws ApiException {
        ApiClient.call(
                context,
                "POST",
                "/users/me/agreements/" + termsType + "/withdraw",
                null,
                true
        );
    }

    public static SettingsContracts.DeviceSnapshot device(Context context, long patientId) throws ApiException {
        return SettingsContracts.readDevice(ApiClient.call(
                context,
                "GET",
                "/patients/" + patientId + "/device",
                null,
                true
        ));
    }

    public static SettingsContracts.DeviceSnapshot updateDevice(
            Context context,
            long patientId,
            String field,
            boolean enabled
    ) throws ApiException {
        return SettingsContracts.readDevice(ApiClient.call(
                context,
                "PATCH",
                "/patients/" + patientId + "/device/settings",
                SettingsContracts.deviceSettingBody(field, enabled),
                true
        ));
    }

    public static long syncDevice(Context context, long patientId) throws ApiException {
        return SettingsContracts.readScheduleVersion(ApiClient.call(
                context,
                "POST",
                "/patients/" + patientId + "/device/sync",
                null,
                true
        ));
    }

    public static void disconnectDevice(Context context, long patientId) throws ApiException {
        ApiClient.call(context, "DELETE", "/patients/" + patientId + "/device", null, true);
    }

    public static SettingsContracts.ShareDocument shareSummary(
            Context context,
            long patientId,
            LocalDate from,
            LocalDate to,
            boolean includeRecords,
            boolean includePeriod,
            boolean includeInteractions,
            boolean includeAdherence
    ) throws ApiException {
        JSONObject response = ApiClient.call(
                context,
                "GET",
                SettingsContracts.sharePath(
                        patientId,
                        from,
                        to,
                        includeRecords,
                        includePeriod,
                        includeInteractions,
                        includeAdherence
                ),
                null,
                true
        );
        return SettingsContracts.readShare(response);
    }
}
