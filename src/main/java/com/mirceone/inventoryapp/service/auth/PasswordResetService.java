package com.mirceone.inventoryapp.service.auth;

import com.mirceone.inventoryapp.model.ProviderType;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.email.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenService passwordResetTokenService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final String frontendBaseUrl;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenService passwordResetTokenService,
            RefreshTokenService refreshTokenService,
            EmailService emailService,
            @Value("${app.frontend-url}") String frontendBaseUrl
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenService = passwordResetTokenService;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
        this.frontendBaseUrl = stripTrailingSlash(frontendBaseUrl);
    }

    @Transactional
    public void requestReset(AuthContracts.ForgotPasswordSpec request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (!canResetPassword(user)) {
                return;
            }
            String rawToken = passwordResetTokenService.create(user.getId());
            String resetLink = frontendBaseUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });
    }

    @Transactional
    public void resetPassword(AuthContracts.CompletePasswordResetSpec request) {
        UUID userId = passwordResetTokenService.consume(request.token());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Password reset not allowed"));

        if (!canResetPassword(user)) {
            throw new ResponseStatusException(BAD_REQUEST, "Password reset not allowed for this account");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);
    }

    private static boolean canResetPassword(UserEntity user) {
        return user.getProvider() == ProviderType.LOCAL && user.getPasswordHash() != null;
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
