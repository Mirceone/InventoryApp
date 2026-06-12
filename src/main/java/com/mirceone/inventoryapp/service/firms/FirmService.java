package com.mirceone.inventoryapp.service.firms;

import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.FirmStatusChangeSource;
import com.mirceone.inventoryapp.model.FirmStatusHistoryEntity;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.repository.FirmStatusHistoryRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import com.mirceone.inventoryapp.repository.WorkOrderInvoiceRepository;
import com.mirceone.inventoryapp.service.storage.BlobStorage;
import com.mirceone.inventoryapp.service.workorders.WorkOrderStorageKeys;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import com.mirceone.inventoryapp.service.firms.access.FirmStatusCatalog;
import com.mirceone.inventoryapp.service.firms.access.MemberRoleCatalog;
import com.mirceone.inventoryapp.service.inventory.CategoryService;
import com.mirceone.inventoryapp.service.notifications.NotificationService;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class FirmService {

    private static final Logger log = LoggerFactory.getLogger(FirmService.class);

    private final FirmRepository firmRepository;
    private final FirmMemberRepository firmMemberRepository;
    private final WorkOrderFileRepository workOrderFileRepository;
    private final WorkOrderInvoiceRepository workOrderInvoiceRepository;
    private final FirmStatusHistoryRepository firmStatusHistoryRepository;
    private final BlobStorage blobStorage;
    private final CategoryService categoryService;
    private final FirmAccessService firmAccessService;
    private final NotificationService notificationService;
    private final AfterCommitExecutor afterCommitExecutor;

    public FirmService(
            FirmRepository firmRepository,
            FirmMemberRepository firmMemberRepository,
            WorkOrderFileRepository workOrderFileRepository,
            WorkOrderInvoiceRepository workOrderInvoiceRepository,
            FirmStatusHistoryRepository firmStatusHistoryRepository,
            BlobStorage blobStorage,
            CategoryService categoryService,
            FirmAccessService firmAccessService,
            NotificationService notificationService,
            AfterCommitExecutor afterCommitExecutor
    ) {
        this.firmRepository = firmRepository;
        this.firmMemberRepository = firmMemberRepository;
        this.workOrderFileRepository = workOrderFileRepository;
        this.workOrderInvoiceRepository = workOrderInvoiceRepository;
        this.firmStatusHistoryRepository = firmStatusHistoryRepository;
        this.blobStorage = blobStorage;
        this.categoryService = categoryService;
        this.firmAccessService = firmAccessService;
        this.notificationService = notificationService;
        this.afterCommitExecutor = afterCommitExecutor;
    }

    @Transactional
    public FirmContracts.FirmSummary createFirm(UUID userId, FirmContracts.CreateFirmSpec request) {
        String name = sanitizeFirmName(request.name());
        FirmEntity firm = new FirmEntity(userId, name);
        firm = firmRepository.save(firm);

        FirmMemberEntity member = new FirmMemberEntity(firm.getId(), userId, MemberRole.OWNER);
        firmMemberRepository.save(member);

        categoryService.ensureDefaultCategoryForFirm(firm.getId());

        return toSummary(firm, MemberRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<FirmContracts.FirmSummary> getFirmsForUser(UUID userId) {
        List<FirmMemberEntity> memberships = firmMemberRepository.findAllByUserId(userId);
        Map<UUID, MemberRole> roleByFirmId = memberships.stream()
                .collect(Collectors.toMap(FirmMemberEntity::getFirmId, FirmMemberEntity::getRole));

        List<UUID> firmIds = memberships.stream()
                .map(FirmMemberEntity::getFirmId)
                .toList();

        return firmRepository.findAllByIdIn(firmIds)
                .stream()
                .map(firm -> toSummary(firm, roleByFirmId.get(firm.getId())))
                .toList();
    }

    @Transactional
    public FirmContracts.FirmSummary renameFirm(UUID userId, UUID firmId, FirmContracts.UpdateFirmSpec spec) {
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.FIRM_UPDATE);
        FirmEntity firm = requireFirm(firmId);
        firm.setName(sanitizeFirmName(spec.name()));
        FirmEntity saved = firmRepository.save(firm);
        MemberRole role = firmAccessService.resolveMembership(firmId, userId).role();
        return toSummary(saved, role);
    }

    @Transactional
    public FirmContracts.FirmSummary updateFirmStatus(UUID userId, UUID firmId, FirmContracts.UpdateFirmStatusSpec spec) {
        firmAccessService.requirePermission(firmId, userId, FirmPermission.FIRM_UPDATE);
        FirmEntity firm = requireFirm(firmId);
        applyStatus(firm, spec.status(), spec.message(), userId, FirmStatusChangeSource.MANUAL);
        FirmEntity saved = firmRepository.save(firm);
        MemberRole role = firmAccessService.resolveMembership(firmId, userId).role();
        return toSummary(saved, role);
    }

    @Transactional(readOnly = true)
    public List<FirmContracts.FirmStatusHistoryEntry> getFirmStatusHistory(UUID userId, UUID firmId) {
        firmAccessService.requirePermission(firmId, userId, FirmPermission.FIRM_UPDATE);
        return firmStatusHistoryRepository.findAllByFirmIdOrderByCreatedAtDesc(firmId).stream()
                .map(this::toHistoryEntry)
                .toList();
    }

    /**
     * Sets firm status from server-side code only (e.g. health checks). Not exposed over HTTP.
     */
    @Transactional
    public void setFirmStatusSystem(UUID firmId, FirmStatus status, String message) {
        FirmEntity firm = requireFirm(firmId);
        applyStatus(firm, status, message, null, FirmStatusChangeSource.SYSTEM);
        firmRepository.save(firm);
    }

    @Transactional
    public void deleteFirm(UUID userId, UUID firmId) {
        firmAccessService.requirePermission(firmId, userId, FirmPermission.FIRM_DELETE);
        requireFirm(firmId);
        // Bulk-delete file rows first so the restrictive folder FK does not block the
        // cascading delete of the folder trees.
        workOrderFileRepository.deleteByFirmId(firmId);
        workOrderInvoiceRepository.deleteByFirmId(firmId);
        firmRepository.deleteById(firmId);
        String prefix = WorkOrderStorageKeys.firmPrefix(firmId);
        afterCommitExecutor.execute(() -> deletePrefixQuietly(firmId, prefix));
    }

    private FirmEntity requireFirm(UUID firmId) {
        return firmRepository.findById(firmId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Firm not found"));
    }

    private void applyStatus(
            FirmEntity firm,
            FirmStatus status,
            String message,
            UUID actorUserId,
            FirmStatusChangeSource source
    ) {
        FirmStatus previousStatus = firm.getStatus();
        String normalizedMessage = normalizeStatusMessage(message);

        if (previousStatus == status && java.util.Objects.equals(firm.getStatusMessage(), normalizedMessage)) {
            return;
        }

        firm.setStatus(status);
        firm.setStatusMessage(normalizedMessage);
        firm.setStatusUpdatedAt(Instant.now());
        firmStatusHistoryRepository.save(new FirmStatusHistoryEntity(
                firm.getId(),
                previousStatus,
                status,
                normalizedMessage,
                actorUserId,
                source
        ));
        notificationService.notifyFirmStatusChangedAfterCommit(
                firm.getId(),
                previousStatus,
                status,
                normalizedMessage,
                source
        );
    }

    private String normalizeStatusMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String trimmed = message.strip();
        return trimmed.length() > 512 ? trimmed.substring(0, 512) : trimmed;
    }

    private String sanitizeFirmName(String raw) {
        try {
            return FirmNameSanitizer.sanitize(raw);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
        }
    }

    private void deletePrefixQuietly(UUID firmId, String prefix) {
        try {
            blobStorage.deleteByPrefix(prefix);
        } catch (IOException e) {
            log.warn("Blob prefix delete failed firmId={} prefix={}: {}", firmId, prefix, e.getMessage());
        }
    }

    private FirmContracts.FirmSummary toSummary(FirmEntity firm, MemberRole role) {
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

    private FirmContracts.FirmStatusHistoryEntry toHistoryEntry(FirmStatusHistoryEntity entry) {
        return new FirmContracts.FirmStatusHistoryEntry(
                entry.getId(),
                entry.getPreviousStatus(),
                FirmStatusCatalog.displayLabel(entry.getPreviousStatus()),
                entry.getNewStatus(),
                FirmStatusCatalog.displayLabel(entry.getNewStatus()),
                entry.getMessage(),
                entry.getActorUserId(),
                entry.getSource(),
                entry.getCreatedAt()
        );
    }
}
