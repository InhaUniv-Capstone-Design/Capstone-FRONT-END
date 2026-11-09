package com.example.yakyeon2.api;

import android.content.Context;

import com.example.yakyeon2.data.AuthTokenStore;
import com.example.yakyeon2.fcm.FcmRegistrar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public final class AuthApi {

    private AuthApi() {
    }

    public static void login(Context context, String loginId, String password) throws ApiException {
        if (loginId == null
                || password == null
                || loginId.length() > com.example.yakyeon2.auth.AuthValidation.LOGIN_ID_MAX
                || password.length() > com.example.yakyeon2.auth.AuthValidation.LOGIN_PASSWORD_MAX) {
            throw ApiException.local();
        }
        JSONObject body = new JSONObject();
        try {
            body.put("login_id", loginId);
            body.put("password", password);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        SessionResponse.save(context, ApiClient.call(context, "POST", "/auth/login", body, false));
    }

    public static void refresh(Context context) throws ApiException {
        ApiClient.refreshSession(context);
    }

    public static boolean logout(Context context) throws ApiException {
        String refreshToken = AuthTokenStore.refreshToken(context);
        if (!AuthContracts.callsServerLogout(refreshToken)) {
            AuthTokenStore.clear(context);
            return false;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("refresh_token", refreshToken);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        try {
            ApiClient.call(context, "POST", "/auth/logout", body, false);
        } catch (ApiException exception) {
            if (exception.endsSession()) {
                AuthTokenStore.clear(context);
            }
            throw exception;
        }
        AuthTokenStore.clear(context);
        return true;
    }

    public static AuthContracts.PreparedTerms terms(Context context) throws ApiException {
        return AuthContracts.readTerms(ApiClient.callArray(context, "GET", "/terms", null, false));
    }

    public static AuthContracts.PhoneVerification sendPhoneCode(Context context, String phone) throws ApiException {
        return AuthContracts.readPhoneVerification(ApiClient.call(
                context,
                "POST",
                "/auth/phone-verifications",
                AuthContracts.phoneBody(phone),
                false
        ));
    }

    public static AuthContracts.PhoneConfirm confirmPhone(
            Context context,
            long verificationId,
            String code,
            String phone
    ) throws ApiException {
        return AuthContracts.readPhoneConfirm(ApiClient.call(
                context,
                "POST",
                "/auth/phone-verifications/" + verificationId + "/confirm",
                AuthContracts.confirmBody(code, phone),
                false
        ));
    }

    public static boolean checkLoginId(Context context, String loginId) throws ApiException {
        return AuthContracts.readAvailable(ApiClient.call(
                context,
                "GET",
                AuthContracts.checkIdPath(loginId),
                null,
                false
        ));
    }

    public static long signup(
            Context context,
            String loginId,
            String password,
            String passwordConfirm,
            String accountType,
            String name,
            String birthDate,
            String phone,
            long verificationId,
            String verificationToken,
            AuthContracts.PreparedTerms terms
    ) throws ApiException {
        return AuthContracts.readSignupUserId(ApiClient.call(
                context,
                "POST",
                "/auth/signup",
                AuthContracts.signupBody(
                        loginId,
                        password,
                        passwordConfirm,
                        accountType,
                        name,
                        birthDate,
                        phone,
                        verificationId,
                        verificationToken,
                        terms
                ),
                false
        ));
    }

    public static void registerPushToken(Context context, String fcmToken, boolean notificationPermitted)
            throws ApiException {
        ApiClient.call(
                context,
                "PUT",
                "/users/me/push-token",
                AuthContracts.pushRegistrationBody(fcmToken, notificationPermitted),
                true
        );
        AuthTokenStore.saveFcmToken(context, fcmToken);
    }

    public static void deletePushToken(Context context, String fcmToken) throws ApiException {
        ApiClient.call(
                context,
                "DELETE",
                "/users/me/push-token",
                AuthContracts.pushTokenBody(fcmToken),
                true
        );
    }

    public static List<AuthContracts.FoundAccount> findLoginId(
            Context context,
            String name,
            String phone,
            long verificationId,
            String verificationToken
    ) throws ApiException {
        return AuthContracts.readFoundAccounts(ApiClient.call(
                context,
                "POST",
                "/auth/find-id",
                AuthContracts.findIdBody(name, phone, verificationId, verificationToken),
                false
        ));
    }

    /**
     * Empty 204 is success. The caller drops the verification and any saved session.
     * Passwords and verification tokens are not logged.
     */
    public static void resetPassword(
            Context context,
            String loginId,
            String phone,
            long verificationId,
            String verificationToken,
            String newPassword,
            String newPasswordConfirm
    ) throws ApiException {
        ApiClient.call(
                context,
                "POST",
                "/auth/password-reset",
                AuthContracts.passwordResetBody(
                        loginId,
                        phone,
                        verificationId,
                        verificationToken,
                        newPassword,
                        newPasswordConfirm
                ),
                false
        );
    }

    public static void changePassword(
            Context context,
            String currentPassword,
            String newPassword,
            String newPasswordConfirm
    ) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("current_password", currentPassword);
            body.put("new_password", newPassword);
            body.put("new_password_confirm", newPasswordConfirm);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        ApiClient.call(context, "PATCH", "/auth/password", body, true);
    }

    /**
     * Deletes the stored push token first, then the account.
     * A missing local token skips the push call. A 404 from that call does not stop
     * account deletion. Any other failure leaves the account in place.
     * Local data is cleared by the caller only after this method returns.
     */
    public static void deleteAccount(Context context, String password) throws ApiException {
        String pushToken = AuthTokenStore.fcmToken(context);
        if (pushToken.isEmpty()) {
            pushToken = FcmRegistrar.deviceToken(context);
        }
        if (!pushToken.isEmpty()) {
            try {
                deletePushToken(context, pushToken);
            } catch (ApiException exception) {
                if (exception.status() != 404) {
                    throw exception;
                }
            }
            FcmRegistrar.noteSessionEnded(context);
        }
        JSONObject body = new JSONObject();
        try {
            body.put("password", password);
        } catch (JSONException exception) {
            throw ApiException.local();
        }
        ApiClient.call(context, "DELETE", "/auth/account", body, true);
    }
}
