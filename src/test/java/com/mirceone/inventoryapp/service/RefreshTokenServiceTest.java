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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private final Map<String, RefreshTokenEntity> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, 3600);

        when(refreshTokenRepository.save(ArgumentMatchers.any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> {
                    RefreshTokenEntity entity = invocation.getArgument(0);
                    store.put(entity.getTokenHash(), entity);
                    return entity;
                });

        when(refreshTokenRepository.findByTokenHash(ArgumentMatchers.anyString()))
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
}
