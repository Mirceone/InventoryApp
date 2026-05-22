package com.mirceone.inventoryapp.service.auth;

import com.mirceone.inventoryapp.model.PasswordResetTokenEntity;
import com.mirceone.inventoryapp.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private PasswordResetTokenService passwordResetTokenService;

    @BeforeEach
    void setUp() {
        passwordResetTokenService = new PasswordResetTokenService(passwordResetTokenRepository, 1800L);
    }

    @Test
    void createStoresHashedTokenNotRawValue() {
        UUID userId = UUID.randomUUID();
        when(passwordResetTokenRepository.save(any(PasswordResetTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = passwordResetTokenService.create(userId);

        ArgumentCaptor<PasswordResetTokenEntity> captor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(passwordResetTokenRepository).save(captor.capture());

        assertNotEquals(rawToken, captor.getValue().getTokenHash());
        assertEquals(PasswordResetTokenService.hashToken(rawToken), captor.getValue().getTokenHash());
    }

    @Test
    void consumeMarksTokenUsed() {
        String rawToken = "test-raw-token-value";
        String hashed = PasswordResetTokenService.hashToken(rawToken);
        UUID userId = UUID.randomUUID();

        PasswordResetTokenEntity entity = new PasswordResetTokenEntity(userId, hashed, Instant.now().plusSeconds(600));
        when(passwordResetTokenRepository.findByTokenHash(hashed)).thenReturn(Optional.of(entity));
        when(passwordResetTokenRepository.save(any(PasswordResetTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID result = passwordResetTokenService.consume(rawToken);

        assertEquals(userId, result);
        verify(passwordResetTokenRepository).save(entity);
    }

    @Test
    void consumeRejectsExpiredToken() {
        String rawToken = "expired-token";
        String hashed = PasswordResetTokenService.hashToken(rawToken);
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity(
                UUID.randomUUID(), hashed, Instant.now().minusSeconds(60)
        );
        when(passwordResetTokenRepository.findByTokenHash(hashed)).thenReturn(Optional.of(entity));

        assertThrows(ResponseStatusException.class, () -> passwordResetTokenService.consume(rawToken));
    }
}

