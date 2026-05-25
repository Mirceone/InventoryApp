package com.mirceone.inventoryapp.service.firms.members;

import com.mirceone.inventoryapp.service.auth.PasswordResetTokenService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class FirmInvitationTokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashToken(String rawToken) {
        return PasswordResetTokenService.hashToken(rawToken);
    }
}
