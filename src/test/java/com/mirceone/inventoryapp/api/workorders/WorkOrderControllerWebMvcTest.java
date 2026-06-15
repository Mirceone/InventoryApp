package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.model.WorkOrderStatus;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.workorders.WorkOrderContracts;
import com.mirceone.inventoryapp.service.workorders.WorkOrderService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkOrderController.class)
class WorkOrderControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkOrderService workOrderService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    @Test
    void createWorkOrderReturns201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();
        when(workOrderService.createWorkOrder(eq(userId), eq(firmId), any(WorkOrderContracts.CreateWorkOrderSpec.class)))
                .thenReturn(sampleSummary(workOrderId, firmId, userId));

        mockMvc.perform(post("/firms/{firmId}/work-orders", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Proiect X",
                                  "clientName": "Client SRL",
                                  "location": "Bucharest",
                                  "description": "Details",
                                  "estimatedEndDate": "2026-08-08"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Proiect X"))
                .andExpect(jsonPath("$.clientName").value("Client SRL"))
                .andExpect(jsonPath("$.estimatedEndDate").value("2026-08-08"));
    }

    @Test
    void duplicateNameReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        when(workOrderService.createWorkOrder(eq(userId), eq(firmId), any(WorkOrderContracts.CreateWorkOrderSpec.class)))
                .thenThrow(new ResponseStatusException(CONFLICT, "duplicate"));

        mockMvc.perform(post("/firms/{firmId}/work-orders", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Dup",
                                  "clientName": "Client SRL",
                                  "location": "Bucharest",
                                  "estimatedEndDate": "2026-08-08"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void listWorkOrdersReturnsArray() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        when(workOrderService.listWorkOrders(eq(userId), eq(firmId)))
                .thenReturn(List.of(sampleSummary(UUID.randomUUID(), firmId, userId)));

        mockMvc.perform(get("/firms/{firmId}/work-orders", firmId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileCount").value(3));
    }

    @Test
    void patchWorkOrderPartialUpdateReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();
        when(workOrderService.updateWorkOrder(eq(userId), eq(firmId), eq(workOrderId), any(WorkOrderContracts.UpdateWorkOrderSpec.class)))
                .thenReturn(new WorkOrderSummary(
                        workOrderId,
                        firmId,
                        "Renamed",
                        "Client SRL",
                        "Cluj",
                        null,
                        LocalDate.parse("2026-09-15"),
                        WorkOrderStatus.IN_PROGRESS,
                        userId,
                        Instant.parse("2026-06-08T10:00:00Z"),
                        1
                ));

        mockMvc.perform(patch("/firms/{firmId}/work-orders/{workOrderId}", firmId, workOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Renamed",
                                  "clientName": "Client SRL",
                                  "location": "Cluj",
                                  "description": null,
                                  "estimatedEndDate": "2026-09-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void patchWorkOrderEmptyBodyReturns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();
        when(workOrderService.updateWorkOrder(eq(userId), eq(firmId), eq(workOrderId), any(WorkOrderContracts.UpdateWorkOrderSpec.class)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "No fields to update"));

        mockMvc.perform(patch("/firms/{firmId}/work-orders/{workOrderId}", firmId, workOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchWorkOrderStatusReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();
        when(workOrderService.updateWorkOrderStatus(eq(userId), eq(firmId), eq(workOrderId), eq(WorkOrderStatus.COMPLETED)))
                .thenReturn(new WorkOrderSummary(
                        workOrderId,
                        firmId,
                        "Proiect X",
                        "Client SRL",
                        "Bucharest",
                        null,
                        LocalDate.parse("2026-08-08"),
                        WorkOrderStatus.COMPLETED,
                        userId,
                        Instant.parse("2026-06-08T10:00:00Z"),
                        2
                ));

        mockMvc.perform(patch("/firms/{firmId}/work-orders/{workOrderId}/status", firmId, workOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deleteWorkOrderReturns204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();
        doNothing().when(workOrderService).deleteWorkOrder(eq(userId), eq(firmId), eq(workOrderId));

        mockMvc.perform(delete("/firms/{firmId}/work-orders/{workOrderId}", firmId, workOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }

    private static WorkOrderSummary sampleSummary(UUID workOrderId, UUID firmId, UUID userId) {
        return new WorkOrderSummary(
                workOrderId,
                firmId,
                "Proiect X",
                "Client SRL",
                "Bucharest",
                "Details",
                LocalDate.parse("2026-08-08"),
                WorkOrderStatus.PLANNED,
                userId,
                Instant.parse("2026-01-01T00:00:00Z"),
                3
        );
    }
}
