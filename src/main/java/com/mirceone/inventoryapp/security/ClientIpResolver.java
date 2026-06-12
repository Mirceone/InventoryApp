package com.mirceone.inventoryapp.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the client IP used as a rate-limit key.
 *
 * <p>{@code X-Forwarded-For} is attacker-controlled and is only trusted when the app is
 * explicitly configured to run behind a trusted reverse proxy
 * ({@code app.security.rate-limit.trust-forwarded-for=true}). Otherwise the direct socket
 * address is used, so clients cannot forge their bucket key.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request, boolean trustForwardedFor) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // The left-most entry is the original client when added by a trusted proxy.
                int comma = forwarded.indexOf(',');
                String first = (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
                if (!first.isEmpty()) {
                    return first;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
