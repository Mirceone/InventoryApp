package com.mirceone.inventoryapp.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Value("${app.jwt.secret:}")
    private String secretBase64;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSigningKey() {
        byte[] keyBytes;

        if (secretBase64 == null || secretBase64.isBlank()) {
            // Pentru demo local: generăm un secret la startup.
            keyBytes = new byte[32]; // 256-bit
            new SecureRandom().nextBytes(keyBytes);
            log.warn("APP_JWT_SECRET nu este setat. Se generează automat un secret random pentru demo.");
        } else {
            keyBytes = Base64.getDecoder().decode(secretBase64);
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSigningKey).build();
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        ImmutableSecret<SecurityContext> immutableSecret = new ImmutableSecret<>(jwtSigningKey);
        return new NimbusJwtEncoder(immutableSecret);
    }
}
