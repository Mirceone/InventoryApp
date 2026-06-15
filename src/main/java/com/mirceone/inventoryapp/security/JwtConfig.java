package com.mirceone.inventoryapp.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    /** Profiles where a missing JWT secret is tolerated (an ephemeral key is generated). */
    private static final java.util.Set<String> DEV_PROFILES = java.util.Set.of("dev", "local", "test");

    @Value("${app.jwt.secret:}")
    private String secretBase64;

    @Value("${app.jwt.issuer:http://localhost:8080}")
    private String issuer;

    private final Environment environment;

    public JwtConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSigningKey() {
        byte[] keyBytes;

        if (secretBase64 == null || secretBase64.isBlank()) {
            // Fail fast in production: an auto-generated key is invalidated on every restart
            // and differs across instances, silently breaking token validation.
            if (!isDevProfile()) {
                throw new IllegalStateException(
                        "APP_JWT_SECRET must be set (Base64-encoded, >= 256-bit) outside dev/local/test profiles.");
            }
            keyBytes = new byte[32]; // 256-bit
            new SecureRandom().nextBytes(keyBytes);
            log.warn("APP_JWT_SECRET not set; generating an ephemeral random secret (dev profile only).");
        } else {
            keyBytes = Base64.getDecoder().decode(secretBase64);
            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                        "APP_JWT_SECRET is too short: HMAC-SHA256 requires a key of at least 256 bits (32 bytes).");
            }
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    private boolean isDevProfile() {
        String[] active = environment.getActiveProfiles();
        if (active.length == 0) {
            // No explicit profile: treat as dev to keep local "just run it" working.
            return true;
        }
        return Arrays.stream(active).anyMatch(DEV_PROFILES::contains);
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(withIssuer);
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        ImmutableSecret<SecurityContext> immutableSecret = new ImmutableSecret<>(jwtSigningKey);
        return new NimbusJwtEncoder(immutableSecret);
    }
}
