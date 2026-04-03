package com.mirceone.inventoryapp.service;

import com.mirceone.inventoryapp.api.auth.LoginRequest;
import com.mirceone.inventoryapp.api.auth.MeResponse;
import com.mirceone.inventoryapp.api.auth.SignupRequest;
import com.mirceone.inventoryapp.api.auth.AuthResponse;
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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
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

        String token = jwtTokenService.createAccessToken(user.getId(), user.getEmail(), user.getProvider());
        return new AuthResponse("Bearer", token, jwtTokenService.getAccessTokenTtlSeconds());
    }

    public AuthResponse login(LoginRequest request) {
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

        String token = jwtTokenService.createAccessToken(user.getId(), user.getEmail(), user.getProvider());
        return new AuthResponse("Bearer", token, jwtTokenService.getAccessTokenTtlSeconds());
    }

    public MeResponse getMe(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProvider()
        );
    }
}
