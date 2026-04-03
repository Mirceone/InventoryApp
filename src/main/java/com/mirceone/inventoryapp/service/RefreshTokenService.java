package com.mirceone.inventoryapp.service;

import com.mirceone.inventoryapp.model.RefreshTokenEntity;
import com.mirceone.inventoryapp.repository.RefreshTokenRepository;
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
import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenTtlSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-ttl-seconds:1209600}") long refreshTokenTtlSeconds
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    @Transactional
    public String create(UUID userId) {
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenTtlSeconds);

        RefreshTokenEntity entity = new RefreshTokenEntity(userId, tokenHash, expiresAt);
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Transactional
    public UUID consumeAndRotate(String rawToken) {
        RefreshTokenEntity current = getActive(rawToken);
        current.setRevokedAt(Instant.now());
        refreshTokenRepository.save(current);
        return current.getUserId();
    }

    @Transactional
    public void revoke(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(entity -> {
            if (entity.getRevokedAt() == null) {
                entity.setRevokedAt(Instant.now());
                refreshTokenRepository.save(entity);
            }
        });
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    private RefreshTokenEntity getActive(String rawToken) {
        String tokenHash = hashToken(rawToken);
        RefreshTokenEntity entity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid refresh token"));

        if (entity.getRevokedAt() != null || entity.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Refresh token expired or revoked");
        }
        return entity;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
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
