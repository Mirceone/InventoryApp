package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.auth.PasswordResetService;
import com.mirceone.inventoryapp.service.auth.PasswordResetTokenService;
import com.mirceone.inventoryapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PasswordResetIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordResetTokenService passwordResetTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void forgotAndResetPasswordFlowWorks() {
        String email = "password-reset-it@example.com";
        authService.signup(new AuthContracts.SignupSpec(email, "oldpassword123", "Reset IT"));
        UUID userId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();

        String rawToken = passwordResetTokenService.create(userId);
        passwordResetService.resetPassword(new AuthContracts.CompletePasswordResetSpec(rawToken, "newpassword456"));

        var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertTrue(passwordEncoder.matches("newpassword456", user.getPasswordHash()));
        assertFalse(passwordEncoder.matches("oldpassword123", user.getPasswordHash()));

        assertThrows(ResponseStatusException.class, () ->
                authService.login(new AuthContracts.LoginSpec(email, "oldpassword123")));
        assertTrue(authService.login(new AuthContracts.LoginSpec(email, "newpassword456")).accessToken() != null);
    }

    @Test
    void resetWithInvalidTokenFails() {
        assertThrows(ResponseStatusException.class, () ->
                passwordResetService.resetPassword(new AuthContracts.CompletePasswordResetSpec(
                        "invalid-token", "newpassword456")));
    }

    @Test
    void forgotPasswordForUnknownEmailDoesNotThrow() {
        passwordResetService.requestReset(new AuthContracts.ForgotPasswordSpec("unknown-reset@example.com"));
    }
}
