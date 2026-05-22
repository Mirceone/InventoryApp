package com.mirceone.inventoryapp.service.documents;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.FirmDossierEntity;
import com.mirceone.inventoryapp.model.FirmDocumentEntity;
import com.mirceone.inventoryapp.repository.FirmDocumentRepository;
import com.mirceone.inventoryapp.repository.FirmDossierRepository;
import com.mirceone.inventoryapp.service.documents.storage.DocumentStorage;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
public class DossierService {

    private static final Logger log = LoggerFactory.getLogger(DossierService.class);

    private final AppIntegrationProperties props;
    private final FirmAccessService firmAccessService;
    private final FirmDossierRepository firmDossierRepository;
    private final FirmDocumentRepository firmDocumentRepository;
    private final DocumentStorage documentStorage;

    public DossierService(
            AppIntegrationProperties props,
            FirmAccessService firmAccessService,
            FirmDossierRepository firmDossierRepository,
            FirmDocumentRepository firmDocumentRepository,
            DocumentStorage documentStorage
    ) {
        this.props = props;
        this.firmAccessService = firmAccessService;
        this.firmDossierRepository = firmDossierRepository;
        this.firmDocumentRepository = firmDocumentRepository;
        this.documentStorage = documentStorage;
    }

    @Transactional
    public DossierSummary createDossier(UUID userId, UUID firmId, String rawName) {
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        String name = DossierNameSanitizer.sanitize(rawName);
        if (firmDossierRepository.existsByFirmIdAndNameIgnoreCase(firmId, name)) {
            throw new ResponseStatusException(CONFLICT, "A dossier with this name already exists");
        }
        try {
            FirmDossierEntity saved = firmDossierRepository.save(new FirmDossierEntity(firmId, name, userId));
            return toSummary(saved, 0);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "A dossier with this name already exists", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<DossierSummary> listDossiers(UUID userId, UUID firmId) {
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        return firmDossierRepository.findAllByFirmIdOrderByCreatedAtDesc(firmId).stream()
                .map(d -> toSummary(d, firmDocumentRepository.countByDossierId(d.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DossierSummary getDossier(UUID userId, UUID firmId, UUID dossierId) {
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        FirmDossierEntity dossier = requireDossier(firmId, dossierId);
        return toSummary(dossier, firmDocumentRepository.countByDossierId(dossierId));
    }

    @Transactional
    public DossierSummary renameDossier(UUID userId, UUID firmId, UUID dossierId, String rawName) {
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        FirmDossierEntity dossier = requireDossier(firmId, dossierId);
        String name = DossierNameSanitizer.sanitize(rawName);
        if (firmDossierRepository.existsByFirmIdAndNameIgnoreCaseAndIdNot(firmId, name, dossierId)) {
            throw new ResponseStatusException(CONFLICT, "A dossier with this name already exists");
        }
        dossier.setName(name);
        try {
            FirmDossierEntity saved = firmDossierRepository.save(dossier);
            return toSummary(saved, firmDocumentRepository.countByDossierId(dossierId));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "A dossier with this name already exists", ex);
        }
    }

    @Transactional
    public void deleteDossier(UUID userId, UUID firmId, UUID dossierId) {
        assertDossierEnabled();
        firmAccessService.requirePermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        firmAccessService.requireFirmOperationalForUser(firmId, userId);
        requireDossier(firmId, dossierId);
        List<String> storageKeys = firmDocumentRepository.findAllByDossierId(dossierId).stream()
                .map(FirmDocumentEntity::getStorageKey)
                .toList();
        firmDossierRepository.deleteById(dossierId);
        scheduleStorageDeletes(firmId, dossierId, storageKeys);
    }

    public FirmDossierEntity requireDossier(UUID firmId, UUID dossierId) {
        return firmDossierRepository.findByIdAndFirmId(dossierId, firmId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Dossier not found"));
    }

    private void scheduleStorageDeletes(UUID firmId, UUID dossierId, List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        List<String> copy = new ArrayList<>(keys);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteKeysQuietly(firmId, dossierId, copy);
                }
            });
        } else {
            deleteKeysQuietly(firmId, dossierId, copy);
        }
    }

    private void deleteKeysQuietly(UUID firmId, UUID dossierId, List<String> keys) {
        for (String key : keys) {
            try {
                documentStorage.delete(key);
            } catch (IOException e) {
                log.warn("Storage delete failed firmId={} dossierId={} key={}: {}", firmId, dossierId, key, e.getMessage());
            }
        }
    }

    private DossierSummary toSummary(FirmDossierEntity entity, long documentCount) {
        return new DossierSummary(
                entity.getId(),
                entity.getFirmId(),
                entity.getName(),
                entity.getCreatedByUserId(),
                entity.getCreatedAt(),
                documentCount
        );
    }

    private void assertDossierEnabled() {
        if (!props.getFeatures().isDossierEnabled()) {
            throw new ResponseStatusException(FORBIDDEN, "Electronic folder feature is disabled");
        }
    }
}
