package com.example.yakyeon2.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ApiFailureTest {

    @Test
    public void separatesNetworkAuthenticationValidationServerAndSession() {
        assertEquals(ApiFailure.Kind.AUTHENTICATION, ApiFailure.kind(401, ApiCodes.INVALID_CREDENTIALS));
        assertEquals(ApiFailure.Kind.AUTHENTICATION, ApiFailure.kind(401, ApiCodes.CURRENT_PASSWORD_MISMATCH));
        assertEquals(ApiFailure.Kind.VALIDATION, ApiFailure.kind(400, ApiCodes.VALIDATION_ERROR));
        assertEquals(ApiFailure.Kind.VALIDATION, ApiFailure.kind(400, ApiCodes.PASSWORD_MISMATCH));
        assertEquals(ApiFailure.Kind.VALIDATION, ApiFailure.kind(400, ApiCodes.INVALID_NOTIFICATION_SETTING));
        assertEquals(ApiFailure.Kind.SESSION, ApiFailure.kind(401, ApiCodes.INVALID_REFRESH_TOKEN));
        assertEquals(ApiFailure.Kind.SESSION, ApiFailure.kind(401, ApiCodes.REFRESH_TOKEN_REUSED));
        assertEquals(ApiFailure.Kind.SERVER, ApiFailure.kind(500, null));
        assertEquals(ApiFailure.Kind.OTHER, ApiFailure.kind(404, ApiCodes.USER_NOT_FOUND));
        assertEquals(ApiFailure.Kind.OTHER, ApiFailure.kind(429, ApiCodes.LOGIN_LOCKED));
        assertFalse(ApiFailure.Kind.SERVER == ApiFailure.kind(429, ApiCodes.LOGIN_LOCKED));
        assertFalse(ApiFailure.Kind.AUTHENTICATION == ApiFailure.kind(429, ApiCodes.LOGIN_LOCKED));
    }

    @Test
    public void clearsSessionOnlyForRefreshTokenFailures() {
        assertTrue(ApiCodes.endsSession(ApiCodes.INVALID_REFRESH_TOKEN));
        assertTrue(ApiCodes.endsSession(ApiCodes.REFRESH_TOKEN_REUSED));
        assertFalse(ApiCodes.endsSession(ApiCodes.UNAUTHENTICATED));
        assertFalse(ApiCodes.endsSession(ApiCodes.INVALID_CREDENTIALS));
        assertFalse(ApiException.disconnected().endsSession());
        assertFalse(ApiException.http(500, null, "").endsSession());
        assertTrue(ApiException.http(401, ApiCodes.REFRESH_TOKEN_REUSED, "").endsSession());
    }

    @Test
    public void exceptionKindMatchesTransportAndBody() {
        assertEquals(ApiFailure.Kind.NETWORK, ApiException.disconnected().failure());
        assertEquals(ApiFailure.Kind.RESPONSE, ApiException.response(200).failure());
        assertEquals(ApiFailure.Kind.SERVER, ApiException.http(503, null, "").failure());
        assertEquals(
                ApiFailure.Kind.AUTHENTICATION,
                ApiException.http(401, ApiCodes.INVALID_CREDENTIALS, "아이디 또는 비밀번호가 올바르지 않습니다.").failure()
        );
    }

    @Test
    public void fallbackUsesSpecMessagesAndDoesNotInventCodes() {
        assertEquals("아이디 또는 비밀번호가 올바르지 않습니다.", ApiMessages.fallback(ApiCodes.INVALID_CREDENTIALS));
        assertEquals(
                "로그인 시도가 너무 많아요. 15분 뒤에 다시 시도해주세요.",
                ApiMessages.fallback(ApiCodes.LOGIN_LOCKED)
        );
        assertTrue(ApiMessages.prefersServerText(ApiCodes.INVALID_SCHEDULE));
        assertTrue(ApiMessages.prefersServerText(ApiCodes.VALIDATION_ERROR));
        assertFalse(ApiCodes.endsSession(ApiCodes.LOGIN_LOCKED));
        assertEquals("비밀번호와 비밀번호 확인이 일치하지 않습니다.", ApiMessages.fallback(ApiCodes.PASSWORD_MISMATCH));
        assertNull(ApiMessages.fallback("ACCOUNT_WITHDRAWN"));
    }
}
