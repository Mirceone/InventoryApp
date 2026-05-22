package com.mirceone.inventoryapp.service.auth;

import com.mirceone.inventoryapp.model.ProviderType;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthContracts.IssuedTokenPair signup(AuthContracts.SignupSpec request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Email already in use");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        UserEntity user = new UserEntity(
                email,
                passwordHash,
                ProviderType.LOCAL,
                email,
                request.displayName()
        );

        user = userRepository.save(user);

        return issueTokenPair(user);
    }

    @Transactional
    public AuthContracts.IssuedTokenPair login(AuthContracts.LoginSpec request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid email or password"));

        if (user.getProvider() != ProviderType.LOCAL || user.getPasswordHash() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Use social login for this account provider");
        }

        boolean ok = passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!ok) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid email or password");
        }

        return issueTokenPair(user);
    }

    @Transactional
    public AuthContracts.IssuedTokenPair refresh(AuthContracts.RefreshSpec request) {
        UUID userId = refreshTokenService.consumeAndRotate(request.refreshToken());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found for refresh token"));

        return issueTokenPair(user);
    }

    @Transactional
    public void logout(AuthContracts.LogoutSpec request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    public AuthContracts.CurrentUserSnapshot getMe(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        return new AuthContracts.CurrentUserSnapshot(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProvider()
        );
    }

    private AuthContracts.IssuedTokenPair issueTokenPair(UserEntity user) {
        String accessToken = jwtTokenService.createAccessToken(user.getId(), user.getEmail(), user.getProvider());
        String refreshToken = refreshTokenService.create(user.getId());

        return new AuthContracts.IssuedTokenPair(
                "Bearer",
                accessToken,
                jwtTokenService.getAccessTokenTtlSeconds(),
                refreshToken,
                refreshTokenService.getRefreshTokenTtlSeconds()
        );
    }
}
