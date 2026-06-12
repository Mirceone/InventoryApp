package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.model.WorkOrderInvoiceEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.repository.WorkOrderInvoiceRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import com.mirceone.inventoryapp.service.storage.BlobStorage;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import com.mirceone.inventoryapp.service.workorders.invoices.InvoiceDebugLog;
import com.mirceone.inventoryapp.service.workorders.invoices.InvoiceProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
public class WorkOrderInvoiceService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderInvoiceService.class);

    private final AppIntegrationProperties props;
    private final FirmAccessService firmAccessService;
    private final WorkOrderService workOrderService;
    private final WorkOrderInvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final BlobStorage blobStorage;
    private final AfterCommitExecutor afterCommitExecutor;
    private final InvoiceProcessingService processingService;

    public WorkOrderInvoiceService(
            AppIntegrationProperties props,
            FirmAccessService firmAccessService,
            WorkOrderService workOrderService,
            WorkOrderInvoiceRepository invoiceRepository,
            UserRepository userRepository,
            BlobStorage blobStorage,
            AfterCommitExecutor afterCommitExecutor,
            InvoiceProcessingService processingService
    ) {
        this.props = props;
        this.firmAccessService = firmAccessService;
        this.workOrderService = workOrderService;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.blobStorage = blobStorage;
        this.afterCommitExecutor = afterCommitExecutor;
        this.processingService = processingService;
    }

    public InvoiceSummary upload(UUID userId, UUID firmId, UUID workOrderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File is required");
        }
        requireAccess(userId, firmId, workOrderId);
        return toDetailSummary(persistUpload(userId, firmId, workOrderId, file));
    }

    public BatchInvoiceUploadResult uploadBatch(UUID userId, UUID firmId, UUID workOrderId, MultipartFile[] files) {
        requireAccess(userId, firmId, workOrderId);

        if (files == null || files.length == 0) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one file is required");
        }

        int maxFiles = props.getInvoices().getBatchMaxFiles();
        if (files.length > maxFiles) {
            throw new ResponseStatusException(BAD_REQUEST, "Too many files (max " + maxFiles + ")");
        }

        long maxTotal = props.getInvoices().getBatchMaxTotalBytes();
        long totalSize = 0;
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty() && f.getSize() >= 0) {
                totalSize += f.getSize();
            }
        }
        if (totalSize > maxTotal) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "Batch exceeds maximum total size");
        }

        List<BatchInvoiceUploadResult.BatchInvoiceUploadItem> accepted = new ArrayList<>();
        List<BatchInvoiceUploadResult.BatchInvoiceUploadError> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                errors.add(new BatchInvoiceUploadResult.BatchInvoiceUploadError(safeName(file), "File is empty"));
                continue;
            }
            try {
                WorkOrderInvoiceEntity entity = persistUpload(userId, firmId, workOrderId, file);
                accepted.add(new BatchInvoiceUploadResult.BatchInvoiceUploadItem(
                        entity.getId(),
                        entity.getDisplayName(),
                        entity.getProcessingStatus()
                ));
            } catch (ResponseStatusException ex) {
                errors.add(new BatchInvoiceUploadResult.BatchInvoiceUploadError(safeName(file), ex.getReason()));
            } catch (Exception ex) {
                errors.add(new BatchInvoiceUploadResult.BatchInvoiceUploadError(
                        safeName(file),
                        ex.getMessage() != null ? ex.getMessage() : "Upload failed"));
            }
        }

        return new BatchInvoiceUploadResult(accepted, errors);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceSummary> listInvoices(UUID userId, UUID firmId, UUID workOrderId, Pageable pageable) {
        requireAccess(userId, firmId, workOrderId);
        int max = Math.max(1, props.getInvoices().getPageMaxSize());
        Pageable effective = pageable.getPageSize() > max
                ? PageRequest.of(pageable.getPageNumber(), max)
                : pageable;

        Page<WorkOrderInvoiceEntity> page =
                invoiceRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId, effective);
        Map<UUID, String> emails = uploaderEmails(page.getContent());
        return page.map(entity -> toListSummary(entity, emails));
    }

    @Transactional(readOnly = true)
    public InvoiceSummary getInvoice(UUID userId, UUID firmId, UUID workOrderId, UUID invoiceId) {
        requireAccess(userId, firmId, workOrderId);
        WorkOrderInvoiceEntity invoice = requireInvoice(firmId, workOrderId, invoiceId);
        return toDetailSummary(invoice);
    }

    @Transactional(readOnly = true)
    public FileDownload openForDownload(UUID userId, UUID firmId, UUID workOrderId, UUID invoiceId) {
        requireAccess(userId, firmId, workOrderId);
        WorkOrderInvoiceEntity invoice = requireInvoice(firmId, workOrderId, invoiceId);
        try {
            String mime = invoice.getMimeType();
            if (mime == null || mime.isBlank()) {
                mime = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return new FileDownload(invoice.getDisplayName(), mime, blobStorage.open(invoice.getStorageKey()));
        } catch (IOException e) {
            throw new ResponseStatusException(NOT_FOUND, "Invoice content not found", e);
        }
    }

    @Transactional
    public void deleteInvoice(UUID userId, UUID firmId, UUID workOrderId, UUID invoiceId) {
        requireAccess(userId, firmId, workOrderId);
        WorkOrderInvoiceEntity invoice = requireInvoice(firmId, workOrderId, invoiceId);
        String key = invoice.getStorageKey();
        invoiceRepository.delete(invoice);
        afterCommitExecutor.execute(() -> deleteBlobQuietly(key));
    }

    @Transactional
    public InvoiceSummary retryProcessing(UUID userId, UUID firmId, UUID workOrderId, UUID invoiceId) {
        requireAccess(userId, firmId, workOrderId);
        WorkOrderInvoiceEntity invoice = requireInvoice(firmId, workOrderId, invoiceId);
        if (invoice.getProcessingStatus() != InvoiceProcessingStatus.FAILED) {
            throw new ResponseStatusException(BAD_REQUEST, "Only failed invoices can be retried");
        }
        invoice.setProcessingStatus(InvoiceProcessingStatus.PENDING);
        invoice.setMarkdownText(null);
        invoice.setProcessingError(null);
        invoice.setProcessedAt(null);
        WorkOrderInvoiceEntity saved = invoiceRepository.saveAndFlush(invoice);
        UUID savedId = saved.getId();
        afterCommitExecutor.execute(() -> processingService.processAsync(savedId));
        return toDetailSummary(saved);
    }

    private WorkOrderInvoiceEntity persistUpload(UUID userId, UUID firmId, UUID workOrderId, MultipartFile file) {
        String sanitized;
        try {
            sanitized = FileNameSanitizer.sanitizeDisplayName(file.getOriginalFilename());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
        }

        long maxBytes = props.getStorage().getMaxFileSizeBytes();
        long size = file.getSize();
        if (size >= 0 && size > maxBytes) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "File exceeds maximum allowed size");
        }

        String contentType = file.getContentType();
        if (!isInvoiceMimeAllowed(contentType)) {
            throw new ResponseStatusException(BAD_REQUEST, "MIME type not allowed for invoices");
        }

        String extension = ExtensionNormalizer.fromFilename(sanitized);
        UUID invoiceId = UUID.randomUUID();
        String storageKey = InvoiceStorageKeys.invoiceKey(
                firmId, workOrderId, invoiceId, FileNameSanitizer.storageFileSuffix(sanitized));

        String checksum;
        long bytesWritten;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(file.getInputStream(), md)) {
                bytesWritten = blobStorage.store(storageKey, in, size);
            }
            checksum = HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to store invoice", e);
        }

        long sizeToPersist = size >= 0 ? size : bytesWritten;
        if (sizeToPersist > maxBytes) {
            deleteBlobQuietly(storageKey);
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "File exceeds maximum allowed size");
        }

        String storedMime = contentType != null && !contentType.isBlank() ? contentType.strip() : null;

        try {
            WorkOrderInvoiceEntity saved = invoiceRepository.saveAndFlush(new WorkOrderInvoiceEntity(
                    invoiceId,
                    firmId,
                    workOrderId,
                    userId,
                    sanitized,
                    extension.isEmpty() ? null : extension,
                    storedMime,
                    sizeToPersist,
                    checksum,
                    storageKey
            ));
            UUID savedId = saved.getId();
            // #region agent log
            InvoiceDebugLog.write(
                    "E", "WorkOrderInvoiceService.persistUpload",
                    "invoice saved, scheduling async processing",
                    InvoiceDebugLog.data(
                            "invoiceId", savedId,
                            "displayName", sanitized,
                            "mimeType", storedMime,
                            "status", saved.getProcessingStatus().name()));
            // #endregion
            afterCommitExecutor.execute(() -> processingService.processAsync(savedId));
            return saved;
        } catch (RuntimeException e) {
            deleteBlobQuietly(storageKey);
            throw e;
        }
    }

    private WorkOrderInvoiceEntity requireInvoice(UUID firmId, UUID workOrderId, UUID invoiceId) {
        return invoiceRepository.findByIdAndFirmIdAndWorkOrderId(invoiceId, firmId, workOrderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Invoice not found"));
    }

    private void requireAccess(UUID userId, UUID firmId, UUID workOrderId) {
        workOrderService.assertWorkOrderEnabled();
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        workOrderService.requireWorkOrder(firmId, workOrderId);
    }

    private Map<UUID, String> uploaderEmails(List<WorkOrderInvoiceEntity> invoices) {
        List<UUID> userIds = invoices.stream()
                .map(WorkOrderInvoiceEntity::getUploadedByUserId)
                .distinct()
                .toList();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u.getEmail() != null ? u.getEmail() : "", (a, b) -> a));
    }

    private InvoiceSummary toListSummary(WorkOrderInvoiceEntity entity, Map<UUID, String> emails) {
        return InvoiceSummary.listItem(
                entity.getId(),
                entity.getWorkOrderId(),
                entity.getDisplayName(),
                entity.getExtension(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getProcessingStatus(),
                entity.getCreatedAt(),
                entity.getProcessedAt(),
                entity.getUploadedByUserId(),
                emails.getOrDefault(entity.getUploadedByUserId(), "")
        );
    }

    private InvoiceSummary toDetailSummary(WorkOrderInvoiceEntity entity) {
        Map<UUID, String> emails = uploaderEmails(List.of(entity));
        return new InvoiceSummary(
                entity.getId(),
                entity.getWorkOrderId(),
                entity.getDisplayName(),
                entity.getExtension(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getProcessingStatus(),
                entity.getProcessingStatus() == InvoiceProcessingStatus.READY ? entity.getMarkdownText() : null,
                entity.getProcessingStatus() == InvoiceProcessingStatus.FAILED ? entity.getProcessingError() : null,
                entity.getCreatedAt(),
                entity.getProcessedAt(),
                entity.getUploadedByUserId(),
                emails.getOrDefault(entity.getUploadedByUserId(), "")
        );
    }

    private static String safeName(MultipartFile file) {
        return file != null && file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
    }

    private void deleteBlobQuietly(String key) {
        try {
            blobStorage.delete(key);
        } catch (IOException e) {
            log.warn("Blob delete failed key={}: {}", key, e.getMessage());
        }
    }

    private boolean isInvoiceMimeAllowed(String contentType) {
        List<String> prefixes = props.getInvoices().getAllowedMimePrefixes();
        if (prefixes == null || prefixes.isEmpty()) {
            return true;
        }
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String ct = contentType.strip().toLowerCase(Locale.ROOT);
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isBlank()) {
                continue;
            }
            String p = prefix.strip();
            if (p.endsWith("/")) {
                if (ct.startsWith(p.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            } else if (ct.equalsIgnoreCase(p) || ct.startsWith(p.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
