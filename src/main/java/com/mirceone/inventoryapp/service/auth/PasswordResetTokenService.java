package com.mirceone.inventoryapp.service.auth;

import com.mirceone.inventoryapp.model.PasswordResetTokenEntity;
import com.mirceone.inventoryapp.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final long tokenTtlSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetTokenService(
            PasswordResetTokenRepository passwordResetTokenRepository,
            @Value("${app.password-reset.token-ttl-seconds:1800}") long tokenTtlSeconds
    ) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    @Transactional
    public String create(UUID userId) {
        cleanupExpiredTokens();
        invalidateActiveTokensForUser(userId);

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plusSeconds(tokenTtlSeconds);

        passwordResetTokenRepository.save(new PasswordResetTokenEntity(userId, tokenHash, expiresAt));
        return rawToken;
    }

    @Transactional
    public UUID consume(String rawToken) {
        cleanupExpiredTokens();
        PasswordResetTokenEntity entity = getActive(rawToken);
        entity.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(entity);
        return entity.getUserId();
    }

    @Transactional
    public void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private void invalidateActiveTokensForUser(UUID userId) {
        List<PasswordResetTokenEntity> active = passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(userId);
        Instant now = Instant.now();
        for (PasswordResetTokenEntity token : active) {
            token.setUsedAt(now);
            passwordResetTokenRepository.save(token);
        }
    }

    private PasswordResetTokenEntity getActive(String rawToken) {
        String tokenHash = hashToken(rawToken);
        PasswordResetTokenEntity entity = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid or expired reset token"));

        if (entity.getUsedAt() != null || entity.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid or expired reset token");
        }
        return entity;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
