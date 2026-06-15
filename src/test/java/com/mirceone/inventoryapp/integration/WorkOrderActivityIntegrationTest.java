package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.FirmService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderActivityService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderActivitySummary;
import com.mirceone.inventoryapp.service.workorders.WorkOrderContracts;
import com.mirceone.inventoryapp.service.workorders.WorkOrderService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WorkOrderActivityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FirmService firmService;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private WorkOrderActivityService activityService;

    @Test
    void createAndListActivityNewestFirstWithAuthorIdentity() {
        authService.signup(new AuthContracts.SignupSpec("activity-it@example.com", "password123", "Ion Popescu"));
        UserEntity user = userRepository.findByEmailIgnoreCase("activity-it@example.com").orElseThrow();
        UUID userId = user.getId();

        FirmContracts.FirmSummary firm =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Firm Activity"));
        WorkOrderSummary workOrder = workOrderService.createWorkOrder(
                userId, firm.id(),
                new WorkOrderContracts.CreateWorkOrderSpec(
                        "Renovare", "Client", "Bucharest", null,
                        LocalDate.now(ZoneOffset.UTC).plusDays(14)));

        WorkOrderActivitySummary first = activityService.createActivity(
                userId, firm.id(), workOrder.id(),
                new WorkOrderContracts.CreateActivitySpec("Installed ceiling fixtures", "Zone 2 done"));
        assertNotNull(first.id());
        assertEquals("Installed ceiling fixtures", first.title());
        assertEquals("Zone 2 done", first.description());
        assertEquals(userId, first.createdByUserId());
        assertEquals("activity-it@example.com", first.createdByEmail());
        assertEquals("Ion Popescu", first.createdByDisplayName());

        WorkOrderActivitySummary second = activityService.createActivity(
                userId, firm.id(), workOrder.id(),
                new WorkOrderContracts.CreateActivitySpec("Wired zone 3", null));
        assertNull(second.description());

        List<WorkOrderActivitySummary> activity = activityService.listActivity(userId, firm.id(), workOrder.id());
        assertEquals(2, activity.size());
        // Newest first.
        assertEquals("Wired zone 3", activity.get(0).title());
        assertEquals("Installed ceiling fixtures", activity.get(1).title());
    }
}
