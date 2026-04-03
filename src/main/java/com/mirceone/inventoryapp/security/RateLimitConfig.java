package com.mirceone.inventoryapp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Bean
    public AuthRateLimiter authRateLimiter(
            @Value("${app.security.rate-limit.auth.max-requests:20}") int maxRequests,
            @Value("${app.security.rate-limit.auth.window-seconds:60}") long windowSeconds
    ) {
        return new AuthRateLimiter(maxRequests, windowSeconds);
    }
}
