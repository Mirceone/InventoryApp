package com.mirceone.inventoryapp.service;

import com.mirceone.inventoryapp.model.RefreshTokenEntity;
import com.mirceone.inventoryapp.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private final Map<String, RefreshTokenEntity> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, 3600, 5);

        when(refreshTokenRepository.save(ArgumentMatchers.any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> {
                    RefreshTokenEntity entity = invocation.getArgument(0);
                    store.put(entity.getTokenHash(), entity);
                    return entity;
                });

        lenient().when(refreshTokenRepository.findByTokenHash(ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(0))));
    }

    @Test
    void consumeAndRotateReturnsUserIdForValidToken() {
        UUID userId = UUID.randomUUID();
        String refreshToken = refreshTokenService.create(userId);

        UUID consumedUserId = refreshTokenService.consumeAndRotate(refreshToken);

        assertEquals(userId, consumedUserId);
    }

    @Test
    void consumeAfterRevokeThrowsUnauthorized() {
        UUID userId = UUID.randomUUID();
        String refreshToken = refreshTokenService.create(userId);
        refreshTokenService.revoke(refreshToken);

        assertThrows(ResponseStatusException.class, () -> refreshTokenService.consumeAndRotate(refreshToken));
    }

    @Test
    void revokeAllForUserRevokesAllActiveTokens() {
        UUID userId = UUID.randomUUID();
        RefreshTokenEntity token1 = new RefreshTokenEntity(userId, "hash1", java.time.Instant.now().plusSeconds(1000));
        RefreshTokenEntity token2 = new RefreshTokenEntity(userId, "hash2", java.time.Instant.now().plusSeconds(1000));
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of(token1, token2));

        refreshTokenService.revokeAllForUser(userId);

        verify(refreshTokenRepository).save(token1);
        verify(refreshTokenRepository).save(token2);
    }
}
