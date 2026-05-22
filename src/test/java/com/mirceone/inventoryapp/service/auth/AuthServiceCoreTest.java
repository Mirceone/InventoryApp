package com.mirceone.inventoryapp.service.auth;

import com.mirceone.inventoryapp.model.ProviderType;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceCoreTest {

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
    void signupCreatesUserAndReturnsTokenPair() {
        AuthContracts.SignupSpec request =
                new AuthContracts.SignupSpec("new@example.com", "password123", "New User");
        UserEntity saved = new UserEntity("new@example.com", "hashed", ProviderType.LOCAL, "new@example.com", "New User");
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());

        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(UserEntity.class))).thenReturn(saved);
        when(jwtTokenService.createAccessToken(any(UUID.class), eq("new@example.com"), eq(ProviderType.LOCAL))).thenReturn("access");
        when(jwtTokenService.getAccessTokenTtlSeconds()).thenReturn(3600L);
        when(refreshTokenService.create(any(UUID.class))).thenReturn("refresh");
        when(refreshTokenService.getRefreshTokenTtlSeconds()).thenReturn(1209600L);

        AuthContracts.IssuedTokenPair response = authService.signup(request);

        assertEquals("Bearer", response.tokenType());
        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
    }

    @Test
    void signupWithExistingEmailThrowsConflict() {
        AuthContracts.SignupSpec request =
                new AuthContracts.SignupSpec("existing@example.com", "password123", "Existing");
        when(userRepository.findByEmailIgnoreCase("existing@example.com"))
                .thenReturn(Optional.of(new UserEntity("existing@example.com", "hash", ProviderType.LOCAL,
                        "existing@example.com", "Existing")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.signup(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void loginWithWrongPasswordThrowsUnauthorized() {
        AuthContracts.LoginSpec request = new AuthContracts.LoginSpec("user@example.com", "wrong-pass");
        UserEntity existing = new UserEntity("user@example.com", "stored-hash", ProviderType.LOCAL, "user@example.com", "User");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong-pass", "stored-hash")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
