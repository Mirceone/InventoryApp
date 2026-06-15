package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.api.support.CurrentUserId;
import com.mirceone.inventoryapp.service.workorders.WorkOrderActivityService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderActivitySummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/firms/{firmId}/work-orders/{workOrderId}/activity")
@Tag(name = "Work order activity", description = "Free-form activity log entries authored by firm members")
@SecurityRequirement(name = "bearerAuth")
public class WorkOrderActivityController {

    private final WorkOrderActivityService activityService;

    public WorkOrderActivityController(WorkOrderActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    @Operation(summary = "List activity for a work order (newest first)")
    public List<WorkOrderActivityResponse> listActivity(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId
    ) {
        return activityService.listActivity(userId, firmId, workOrderId).stream()
                .map(WorkOrderActivityWebMapper::toResponse)
                .toList();
    }

    @PostMapping
    @Operation(summary = "Add an activity entry to a work order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    public ResponseEntity<WorkOrderActivityResponse> createActivity(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @Valid @RequestBody CreateActivityRequest request
    ) {
        WorkOrderActivitySummary created = activityService.createActivity(
                userId,
                firmId,
                workOrderId,
                WorkOrderActivityWebMapper.toCreateActivitySpec(request)
        );
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/firms/{firmId}/work-orders/{workOrderId}/activity/{activityId}")
                .buildAndExpand(firmId, workOrderId, created.id())
                .toUri();
        return ResponseEntity.created(location).body(WorkOrderActivityWebMapper.toResponse(created));
    }
}
