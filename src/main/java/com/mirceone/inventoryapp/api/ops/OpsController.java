package com.mirceone.inventoryapp.api.ops;

import com.mirceone.inventoryapp.service.ops.OpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ops")
@Tag(name = "Ops", description = "Operational diagnostics endpoints")
public class OpsController {

    private final OpsService opsService;

    public OpsController(OpsService opsService) {
        this.opsService = opsService;
    }

    @GetMapping("/logs")
    @Operation(summary = "Read recent maintenance log lines")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recent log lines returned"),
            @ApiResponse(responseCode = "401", description = "Invalid ops API key"),
            @ApiResponse(responseCode = "503", description = "Ops API disabled")
    })
    public OpsLogsResponse recentLogs(@RequestParam(defaultValue = "50") int limit) {
        return OpsWebMapper.toLogsResponse(opsService.recentLogs(limit));
    }

    @GetMapping("/events")
    @Operation(summary = "Read recent ops events")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recent ops events returned"),
            @ApiResponse(responseCode = "401", description = "Invalid ops API key"),
            @ApiResponse(responseCode = "503", description = "Ops API disabled")
    })
    public List<OpsEventResponse> recentEvents(@RequestParam(defaultValue = "50") int limit) {
        return OpsWebMapper.toEventResponseList(opsService.recentEvents(limit));
    }
}
