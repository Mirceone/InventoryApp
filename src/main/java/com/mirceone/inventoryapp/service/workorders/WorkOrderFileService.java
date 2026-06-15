package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.FileClassificationSource;
import com.mirceone.inventoryapp.model.FileClassificationStatus;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.model.WorkOrderFileEntity;
import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import com.mirceone.inventoryapp.service.storage.BlobStorage;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import com.mirceone.inventoryapp.service.workorders.classification.FileClassificationService;
import com.mirceone.inventoryapp.service.workorders.classification.UploadClassification;
import com.mirceone.inventoryapp.service.workorders.classification.WorkOrderFileClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

/**
 * Upload, listing, download, rename, manual move and delete of work order files.
 * Classification uses extension rules and filename heuristics synchronously; when AI is
 * enabled, unmatched files are placed in the catch-all folder with PENDING status and
 * classified asynchronously by {@link com.mirceone.inventoryapp.service.workorders.classification.FileClassificationWorker}.
 */
@Service
public class WorkOrderFileService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderFileService.class);
    private static final int INSERT_RETRY_ATTEMPTS = 3;

    private final AppIntegrationProperties props;
    private final FirmAccessService firmAccessService;
    private final WorkOrderService workOrderService;
    private final WorkOrderFolderService folderService;
    private final WorkOrderFileClassifier workOrderFileClassifier;
    private final FileClassificationService fileClassificationService;
    private final WorkOrderFileRepository fileRepository;
    private final WorkOrderFolderRepository folderRepository;
    private final UserRepository userRepository;
    private final BlobStorage blobStorage;
    private final DisplayNameDeduplicator displayNameDeduplicator;
    private final AfterCommitExecutor afterCommitExecutor;

    public WorkOrderFileService(
            AppIntegrationProperties props,
            FirmAccessService firmAccessService,
            WorkOrderService workOrderService,
            WorkOrderFolderService folderService,
            WorkOrderFileClassifier workOrderFileClassifier,
            FileClassificationService fileClassificationService,
            WorkOrderFileRepository fileRepository,
            WorkOrderFolderRepository folderRepository,
            UserRepository userRepository,
            BlobStorage blobStorage,
            DisplayNameDeduplicator displayNameDeduplicator,
            AfterCommitExecutor afterCommitExecutor
    ) {
        this.props = props;
        this.firmAccessService = firmAccessService;
        this.workOrderService = workOrderService;
        this.folderService = folderService;
        this.workOrderFileClassifier = workOrderFileClassifier;
        this.fileClassificationService = fileClassificationService;
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.blobStorage = blobStorage;
        this.displayNameDeduplicator = displayNameDeduplicator;
        this.afterCommitExecutor = afterCommitExecutor;
    }

    // Intentionally NOT @Transactional: the blob is streamed to storage here, and we must not
    // hold a DB connection/transaction open for the duration of a (potentially large) upload.
    // The single row insert runs in its own transaction (insertWithUniqueName); afterCommit
    // scheduling therefore degrades to immediate execution, which is safe because the row is
    // already durably committed before processAsync is invoked.
    public FileSummary upload(UUID userId, UUID firmId, UUID workOrderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File is required");
        }
        requireAccess(userId, firmId, workOrderId);
        return toSummary(persistUpload(userId, firmId, workOrderId, file));
    }

    public BatchUploadResult uploadBatch(UUID userId, UUID firmId, UUID workOrderId, MultipartFile[] files) {
        requireAccess(userId, firmId, workOrderId);

        if (files == null || files.length == 0) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one file is required");
        }

        int maxFiles = props.getDocuments().getBatchMaxFiles();
        if (files.length > maxFiles) {
            throw new ResponseStatusException(BAD_REQUEST, "Too many files (max " + maxFiles + ")");
        }

        long maxTotal = props.getDocuments().getBatchMaxTotalBytes();
        long totalSize = 0;
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty() && f.getSize() >= 0) {
                totalSize += f.getSize();
            }
        }
        if (totalSize > maxTotal) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "Batch exceeds maximum total size");
        }

        Map<UUID, String> paths = folderPaths(workOrderId);
        List<BatchUploadResult.BatchUploadItem> accepted = new ArrayList<>();
        List<BatchUploadResult.BatchUploadError> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                errors.add(new BatchUploadResult.BatchUploadError(safeName(file), "File is empty"));
                continue;
            }
            try {
                WorkOrderFileEntity entity = persistUpload(userId, firmId, workOrderId, file);
                accepted.add(new BatchUploadResult.BatchUploadItem(
                        entity.getId(),
                        entity.getDisplayName(),
                        entity.getFolderId(),
                        paths.get(entity.getFolderId())
                ));
            } catch (ResponseStatusException ex) {
                errors.add(new BatchUploadResult.BatchUploadError(safeName(file), ex.getReason()));
            } catch (Exception ex) {
                errors.add(new BatchUploadResult.BatchUploadError(
                        safeName(file),
                        ex.getMessage() != null ? ex.getMessage() : "Upload failed"));
            }
        }

        return new BatchUploadResult(accepted, errors);
    }

    @Transactional(readOnly = true)
    public Page<FileSummary> listFiles(UUID userId, UUID firmId, UUID workOrderId, UUID folderId, Pageable pageable) {
        requireAccess(userId, firmId, workOrderId);
        int max = Math.max(1, props.getDocuments().getPageMaxSize());
        Pageable effective = pageable.getPageSize() > max
                ? PageRequest.of(pageable.getPageNumber(), max)
                : pageable;

        Page<WorkOrderFileEntity> page;
        if (folderId != null) {
            folderService.requireFolder(workOrderId, folderId);
            page = fileRepository.findByFolderIdOrderByCreatedAtDesc(folderId, effective);
        } else {
            page = fileRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId, effective);
        }

        Map<UUID, String> paths = folderPaths(workOrderId);
        Map<UUID, String> emails = uploaderEmails(page.getContent());
        return page.map(entity -> toSummary(entity, paths, emails));
    }

    @Transactional(readOnly = true)
    public FileDownload openForDownload(UUID userId, UUID firmId, UUID workOrderId, UUID fileId) {
        requireAccess(userId, firmId, workOrderId);
        WorkOrderFileEntity file = requireFile(firmId, workOrderId, fileId);
        try {
            String mime = file.getMimeType();
            if (mime == null || mime.isBlank()) {
                mime = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return new FileDownload(file.getDisplayName(), mime, blobStorage.open(file.getStorageKey()));
        } catch (IOException e) {
            throw new ResponseStatusException(NOT_FOUND, "File content not found", e);
        }
    }

    @Transactional
    public FileSummary updateFile(
            UUID userId,
            UUID firmId,
            UUID workOrderId,
            UUID fileId,
            WorkOrderContracts.UpdateFileSpec spec
    ) {
        requireAccess(userId, firmId, workOrderId);
        WorkOrderFileEntity file = requireFile(firmId, workOrderId, fileId);

        if (spec.displayName() == null && spec.folderId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "No fields to update");
        }

        if (spec.folderId() != null && !spec.folderId().equals(file.getFolderId())) {
            WorkOrderFolderEntity target = folderService.requireFolder(workOrderId, spec.folderId());
            file.setFolderId(target.getId());
            file.setClassificationStatus(FileClassificationStatus.CLASSIFIED);
            file.setClassificationSource(FileClassificationSource.MANUAL);
            file.setClassificationError(null);
            if (spec.displayName() == null) {
                file.setDisplayName(displayNameDeduplicator.uniqueName(target.getId(), file.getDisplayName()));
            }
        }

        if (spec.displayName() != null) {
            String name;
            try {
                name = FileNameSanitizer.sanitizeDisplayName(spec.displayName());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
            }
            if (!name.equalsIgnoreCase(file.getDisplayName())
                    && fileRepository.existsByFolderIdAndDisplayNameIgnoreCase(file.getFolderId(), name)) {
                throw new ResponseStatusException(CONFLICT, "A file with this name already exists in the folder");
            }
            file.setDisplayName(name);
        }

        try {
            fileRepository.saveAndFlush(file);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "A file with this name already exists in the folder", ex);
        }
        return toSummary(file);
    }

    @Transactional
    public void deleteFile(UUID userId, UUID firmId, UUID workOrderId, UUID fileId) {
        requireAccess(userId, firmId, workOrderId);
        WorkOrderFileEntity file = requireFile(firmId, workOrderId, fileId);
        String key = file.getStorageKey();
        fileRepository.delete(file);
        afterCommitExecutor.execute(() -> deleteBlobQuietly(key));
    }

    private WorkOrderFileEntity persistUpload(UUID userId, UUID firmId, UUID workOrderId, MultipartFile file) {
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
        if (!isMimeAllowed(contentType)) {
            throw new ResponseStatusException(BAD_REQUEST, "MIME type not allowed");
        }

        String extension = ExtensionNormalizer.fromFilename(sanitized);

        UUID fileId = UUID.randomUUID();
        String storageKey = WorkOrderStorageKeys.fileKey(
                firmId, workOrderId, fileId, FileNameSanitizer.storageFileSuffix(sanitized));

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
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }

        long sizeToPersist = size >= 0 ? size : bytesWritten;
        if (sizeToPersist > maxBytes) {
            deleteBlobQuietly(storageKey);
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "File exceeds maximum allowed size");
        }

        String storedMime = contentType != null && !contentType.isBlank() ? contentType.strip() : null;
        UploadClassification classification = workOrderFileClassifier.classifyOnUpload(
                workOrderId, sanitized, storedMime, extension);

        try {
            WorkOrderFileEntity saved = insertWithUniqueName(
                    fileId, firmId, workOrderId, classification.folderId(), userId,
                    sanitized, extension, storedMime, sizeToPersist, checksum, storageKey,
                    classification.status(), classification.source());
            if (classification.status() == FileClassificationStatus.PENDING) {
                UUID savedId = saved.getId();
                afterCommitExecutor.execute(() -> fileClassificationService.processAsync(savedId));
            }
            return saved;
        } catch (RuntimeException e) {
            // Compensation: the blob was written before the row; remove it on failure.
            deleteBlobQuietly(storageKey);
            throw e;
        }
    }

    private WorkOrderFileEntity insertWithUniqueName(
            UUID fileId,
            UUID firmId,
            UUID workOrderId,
            UUID folderId,
            UUID userId,
            String desiredName,
            String extension,
            String mimeType,
            long sizeBytes,
            String checksum,
            String storageKey,
            FileClassificationStatus classificationStatus,
            FileClassificationSource classificationSource
    ) {
        DataIntegrityViolationException last = null;
        for (int attempt = 0; attempt < INSERT_RETRY_ATTEMPTS; attempt++) {
            String name = displayNameDeduplicator.uniqueName(folderId, desiredName);
            try {
                return fileRepository.saveAndFlush(new WorkOrderFileEntity(
                        fileId,
                        firmId,
                        workOrderId,
                        folderId,
                        userId,
                        name,
                        extension.isEmpty() ? null : extension,
                        mimeType,
                        sizeBytes,
                        checksum,
                        storageKey,
                        classificationStatus,
                        classificationSource
                ));
            } catch (DataIntegrityViolationException e) {
                // Concurrent upload claimed the same name; recompute and retry.
                last = e;
            }
        }
        throw new ResponseStatusException(CONFLICT, "A file with this name already exists in the folder", last);
    }

    private WorkOrderFileEntity requireFile(UUID firmId, UUID workOrderId, UUID fileId) {
        return fileRepository.findByIdAndFirmIdAndWorkOrderId(fileId, firmId, workOrderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "File not found"));
    }

    private void requireAccess(UUID userId, UUID firmId, UUID workOrderId) {
        workOrderService.assertWorkOrderEnabled();
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        workOrderService.requireWorkOrder(firmId, workOrderId);
    }

    private Map<UUID, String> folderPaths(UUID workOrderId) {
        return FolderPaths.buildPathMap(
                folderRepository.findAllByWorkOrderIdOrderBySortOrderAscNameAsc(workOrderId));
    }

    private Map<UUID, String> uploaderEmails(List<WorkOrderFileEntity> files) {
        List<UUID> userIds = files.stream()
                .map(WorkOrderFileEntity::getUploadedByUserId)
                .distinct()
                .toList();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u.getEmail() != null ? u.getEmail() : "", (a, b) -> a));
    }

    private FileSummary toSummary(WorkOrderFileEntity entity) {
        Map<UUID, String> paths = folderPaths(entity.getWorkOrderId());
        Map<UUID, String> emails = uploaderEmails(List.of(entity));
        return toSummary(entity, paths, emails);
    }

    private FileSummary toSummary(WorkOrderFileEntity entity, Map<UUID, String> paths, Map<UUID, String> emails) {
        return new FileSummary(
                entity.getId(),
                entity.getFirmId(),
                entity.getWorkOrderId(),
                entity.getFolderId(),
                paths.get(entity.getFolderId()),
                entity.getDisplayName(),
                entity.getExtension(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getCreatedAt(),
                entity.getUploadedByUserId(),
                emails.getOrDefault(entity.getUploadedByUserId(), ""),
                entity.getClassificationStatus(),
                entity.getClassificationSource(),
                entity.getClassificationError()
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

    private boolean isMimeAllowed(String contentType) {
        List<String> prefixes = props.getStorage().getAllowedMimePrefixes();
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

    /** RFC 5987 filename* for Content-Disposition (ASCII fallback + UTF-8 encoded). */
    public static String contentDispositionAttachment(String displayName) {
        String ascii = displayName == null ? "download" : displayName.replace("\"", "'");
        String encoded = URLEncoder.encode(displayName != null ? displayName : "download", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }
}
