package com.mirceone.inventoryapp.api.firms;

import com.mirceone.inventoryapp.service.FirmService;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/firms")
public class FirmController {

    private final FirmService firmService;

    public FirmController(FirmService firmService) {
        this.firmService = firmService;
    }

    @PostMapping
    public FirmResponse createFirm(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateFirmRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return firmService.createFirm(userId, request);
    }

    @GetMapping
    public List<FirmResponse> myFirms(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return firmService.getFirmsForUser(userId);
    }
}
