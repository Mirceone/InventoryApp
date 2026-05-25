package com.mirceone.inventoryapp.api.firms;

import com.mirceone.inventoryapp.service.firms.FirmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
        return FirmWebMapper.toResponse(firmService.createFirm(userId, FirmWebMapper.toCreateSpec(request)));
    }

    @GetMapping
    @Operation(summary = "List firms where current user is member")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Firms returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public List<FirmResponse> myFirms(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return FirmWebMapper.toResponseList(firmService.getFirmsForUser(userId));
    }

    @PatchMapping("/{firmId}/status")
    @Operation(summary = "Update firm status (owner only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Firm status updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm owner"),
            @ApiResponse(responseCode = "404", description = "Firm not found")
    })
    public FirmResponse updateFirmStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @Valid @RequestBody UpdateFirmStatusRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return FirmWebMapper.toResponse(
                firmService.updateFirmStatus(userId, firmId, FirmWebMapper.toUpdateStatusSpec(request))
        );
    }

    @GetMapping("/{firmId}/status/history")
    @Operation(summary = "List firm status history (owner only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status history returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm owner"),
            @ApiResponse(responseCode = "404", description = "Firm not found")
    })
    public List<FirmStatusHistoryResponse> getFirmStatusHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return FirmWebMapper.toHistoryResponseList(firmService.getFirmStatusHistory(userId, firmId));
    }

    @PatchMapping("/{firmId}")
    @Operation(summary = "Rename firm (owner only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Firm renamed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm owner"),
            @ApiResponse(responseCode = "404", description = "Firm not found")
    })
    public FirmResponse renameFirm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @Valid @RequestBody UpdateFirmRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return FirmWebMapper.toResponse(
                firmService.renameFirm(userId, firmId, FirmWebMapper.toUpdateSpec(request))
        );
    }

    @DeleteMapping("/{firmId}")
    @Operation(summary = "Delete firm and all related data (owner only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Firm deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not firm owner"),
            @ApiResponse(responseCode = "404", description = "Firm not found")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFirm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        firmService.deleteFirm(userId, firmId);
    }
}
