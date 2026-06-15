package com.mirceone.inventoryapp.service.firms.dashboard;

import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmInvitationStatus;
import com.mirceone.inventoryapp.model.FirmWorkOrderEntity;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.model.ProductEntity;
import com.mirceone.inventoryapp.repository.CategoryRepository;
import com.mirceone.inventoryapp.repository.FirmInvitationRepository;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.repository.FirmWorkOrderRepository;
import com.mirceone.inventoryapp.repository.ProductRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import com.mirceone.inventoryapp.service.workorders.WorkOrderSummary;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmMembership;
import com.mirceone.inventoryapp.service.firms.access.FirmStatusCatalog;
import com.mirceone.inventoryapp.service.firms.access.MemberRoleCatalog;
import com.mirceone.inventoryapp.service.inventory.InventoryContracts;
import com.mirceone.inventoryapp.service.notifications.NotificationContracts;
import com.mirceone.inventoryapp.service.notifications.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class FirmDashboardService {

    private final FirmRepository firmRepository;
    private final FirmAccessService firmAccessService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FirmWorkOrderRepository firmWorkOrderRepository;
    private final WorkOrderFileRepository workOrderFileRepository;
    private final NotificationService notificationService;
    private final FirmMemberRepository firmMemberRepository;
    private final FirmInvitationRepository firmInvitationRepository;
    private final int defaultReorderThreshold;

    public FirmDashboardService(
            FirmRepository firmRepository,
            FirmAccessService firmAccessService,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            FirmWorkOrderRepository firmWorkOrderRepository,
            WorkOrderFileRepository workOrderFileRepository,
            NotificationService notificationService,
            FirmMemberRepository firmMemberRepository,
            FirmInvitationRepository firmInvitationRepository,
            @Value("${app.inventory.default-reorder-threshold:4}") int defaultReorderThreshold
    ) {
        this.firmRepository = firmRepository;
        this.firmAccessService = firmAccessService;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.firmWorkOrderRepository = firmWorkOrderRepository;
        this.workOrderFileRepository = workOrderFileRepository;
        this.notificationService = notificationService;
        this.firmMemberRepository = firmMemberRepository;
        this.firmInvitationRepository = firmInvitationRepository;
        this.defaultReorderThreshold = defaultReorderThreshold;
    }

    @Transactional(readOnly = true)
    public FirmDashboardContracts.DashboardSnapshot getDashboard(UUID userId, UUID firmId) {
        FirmMembership membership = firmAccessService.resolveMembership(firmId, userId);
        FirmEntity firm = firmRepository.findById(firmId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Firm not found"));

        List<ProductEntity> lowStockProducts = productRepository.findNeedingRestock(firmId, defaultReorderThreshold);
        List<InventoryContracts.BuyListLine> topLowStockItems = lowStockProducts.stream()
                .sorted(Comparator.comparingInt(this::shortfall).reversed().thenComparing(ProductEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .map(this::toBuyListLine)
                .toList();

        List<WorkOrderSummary> recentWorkOrders = firmWorkOrderRepository.findTop5ByFirmIdOrderByCreatedAtDesc(firmId).stream()
                .map(this::toWorkOrderSummary)
                .toList();

        NotificationContracts.NotificationInbox activityInbox = notificationService.listNotificationsForFirm(userId, firmId, 5);

        FirmDashboardContracts.TeamSnapshot team = membership.role() == MemberRole.OWNER
                ? new FirmDashboardContracts.TeamSnapshot(
                firmMemberRepository.countByFirmId(firmId),
                firmInvitationRepository.countByFirmIdAndStatusAndExpiresAtGreaterThanEqual(
                        firmId,
                        FirmInvitationStatus.PENDING,
                        Instant.now()
                )
        )
                : null;

        return new FirmDashboardContracts.DashboardSnapshot(
                toFirmSummary(firm, membership.role()),
                new FirmDashboardContracts.InventorySnapshot(
                        productRepository.countByFirmId(firmId),
                        categoryRepository.countByFirmId(firmId),
                        lowStockProducts.size(),
                        topLowStockItems
                ),
                new FirmDashboardContracts.WorkOrderSnapshot(
                        firmWorkOrderRepository.countByFirmId(firmId),
                        workOrderFileRepository.countByFirmId(firmId),
                        recentWorkOrders
                ),
                new FirmDashboardContracts.ActivitySnapshot(
                        activityInbox.unreadCount(),
                        activityInbox.items()
                ),
                team
        );
    }

    private WorkOrderSummary toWorkOrderSummary(FirmWorkOrderEntity workOrder) {
        return new WorkOrderSummary(
                workOrder.getId(),
                workOrder.getFirmId(),
                workOrder.getName(),
                workOrder.getClientName(),
                workOrder.getLocation(),
                workOrder.getDescription(),
                workOrder.getEstimatedEndDate(),
                workOrder.getStatus(),
                workOrder.getCreatedByUserId(),
                workOrder.getCreatedAt(),
                workOrderFileRepository.countByWorkOrderId(workOrder.getId())
        );
    }

    private FirmContracts.FirmSummary toFirmSummary(FirmEntity firm, MemberRole role) {
        return new FirmContracts.FirmSummary(
                firm.getId(),
                firm.getName(),
                role,
                MemberRoleCatalog.displayLabel(role),
                firm.getStatus(),
                FirmStatusCatalog.displayLabel(firm.getStatus()),
                firm.getStatusMessage()
        );
    }

    private InventoryContracts.BuyListLine toBuyListLine(ProductEntity product) {
        return new InventoryContracts.BuyListLine(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCurrentQuantity(),
                effectiveMinThreshold(product),
                shortfall(product),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getPreferredRouteStopId()
        );
    }

    private int effectiveMinThreshold(ProductEntity product) {
        return product.getReorderThreshold() != null ? product.getReorderThreshold() : defaultReorderThreshold;
    }

    private int shortfall(ProductEntity product) {
        return effectiveMinThreshold(product) - product.getCurrentQuantity();
    }
}
