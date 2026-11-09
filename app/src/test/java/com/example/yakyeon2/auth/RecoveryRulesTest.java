package com.example.yakyeon2.auth;

import com.example.yakyeon2.api.ApiCodes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecoveryRulesTest {

    @Test
    public void missingAccountKeepsThePhoneProofAndABadProofDiscardsIt() {
        assertTrue(RecoveryRules.keepsProof(ApiCodes.ACCOUNT_NOT_FOUND));
        assertTrue(RecoveryRules.keepsProof(ApiCodes.VALIDATION_ERROR));
        assertTrue(RecoveryRules.keepsProof(ApiCodes.RATE_LIMITED));
        assertTrue(RecoveryRules.keepsProof(ApiCodes.PASSWORD_MISMATCH));
        assertFalse(RecoveryRules.keepsProof(ApiCodes.VERIFICATION_NOT_COMPLETED));
    }

    @Test
    public void recoveryNoticesMatchTheServerCodes() {
        assertEquals(RecoveryRules.Notice.ACCOUNT_MISSING, RecoveryRules.noticeFor(ApiCodes.ACCOUNT_NOT_FOUND));
        assertEquals(
                RecoveryRules.Notice.VERIFICATION_RESTART,
                RecoveryRules.noticeFor(ApiCodes.VERIFICATION_NOT_COMPLETED)
        );
        assertEquals(RecoveryRules.Notice.RATE_LIMIT, RecoveryRules.noticeFor(ApiCodes.RATE_LIMITED));
        assertEquals(
                RecoveryRules.Notice.RATE_LIMIT,
                RecoveryRules.noticeFor(ApiCodes.VERIFICATION_RATE_LIMITED)
        );
        assertEquals(RecoveryRules.Notice.PASSWORD_MISMATCH, RecoveryRules.noticeFor(ApiCodes.PASSWORD_MISMATCH));
        assertEquals(RecoveryRules.Notice.VALIDATION, RecoveryRules.noticeFor(ApiCodes.VALIDATION_ERROR));
        assertEquals(RecoveryRules.Notice.OTHER, RecoveryRules.noticeFor(ApiCodes.USER_NOT_FOUND));
    }

    @Test
    public void rateLimitBlocksAnotherRequestUntilTheCooldownPasses() {
        long now = 1_000L;
        assertFalse(RecoveryRules.blocksRetry(0L, now));
        assertTrue(RecoveryRules.blocksRetry(now + RecoveryRules.RATE_LIMIT_COOLDOWN_MS, now));
        assertFalse(RecoveryRules.blocksRetry(now, now));
    }
}
