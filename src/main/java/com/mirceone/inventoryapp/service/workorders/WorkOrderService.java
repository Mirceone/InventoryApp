package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.FirmWorkOrderEntity;
import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;
import com.mirceone.inventoryapp.model.WorkOrderFolderRuleEntity;
import com.mirceone.inventoryapp.model.WorkOrderStatus;
import com.mirceone.inventoryapp.repository.FirmWorkOrderRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRuleRepository;
import com.mirceone.inventoryapp.repository.WorkOrderInvoiceRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import com.mirceone.inventoryapp.service.storage.BlobStorage;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
public class WorkOrderService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);

    private final AppIntegrationProperties props;
    private final FirmAccessService firmAccessService;
    private final FirmWorkOrderRepository firmWorkOrderRepository;
    private final WorkOrderFolderRepository folderRepository;
    private final WorkOrderFolderRuleRepository ruleRepository;
    private final WorkOrderFileRepository fileRepository;
    private final WorkOrderInvoiceRepository invoiceRepository;
    private final DefaultFolderTemplate defaultFolderTemplate;
    private final BlobStorage blobStorage;
    private final AfterCommitExecutor afterCommitExecutor;

    public WorkOrderService(
            AppIntegrationProperties props,
            FirmAccessService firmAccessService,
            FirmWorkOrderRepository firmWorkOrderRepository,
            WorkOrderFolderRepository folderRepository,
            WorkOrderFolderRuleRepository ruleRepository,
            WorkOrderFileRepository fileRepository,
            WorkOrderInvoiceRepository invoiceRepository,
            DefaultFolderTemplate defaultFolderTemplate,
            BlobStorage blobStorage,
            AfterCommitExecutor afterCommitExecutor
    ) {
        this.props = props;
        this.firmAccessService = firmAccessService;
        this.firmWorkOrderRepository = firmWorkOrderRepository;
        this.folderRepository = folderRepository;
        this.ruleRepository = ruleRepository;
        this.fileRepository = fileRepository;
        this.invoiceRepository = invoiceRepository;
        this.defaultFolderTemplate = defaultFolderTemplate;
        this.blobStorage = blobStorage;
        this.afterCommitExecutor = afterCommitExecutor;
    }

    @Transactional
    public WorkOrderSummary createWorkOrder(UUID userId, UUID firmId, WorkOrderContracts.CreateWorkOrderSpec spec) {
        assertWorkOrderEnabled();
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);

        String name = WorkOrderNameSanitizer.sanitize(spec.name());
        String clientName = WorkOrderTextSanitizer.sanitizeRequiredName(spec.clientName(), "Client name");
        String location = WorkOrderTextSanitizer.sanitizeRequiredName(spec.location(), "Location");
        String description = WorkOrderTextSanitizer.sanitizeOptionalDescription(spec.description());
        LocalDate estimatedEndDate = spec.estimatedEndDate();
        validateEstimatedEndDateOnCreate(estimatedEndDate);

        if (firmWorkOrderRepository.existsByFirmIdAndNameIgnoreCase(firmId, name)) {
            throw new ResponseStatusException(CONFLICT, "A work order with this name already exists");
        }
        try {
            FirmWorkOrderEntity saved = firmWorkOrderRepository.save(new FirmWorkOrderEntity(
                    firmId,
                    name,
                    clientName,
                    location,
                    description,
                    estimatedEndDate,
                    userId
            ));
            seedDefaultFolders(saved.getId());
            return toSummary(saved, 0);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "A work order with this name already exists", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<WorkOrderSummary> listWorkOrders(UUID userId, UUID firmId) {
        assertWorkOrderEnabled();
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        return firmWorkOrderRepository.findAllByFirmIdOrderByCreatedAtDesc(firmId).stream()
                .map(d -> toSummary(d, fileRepository.countByWorkOrderId(d.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkOrderSummary getWorkOrder(UUID userId, UUID firmId, UUID workOrderId) {
        assertWorkOrderEnabled();
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        FirmWorkOrderEntity workOrder = requireWorkOrder(firmId, workOrderId);
        return toSummary(workOrder, fileRepository.countByWorkOrderId(workOrderId));
    }

    @Transactional
    public WorkOrderSummary updateWorkOrder(
            UUID userId,
            UUID firmId,
            UUID workOrderId,
            WorkOrderContracts.UpdateWorkOrderSpec spec
    ) {
        assertWorkOrderEnabled();
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        FirmWorkOrderEntity workOrder = requireWorkOrder(firmId, workOrderId);

        if (spec.name() == null
                && spec.clientName() == null
                && spec.location() == null
                && !spec.clearDescription()
                && spec.description() == null
                && spec.estimatedEndDate() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "No fields to update");
        }

        if (spec.name() != null) {
            String name = WorkOrderNameSanitizer.sanitize(spec.name());
            if (firmWorkOrderRepository.existsByFirmIdAndNameIgnoreCaseAndIdNot(firmId, name, workOrderId)) {
                throw new ResponseStatusException(CONFLICT, "A work order with this name already exists");
            }
            workOrder.setName(name);
        }
        if (spec.clientName() != null) {
            workOrder.setClientName(WorkOrderTextSanitizer.sanitizeRequiredName(spec.clientName(), "Client name"));
        }
        if (spec.location() != null) {
            workOrder.setLocation(WorkOrderTextSanitizer.sanitizeRequiredName(spec.location(), "Location"));
        }
        if (spec.clearDescription()) {
            workOrder.setDescription(null);
        } else if (spec.description() != null) {
            workOrder.setDescription(WorkOrderTextSanitizer.sanitizeOptionalDescription(spec.description()));
        }
        if (spec.estimatedEndDate() != null) {
            validateEstimatedEndDateOnUpdate(spec.estimatedEndDate(), workOrder.getCreatedAt());
            workOrder.setEstimatedEndDate(spec.estimatedEndDate());
        }

        try {
            FirmWorkOrderEntity saved = firmWorkOrderRepository.save(workOrder);
            return toSummary(saved, fileRepository.countByWorkOrderId(workOrderId));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "A work order with this name already exists", ex);
        }
    }

    @Transactional
    public WorkOrderSummary updateWorkOrderStatus(
            UUID userId,
            UUID firmId,
            UUID workOrderId,
            WorkOrderStatus status
    ) {
        assertWorkOrderEnabled();
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        FirmWorkOrderEntity workOrder = requireWorkOrder(firmId, workOrderId);
        workOrder.setStatus(status);
        FirmWorkOrderEntity saved = firmWorkOrderRepository.save(workOrder);
        return toSummary(saved, fileRepository.countByWorkOrderId(workOrderId));
    }

    @Transactional
    public void deleteWorkOrder(UUID userId, UUID firmId, UUID workOrderId) {
        assertWorkOrderEnabled();
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        requireWorkOrder(firmId, workOrderId);
        // Bulk-delete file rows first so the restrictive folder FK does not block the
        // cascading delete of the folder tree.
        fileRepository.deleteByWorkOrderId(workOrderId);
        invoiceRepository.deleteByWorkOrderId(workOrderId);
        firmWorkOrderRepository.deleteById(workOrderId);
        String prefix = WorkOrderStorageKeys.workOrderPrefix(firmId, workOrderId);
        afterCommitExecutor.execute(() -> deletePrefixQuietly(prefix));
    }

    public FirmWorkOrderEntity requireWorkOrder(UUID firmId, UUID workOrderId) {
        return firmWorkOrderRepository.findByIdAndFirmId(workOrderId, firmId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Work order not found"));
    }

    void assertWorkOrderEnabled() {
        if (!props.getFeatures().isWorkOrderEnabled()) {
            throw new ResponseStatusException(FORBIDDEN, "Work order feature is disabled");
        }
    }

    private void seedDefaultFolders(UUID workOrderId) {
        List<WorkOrderFolderEntity> folders = new ArrayList<>();
        List<WorkOrderFolderRuleEntity> rules = new ArrayList<>();
        int sortOrder = 0;
        for (DefaultFolderTemplate.TemplateFolder template : defaultFolderTemplate.folders()) {
            WorkOrderFolderEntity folder = new WorkOrderFolderEntity(
                    workOrderId,
                    null,
                    template.name(),
                    template.catchAll(),
                    sortOrder++
            );
            folders.add(folder);
            for (String extension : template.extensions()) {
                rules.add(new WorkOrderFolderRuleEntity(
                        workOrderId,
                        folder.getId(),
                        ExtensionNormalizer.normalizeRuleExtension(extension)
                ));
            }
        }
        folderRepository.saveAll(folders);
        if (!rules.isEmpty()) {
            ruleRepository.saveAll(rules);
        }
    }

    private void validateEstimatedEndDateOnCreate(LocalDate estimatedEndDate) {
        LocalDate startDate = LocalDate.now(ZoneOffset.UTC);
        if (estimatedEndDate.isBefore(startDate)) {
            throw new ResponseStatusException(BAD_REQUEST, "estimatedEndDate cannot be before start date");
        }
    }

    private void validateEstimatedEndDateOnUpdate(LocalDate estimatedEndDate, Instant createdAt) {
        LocalDate startDate = createdAt.atZone(ZoneOffset.UTC).toLocalDate();
        if (estimatedEndDate.isBefore(startDate)) {
            throw new ResponseStatusException(BAD_REQUEST, "estimatedEndDate cannot be before start date");
        }
    }

    private void deletePrefixQuietly(String prefix) {
        try {
            blobStorage.deleteByPrefix(prefix);
        } catch (IOException e) {
            log.warn("Blob prefix delete failed prefix={}: {}", prefix, e.getMessage());
        }
    }

    private WorkOrderSummary toSummary(FirmWorkOrderEntity entity, long fileCount) {
        return new WorkOrderSummary(
                entity.getId(),
                entity.getFirmId(),
                entity.getName(),
                entity.getClientName(),
                entity.getLocation(),
                entity.getDescription(),
                entity.getEstimatedEndDate(),
                entity.getStatus(),
                entity.getCreatedByUserId(),
                entity.getCreatedAt(),
                fileCount
        );
    }
}
