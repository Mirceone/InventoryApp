package com.mirceone.inventoryapp.service.documents;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.DocumentProcessingStatus;
import com.mirceone.inventoryapp.model.FirmDocumentEntity;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.FirmDocumentRepository;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.documents.storage.DocumentStorage;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final AppIntegrationProperties props;
    private final FirmAccessService firmAccessService;
    private final DossierService dossierService;
    private final FirmDocumentRepository firmDocumentRepository;
    private final DocumentStorage documentStorage;
    private final UserRepository userRepository;

    public DocumentService(
            AppIntegrationProperties props,
            FirmAccessService firmAccessService,
            DossierService dossierService,
            FirmDocumentRepository firmDocumentRepository,
            DocumentStorage documentStorage,
            UserRepository userRepository
    ) {
        this.props = props;
        this.firmAccessService = firmAccessService;
        this.dossierService = dossierService;
        this.firmDocumentRepository = firmDocumentRepository;
        this.documentStorage = documentStorage;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<DocumentSummary> listDocuments(UUID userId, UUID firmId, UUID dossierId, String folder, Pageable pageable) {
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        dossierService.requireDossier(firmId, dossierId);
        int max = Math.max(1, props.getDocuments().getPageMaxSize());
        Pageable effective = pageable.getPageSize() > max
                ? PageRequest.of(pageable.getPageNumber(), max)
                : pageable;
        String folderFilter = normalizeFolderFilter(folder);
        if (folderFilter != null) {
            String canonical = DocumentFolderTaxonomy.toCanonicalFolderPath(folderFilter);
            if (DocumentFolderTaxonomy.isFinalPath(canonical)) {
                return firmDocumentRepository.findSummariesByDossierId(dossierId, canonical, effective);
            }
        }
        return firmDocumentRepository.findSummariesByDossierId(dossierId, folderFilter, effective);
    }

    @Transactional(readOnly = true)
    public List<String> listFolders(UUID userId, UUID firmId, UUID dossierId) {
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        dossierService.requireDossier(firmId, dossierId);
        return firmDocumentRepository.findDistinctFolderPathsByDossierId(dossierId).stream()
                .filter(p -> p != null && !p.isBlank())
                .map(DocumentFolderTaxonomy::toCanonicalFolderPath)
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FolderStructureEntry> listFolderStructure(UUID userId, UUID firmId, UUID dossierId) {
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        dossierService.requireDossier(firmId, dossierId);
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : firmDocumentRepository.countClassifiedDocumentsByFolderPath(dossierId)) {
            String path = (String) row[0];
            Long count = (Long) row[1];
            if (path == null || path.isBlank() || count == null) {
                continue;
            }
            String canonical = DocumentFolderTaxonomy.toCanonicalFolderPath(path);
            counts.merge(canonical, count, Long::sum);
        }
        return DocumentFolderTaxonomy.allPaths().stream()
                .map(path -> new FolderStructureEntry(path, counts.getOrDefault(path, 0L)))
                .toList();
    }

    public DocumentSummary upload(UUID userId, UUID firmId, UUID dossierId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File is required");
        }
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        dossierService.requireDossier(firmId, dossierId);
        return toSummary(persistUpload(userId, firmId, dossierId, file));
    }

    public BatchUploadResult uploadBatch(UUID userId, UUID firmId, UUID dossierId, MultipartFile[] files) {
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        dossierService.requireDossier(firmId, dossierId);

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

        List<BatchUploadItem> accepted = new ArrayList<>();
        List<BatchUploadError> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                errors.add(new BatchUploadError(safeName(file), "File is empty"));
                continue;
            }
            try {
                FirmDocumentEntity entity = persistUpload(userId, firmId, dossierId, file);
                accepted.add(new BatchUploadItem(
                        entity.getId(),
                        entity.getOriginalFilename(),
                        entity.getProcessingStatus()
                ));
            } catch (ResponseStatusException ex) {
                errors.add(new BatchUploadError(safeName(file), ex.getReason()));
            } catch (Exception ex) {
                errors.add(new BatchUploadError(safeName(file), ex.getMessage() != null ? ex.getMessage() : "Upload failed"));
            }
        }

        return new BatchUploadResult(accepted, errors);
    }

    private FirmDocumentEntity persistUpload(UUID userId, UUID firmId, UUID dossierId, MultipartFile file) {
        String rawName = file.getOriginalFilename();
        String sanitized;
        try {
            sanitized = DocumentFilenameSanitizer.sanitizeOriginalFilename(rawName);
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

        UUID documentId = UUID.randomUUID();
        String ext = DocumentFilenameSanitizer.storageFileSuffix(sanitized);
        String storageKey = DocumentStorageKeys.pendingKey(firmId, dossierId, documentId, ext);

        String checksum;
        long bytesWritten;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(file.getInputStream(), md)) {
                bytesWritten = documentStorage.store(storageKey, in, size);
            }
            checksum = HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }

        long sizeToPersist = size >= 0 ? size : bytesWritten;
        if (sizeToPersist > maxBytes) {
            try {
                documentStorage.delete(storageKey);
            } catch (IOException cleanup) {
                log.warn("Rollback after size check failed key={}", storageKey);
            }
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "File exceeds maximum allowed size");
        }

        String storedMime = contentType != null && !contentType.isBlank() ? contentType.strip() : null;
        FirmDocumentEntity entity = new FirmDocumentEntity(
                documentId,
                firmId,
                dossierId,
                userId,
                sanitized,
                storedMime,
                sizeToPersist,
                storageKey,
                checksum
        );

        try {
            firmDocumentRepository.save(entity);
        } catch (RuntimeException e) {
            try {
                documentStorage.delete(storageKey);
            } catch (IOException cleanup) {
                log.warn("Rollback storage delete failed key={}: {}", storageKey, cleanup.getMessage());
            }
            throw e;
        }
        return entity;
    }

    @Transactional(readOnly = true)
    public DocumentDownload openForDownload(UUID userId, UUID firmId, UUID dossierId, UUID documentId) {
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        dossierService.requireDossier(firmId, dossierId);
        FirmDocumentEntity doc = firmDocumentRepository.findByIdAndFirmIdAndDossierId(documentId, firmId, dossierId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));
        if (doc.getProcessingStatus() == DocumentProcessingStatus.PENDING) {
            throw new ResponseStatusException(CONFLICT, "Document is still being organized");
        }
        if (doc.getProcessingStatus() == DocumentProcessingStatus.FAILED) {
            throw new ResponseStatusException(CONFLICT, "Document organization failed");
        }
        try {
            String mime = doc.getMimeType();
            if (mime == null || mime.isBlank()) {
                mime = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return new DocumentDownload(doc.getOriginalFilename(), mime, documentStorage.asResource(doc.getStorageKey()));
        } catch (IOException e) {
            throw new ResponseStatusException(NOT_FOUND, "File not found", e);
        }
    }

    @Transactional
    public void deleteDocument(UUID userId, UUID firmId, UUID dossierId, UUID documentId) {
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        dossierService.requireDossier(firmId, dossierId);
        FirmDocumentEntity doc = firmDocumentRepository.findByIdAndFirmIdAndDossierId(documentId, firmId, dossierId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));
        String key = doc.getStorageKey();
        firmDocumentRepository.delete(doc);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteStorageQuietly(firmId, documentId, key);
                }
            });
        } else {
            deleteStorageQuietly(firmId, documentId, key);
        }
    }

    private DocumentSummary toSummary(FirmDocumentEntity entity) {
        UserEntity uploader = userRepository.findById(entity.getUploadedByUserId())
                .orElseThrow(() -> new ResponseStatusException(INTERNAL_SERVER_ERROR, "User not found"));
        return new DocumentSummary(
                entity.getId(),
                entity.getFirmId(),
                entity.getDossierId(),
                entity.getOriginalFilename(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getCreatedAt(),
                entity.getUploadedByUserId(),
                uploader.getEmail(),
                entity.getFolderPath(),
                entity.getProcessingStatus(),
                entity.getOrganizationSource()
        );
    }

    private static String normalizeFolderFilter(String folder) {
        if (folder == null || folder.isBlank()) {
            return null;
        }
        return FolderPathSanitizer.sanitize(folder);
    }

    private static String safeName(MultipartFile file) {
        return file != null && file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
    }

    private void deleteStorageQuietly(UUID firmId, UUID documentId, String key) {
        try {
            documentStorage.delete(key);
        } catch (IOException e) {
            log.warn("Storage delete failed firmId={} documentId={} key={}: {}", firmId, documentId, key, e.getMessage());
        }
    }

    private void assertDossierEnabled() {
        if (!props.getFeatures().isDossierEnabled()) {
            throw new ResponseStatusException(FORBIDDEN, "Electronic folder feature is disabled");
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
    public static String contentDispositionAttachment(String originalFilename) {
        String ascii = originalFilename == null ? "download" : originalFilename.replace("\"", "'");
        String encoded = URLEncoder.encode(originalFilename != null ? originalFilename : "download", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }
}
