package com.mirceone.inventoryapp.service.firms;

import com.mirceone.inventoryapp.model.FirmDocumentEntity;
import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.repository.FirmDocumentRepository;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.service.documents.storage.DocumentStorage;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import com.mirceone.inventoryapp.service.firms.access.FirmStatusCatalog;
import com.mirceone.inventoryapp.service.firms.access.MemberRoleCatalog;
import com.mirceone.inventoryapp.service.inventory.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
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
    private final FirmDocumentRepository firmDocumentRepository;
    private final DocumentStorage documentStorage;
    private final CategoryService categoryService;
    private final FirmAccessService firmAccessService;

    public FirmService(
            FirmRepository firmRepository,
            FirmMemberRepository firmMemberRepository,
            FirmDocumentRepository firmDocumentRepository,
            DocumentStorage documentStorage,
            CategoryService categoryService,
            FirmAccessService firmAccessService
    ) {
        this.firmRepository = firmRepository;
        this.firmMemberRepository = firmMemberRepository;
        this.firmDocumentRepository = firmDocumentRepository;
        this.documentStorage = documentStorage;
        this.categoryService = categoryService;
        this.firmAccessService = firmAccessService;
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
        firmAccessService.requirePermission(firmId, userId, FirmPermission.FIRM_UPDATE);
        firmAccessService.requireFirmOperational(firmId);
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
        applyStatus(firm, spec.status(), spec.message());
        FirmEntity saved = firmRepository.save(firm);
        MemberRole role = firmAccessService.resolveMembership(firmId, userId).role();
        return toSummary(saved, role);
    }

    /**
     * Sets firm status from server-side code only (e.g. health checks). Not exposed over HTTP.
     */
    @Transactional
    public void setFirmStatusSystem(UUID firmId, FirmStatus status, String message) {
        FirmEntity firm = requireFirm(firmId);
        applyStatus(firm, status, message);
        firmRepository.save(firm);
    }

    @Transactional
    public void deleteFirm(UUID userId, UUID firmId) {
        firmAccessService.requirePermission(firmId, userId, FirmPermission.FIRM_DELETE);
        requireFirm(firmId);
        List<String> storageKeys = firmDocumentRepository.findAllByFirmId(firmId).stream()
                .map(FirmDocumentEntity::getStorageKey)
                .toList();
        firmRepository.deleteById(firmId);
        scheduleStorageDeletes(firmId, storageKeys);
    }

    public void assertUserIsMember(UUID firmId, UUID userId) {
        firmAccessService.requireMembership(firmId, userId);
    }

    public void assertUserIsOwner(UUID firmId, UUID userId) {
        firmAccessService.requireOwner(firmId, userId);
    }

    private FirmEntity requireFirm(UUID firmId) {
        return firmRepository.findById(firmId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Firm not found"));
    }

    private void applyStatus(FirmEntity firm, FirmStatus status, String message) {
        firm.setStatus(status);
        firm.setStatusMessage(normalizeStatusMessage(message));
        firm.setStatusUpdatedAt(Instant.now());
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

    private void scheduleStorageDeletes(UUID firmId, List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        List<String> copy = new ArrayList<>(keys);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteKeysQuietly(firmId, copy);
                }
            });
        } else {
            deleteKeysQuietly(firmId, copy);
        }
    }

    private void deleteKeysQuietly(UUID firmId, List<String> keys) {
        for (String key : keys) {
            try {
                documentStorage.delete(key);
            } catch (IOException e) {
                log.warn("Storage delete failed firmId={} key={}: {}", firmId, key, e.getMessage());
            }
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
}
