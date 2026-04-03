package com.mirceone.inventoryapp.api.firms;

import com.mirceone.inventoryapp.service.FirmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/firms")
@Tag(name = "Firms", description = "Firm management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class FirmController {

    private final FirmService firmService;

    public FirmController(FirmService firmService) {
        this.firmService = firmService;
    }

    @PostMapping
    @Operation(summary = "Create a firm for current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Firm created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public FirmResponse createFirm(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateFirmRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return firmService.createFirm(userId, request);
    }

    @GetMapping
    @Operation(summary = "List firms where current user is member")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Firms returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public List<FirmResponse> myFirms(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return firmService.getFirmsForUser(userId);
    }
}
