package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
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
        AuthContracts.IssuedTokenPair signup =
                authService.signup(new AuthContracts.SignupSpec("integration1@example.com", "password123", "Integration User"));
        assertTrue(signup.accessToken() != null && !signup.accessToken().isBlank());
        assertTrue(signup.refreshToken() != null && !signup.refreshToken().isBlank());

        AuthContracts.IssuedTokenPair refreshed =
                authService.refresh(new AuthContracts.RefreshSpec(signup.refreshToken()));
        assertTrue(refreshed.accessToken() != null && !refreshed.accessToken().isBlank());
        assertTrue(refreshed.refreshToken() != null && !refreshed.refreshToken().isBlank());
        assertFalse(signup.refreshToken().equals(refreshed.refreshToken()));

        assertThrows(ResponseStatusException.class,
                () -> authService.refresh(new AuthContracts.RefreshSpec(signup.refreshToken())));
    }

    @Test
    void logoutRevokesRefreshToken() {
        AuthContracts.IssuedTokenPair signup =
                authService.signup(new AuthContracts.SignupSpec("integration2@example.com", "password123", "Integration User 2"));
        authService.logout(new AuthContracts.LogoutSpec(signup.refreshToken()));

        assertThrows(ResponseStatusException.class,
                () -> authService.refresh(new AuthContracts.RefreshSpec(signup.refreshToken())));
    }

    @Test
    void logoutAllRevokesAllSessionsForUser() {
        AuthContracts.IssuedTokenPair first =
                authService.signup(new AuthContracts.SignupSpec("integration3@example.com", "password123", "Integration User 3"));
        AuthContracts.IssuedTokenPair second = authService.refresh(new AuthContracts.RefreshSpec(first.refreshToken()));

        UserEntity user =
                userRepository.findByEmailIgnoreCase("integration3@example.com").orElse(null);
        assertNotNull(user);

        authService.logoutAll(user.getId());

        assertThrows(ResponseStatusException.class,
                () -> authService.refresh(new AuthContracts.RefreshSpec(second.refreshToken())));
    }
}
