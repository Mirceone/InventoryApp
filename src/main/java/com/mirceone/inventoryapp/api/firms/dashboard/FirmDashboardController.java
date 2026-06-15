package com.mirceone.inventoryapp.api.firms.dashboard;

import com.mirceone.inventoryapp.api.support.CurrentUserId;
import com.mirceone.inventoryapp.service.firms.dashboard.FirmDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/firms/{firmId}/dashboard")
@Tag(name = "Firm Dashboard", description = "Operational dashboard for a selected firm")
@SecurityRequirement(name = "bearerAuth")
public class FirmDashboardController {

    private final FirmDashboardService firmDashboardService;

    public FirmDashboardController(FirmDashboardService firmDashboardService) {
        this.firmDashboardService = firmDashboardService;
    }

    @GetMapping
    @Operation(summary = "Get operational dashboard for the selected firm")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not a member of this firm"),
            @ApiResponse(responseCode = "404", description = "Firm not found")
    })
    public FirmDashboardResponse getDashboard(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId
    ) {
        return FirmDashboardWebMapper.toResponse(firmDashboardService.getDashboard(userId, firmId));
    }
}
