package com.mirceone.inventoryapp.service;

import com.mirceone.inventoryapp.api.auth.AuthResponse;
import com.mirceone.inventoryapp.api.auth.RefreshRequest;
import com.mirceone.inventoryapp.model.ProviderType;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenService, refreshTokenService);
    }

    @Test
    void refreshReturnsNewTokenPairWhenRefreshTokenValid() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("user@example.com", "hash", ProviderType.LOCAL, "user@example.com", "Test User");

        when(refreshTokenService.consumeAndRotate("valid-refresh")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenService.createAccessToken(isNull(), anyString(), eq(ProviderType.LOCAL)))
                .thenReturn("access-token");
        when(jwtTokenService.getAccessTokenTtlSeconds()).thenReturn(3600L);
        when(refreshTokenService.create(isNull())).thenReturn("new-refresh-token");
        when(refreshTokenService.getRefreshTokenTtlSeconds()).thenReturn(1209600L);

        AuthResponse response = authService.refresh(new RefreshRequest("valid-refresh"));

        assertEquals("Bearer", response.tokenType());
        assertEquals("access-token", response.accessToken());
        assertEquals(3600L, response.expiresInSeconds());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals(1209600L, response.refreshExpiresInSeconds());
    }

    @Test
    void refreshThrowsWhenTokenIsRevokedOrInvalid() {
        when(refreshTokenService.consumeAndRotate("revoked-token"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked"));

        assertThrows(ResponseStatusException.class, () -> authService.refresh(new RefreshRequest("revoked-token")));
    }
}
