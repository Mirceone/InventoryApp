package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.api.auth.AuthResponse;
import com.mirceone.inventoryapp.api.auth.LogoutRequest;
import com.mirceone.inventoryapp.api.auth.RefreshRequest;
import com.mirceone.inventoryapp.api.auth.SignupRequest;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void signupAndRefreshFlowWorksEndToEnd() {
        AuthResponse signup = authService.signup(new SignupRequest("integration1@example.com", "password123", "Integration User"));
        assertTrue(signup.accessToken() != null && !signup.accessToken().isBlank());
        assertTrue(signup.refreshToken() != null && !signup.refreshToken().isBlank());

        AuthResponse refreshed = authService.refresh(new RefreshRequest(signup.refreshToken()));
        assertTrue(refreshed.accessToken() != null && !refreshed.accessToken().isBlank());
        assertTrue(refreshed.refreshToken() != null && !refreshed.refreshToken().isBlank());
        assertFalse(signup.refreshToken().equals(refreshed.refreshToken()));

        assertThrows(ResponseStatusException.class, () -> authService.refresh(new RefreshRequest(signup.refreshToken())));
    }

    @Test
    void logoutRevokesRefreshToken() {
        AuthResponse signup = authService.signup(new SignupRequest("integration2@example.com", "password123", "Integration User 2"));
        authService.logout(new LogoutRequest(signup.refreshToken()));

        assertThrows(ResponseStatusException.class, () -> authService.refresh(new RefreshRequest(signup.refreshToken())));
    }

    @Test
    void logoutAllRevokesAllSessionsForUser() {
        AuthResponse first = authService.signup(new SignupRequest("integration3@example.com", "password123", "Integration User 3"));
        AuthResponse second = authService.refresh(new RefreshRequest(first.refreshToken()));

        UserEntity user = userRepository.findByEmailIgnoreCase("integration3@example.com").orElse(null);
        assertNotNull(user);

        authService.logoutAll(user.getId());

        assertThrows(ResponseStatusException.class, () -> authService.refresh(new RefreshRequest(second.refreshToken())));
    }
}
