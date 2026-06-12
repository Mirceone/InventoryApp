package com.mirceone.inventoryapp.service.firms.dashboard;

import com.mirceone.inventoryapp.model.CategoryEntity;
import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmInvitationStatus;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.model.NotificationLevel;
import com.mirceone.inventoryapp.model.NotificationType;
import com.mirceone.inventoryapp.model.ProductEntity;
import com.mirceone.inventoryapp.repository.CategoryRepository;
import com.mirceone.inventoryapp.repository.FirmInvitationRepository;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.repository.FirmWorkOrderRepository;
import com.mirceone.inventoryapp.repository.ProductRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.notifications.NotificationContracts;
import com.mirceone.inventoryapp.service.notifications.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmDashboardServiceTest {

    @Mock
    private FirmRepository firmRepository;
    @Mock
    private FirmMemberRepository firmMemberRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private FirmWorkOrderRepository firmWorkOrderRepository;
    @Mock
    private WorkOrderFileRepository workOrderFileRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private FirmInvitationRepository firmInvitationRepository;

    private FirmDashboardService firmDashboardService;

    private UUID firmId;
    private UUID userId;
    private FirmEntity firm;

    @BeforeEach
    void setUp() {
        FirmAccessService firmAccessService = new FirmAccessService(firmMemberRepository, firmRepository);
        firmDashboardService = new FirmDashboardService(
                firmRepository,
                firmAccessService,
                productRepository,
                categoryRepository,
                firmWorkOrderRepository,
                workOrderFileRepository,
                notificationService,
                firmMemberRepository,
                firmInvitationRepository,
                4
        );

        firmId = UUID.randomUUID();
        userId = UUID.randomUUID();
        firm = new FirmEntity(userId, "Axormi");
        ReflectionTestUtils.setField(firm, "id", firmId);
        firm.setStatus(FirmStatus.PAUSED);
        firm.setStatusUpdatedAt(Instant.parse("2026-05-26T08:00:00Z"));

        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));
        when(productRepository.countByFirmId(firmId)).thenReturn(8L);
        when(categoryRepository.countByFirmId(firmId)).thenReturn(3L);
        when(firmWorkOrderRepository.countByFirmId(firmId)).thenReturn(4L);
        when(workOrderFileRepository.countByFirmId(firmId)).thenReturn(12L);
        when(notificationService.listNotificationsForFirm(userId, firmId, 5)).thenReturn(new NotificationContracts.NotificationInbox(
                1,
                List.of(new NotificationContracts.NotificationSummary(
                        UUID.randomUUID(),
                        firmId,
                        NotificationType.PRODUCT_LOW_STOCK,
                        NotificationLevel.WARNING,
                        "Low stock",
                        "One item is low",
                        Map.of("event", "product_low_stock"),
                        false,
                        null,
                        Instant.parse("2026-05-26T09:00:00Z")
                ))
        ));
    }

    @Test
    void ownerDashboardIncludesTeamPulseAndLowStockData() {
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, userId))
                .thenReturn(Optional.of(new com.mirceone.inventoryapp.model.FirmMemberEntity(firmId, userId, MemberRole.OWNER)));
        when(firmMemberRepository.countByFirmId(firmId)).thenReturn(5L);
        when(firmInvitationRepository.countByFirmIdAndStatusAndExpiresAtGreaterThanEqual(
                org.mockito.ArgumentMatchers.eq(firmId),
                org.mockito.ArgumentMatchers.eq(FirmInvitationStatus.PENDING),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(2L);

        ProductEntity lowStock = mock(ProductEntity.class);
        CategoryEntity category = mock(CategoryEntity.class);
        UUID categoryId = UUID.randomUUID();
        when(lowStock.getId()).thenReturn(UUID.randomUUID());
        when(lowStock.getName()).thenReturn("Cable");
        when(lowStock.getSku()).thenReturn("CBL-1");
        when(lowStock.getCurrentQuantity()).thenReturn(1);
        when(lowStock.getReorderThreshold()).thenReturn(5);
        when(lowStock.getCategory()).thenReturn(category);
        when(lowStock.getPreferredRouteStopId()).thenReturn(null);
        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Electrical");
        when(productRepository.findNeedingRestock(firmId, 4)).thenReturn(List.of(lowStock));

        com.mirceone.inventoryapp.model.FirmWorkOrderEntity workOrder =
                new com.mirceone.inventoryapp.model.FirmWorkOrderEntity(
                        firmId,
                        "Site A",
                        "Client SRL",
                        "Bucharest",
                        "Notes",
                        LocalDate.now(ZoneOffset.UTC).plusDays(30),
                        userId
                );
        UUID workOrderId = UUID.randomUUID();
        ReflectionTestUtils.setField(workOrder, "id", workOrderId);
        ReflectionTestUtils.setField(workOrder, "createdAt", Instant.parse("2026-05-26T07:00:00Z"));
        when(firmWorkOrderRepository.findTop5ByFirmIdOrderByCreatedAtDesc(firmId)).thenReturn(List.of(workOrder));
        when(workOrderFileRepository.countByWorkOrderId(workOrderId)).thenReturn(6L);

        FirmDashboardContracts.DashboardSnapshot snapshot = firmDashboardService.getDashboard(userId, firmId);

        assertEquals("Axormi", snapshot.firm().name());
        assertEquals("Paused", snapshot.firm().statusDisplayLabel());
        assertEquals(1L, snapshot.inventory().lowStockCount());
        assertEquals(1, snapshot.inventory().topLowStockItems().size());
        assertEquals(4L, snapshot.workOrders().totalWorkOrders());
        assertEquals(12L, snapshot.workOrders().totalFiles());
        assertEquals(1, snapshot.workOrders().recentWorkOrders().size());
        assertNotNull(snapshot.team());
        assertEquals(5L, snapshot.team().memberCount());
        assertEquals(2L, snapshot.team().pendingInvitationCount());
    }

    @Test
    void memberDashboardOmitsTeamPulse() {
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, userId))
                .thenReturn(Optional.of(new com.mirceone.inventoryapp.model.FirmMemberEntity(firmId, userId, MemberRole.MEMBER)));
        when(productRepository.findNeedingRestock(firmId, 4)).thenReturn(List.of());
        when(firmWorkOrderRepository.findTop5ByFirmIdOrderByCreatedAtDesc(firmId)).thenReturn(List.of());

        FirmDashboardContracts.DashboardSnapshot snapshot = firmDashboardService.getDashboard(userId, firmId);

        assertEquals("Angajat", snapshot.firm().roleDisplayLabel());
        assertNull(snapshot.team());
    }
}
