package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.api.support.CurrentUserId;
import com.mirceone.inventoryapp.service.workorders.WorkOrderService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderSummary;
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
@RequestMapping("/firms/{firmId}/work-orders")
@Tag(name = "Work orders", description = "User-named work orders with a customizable folder tree")
@SecurityRequirement(name = "bearerAuth")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @PostMapping
    @Operation(summary = "Create a named work order (seeded with the default folder structure)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Duplicate name")
    })
    public ResponseEntity<WorkOrderResponse> createWorkOrder(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @Valid @RequestBody CreateWorkOrderRequest request
    ) {
        WorkOrderSummary created = workOrderService.createWorkOrder(
                userId,
                firmId,
                WorkOrderWebMapper.toCreateWorkOrderSpec(request)
        );
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/firms/{firmId}/work-orders/{workOrderId}")
                .buildAndExpand(firmId, created.id())
                .toUri();
        return ResponseEntity.created(location).body(WorkOrderWebMapper.toResponse(created));
    }

    @GetMapping
    @Operation(summary = "List work orders for firm")
    public List<WorkOrderResponse> listWorkOrders(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId
    ) {
        return workOrderService.listWorkOrders(userId, firmId).stream()
                .map(WorkOrderWebMapper::toResponse)
                .toList();
    }

    @GetMapping("/{workOrderId}")
    @Operation(summary = "Get work order by id")
    public WorkOrderResponse getWorkOrder(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId
    ) {
        return WorkOrderWebMapper.toResponse(workOrderService.getWorkOrder(userId, firmId, workOrderId));
    }

    @PatchMapping("/{workOrderId}")
    @Operation(summary = "Update work order (partial)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Validation error or empty body"),
            @ApiResponse(responseCode = "409", description = "Duplicate name")
    })
    public WorkOrderResponse updateWorkOrder(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @Valid @RequestBody UpdateWorkOrderRequest request
    ) {
        return WorkOrderWebMapper.toResponse(workOrderService.updateWorkOrder(
                userId,
                firmId,
                workOrderId,
                WorkOrderWebMapper.toUpdateWorkOrderSpec(request)
        ));
    }

    @PatchMapping("/{workOrderId}/status")
    @Operation(summary = "Update work order status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    public WorkOrderResponse updateWorkOrderStatus(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @Valid @RequestBody UpdateWorkOrderStatusRequest request
    ) {
        return WorkOrderWebMapper.toResponse(workOrderService.updateWorkOrderStatus(
                userId,
                firmId,
                workOrderId,
                request.status()
        ));
    }

    @DeleteMapping("/{workOrderId}")
    @Operation(summary = "Delete work order with its folders and files")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkOrder(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId
    ) {
        workOrderService.deleteWorkOrder(userId, firmId, workOrderId);
    }
}
