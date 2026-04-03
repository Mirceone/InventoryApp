package com.mirceone.inventoryapp.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimiterTest {

    @Test
    void allowsRequestsWithinLimitAndBlocksAfterLimit() {
        AuthRateLimiter limiter = new AuthRateLimiter(2, 60);
        String key = "127.0.0.1|/auth/login";

        assertTrue(limiter.allow(key));
        assertTrue(limiter.allow(key));
        assertFalse(limiter.allow(key));
    }
}
