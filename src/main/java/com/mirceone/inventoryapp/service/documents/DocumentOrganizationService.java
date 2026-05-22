package com.mirceone.inventoryapp.service.documents;

import com.mirceone.inventoryapp.model.DocumentProcessingStatus;
import com.mirceone.inventoryapp.model.FirmDocumentEntity;
import com.mirceone.inventoryapp.model.OrganizationSource;
import com.mirceone.inventoryapp.repository.FirmDocumentRepository;
import com.mirceone.inventoryapp.service.documents.ai.DocumentFolderClassifier;
import com.mirceone.inventoryapp.service.documents.ai.FolderClassificationResult;
import com.mirceone.inventoryapp.service.documents.storage.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Service
public class DocumentOrganizationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentOrganizationService.class);

    private final FirmDocumentRepository firmDocumentRepository;
    private final DocumentStorage documentStorage;
    private final DocumentFolderClassifier documentFolderClassifier;

    public DocumentOrganizationService(
            FirmDocumentRepository firmDocumentRepository,
            DocumentStorage documentStorage,
            DocumentFolderClassifier documentFolderClassifier
    ) {
        this.firmDocumentRepository = firmDocumentRepository;
        this.documentStorage = documentStorage;
        this.documentFolderClassifier = documentFolderClassifier;
    }

    @Transactional
    public void organizeDocument(UUID documentId) {
        FirmDocumentEntity doc = firmDocumentRepository.findById(documentId)
                .orElse(null);
        if (doc == null || doc.getProcessingStatus() != DocumentProcessingStatus.PENDING) {
            return;
        }

        try {
            documentStorage.asResource(doc.getStorageKey());
        } catch (IOException e) {
            markFailed(doc, "File missing on disk");
            return;
        }

        FolderClassificationResult classification = documentFolderClassifier.classify(
                doc.getOriginalFilename(),
                doc.getMimeType()
        );
        String folderPath = classification.folderPath();
        String suffix = DocumentStorageKeys.fileSuffixFromStorageKey(doc.getStorageKey());
        String targetKey = DocumentStorageKeys.classifiedKey(
                doc.getFirmId(), doc.getDossierId(), folderPath, doc.getId(), suffix);

        try {
            documentStorage.move(doc.getStorageKey(), targetKey);
        } catch (IOException e) {
            markFailed(doc, "Failed to move file: " + e.getMessage());
            return;
        }

        doc.setStorageKey(targetKey);
        doc.setFolderPath(folderPath);
        doc.setProcessingStatus(DocumentProcessingStatus.CLASSIFIED);
        doc.setOrganizationSource(classification.source());
        doc.setOrganizationError(null);
        firmDocumentRepository.save(doc);
        log.debug("Classified document {} into folder {}", doc.getId(), folderPath);
    }

    private void markFailed(FirmDocumentEntity doc, String error) {
        doc.setProcessingStatus(DocumentProcessingStatus.FAILED);
        doc.setOrganizationError(error != null && error.length() > 500 ? error.substring(0, 500) : error);
        firmDocumentRepository.save(doc);
        log.warn("Document organization failed id={}: {}", doc.getId(), error);
    }
}
