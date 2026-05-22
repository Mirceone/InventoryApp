package com.mirceone.inventoryapp.service.documents;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.DocumentProcessingStatus;
import com.mirceone.inventoryapp.model.FirmDocumentEntity;
import com.mirceone.inventoryapp.repository.FirmDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DocumentOrganizationWorker {

    private static final Logger log = LoggerFactory.getLogger(DocumentOrganizationWorker.class);

    private final AppIntegrationProperties props;
    private final FirmDocumentRepository firmDocumentRepository;
    private final DocumentOrganizationService documentOrganizationService;

    public DocumentOrganizationWorker(
            AppIntegrationProperties props,
            FirmDocumentRepository firmDocumentRepository,
            DocumentOrganizationService documentOrganizationService
    ) {
        this.props = props;
        this.firmDocumentRepository = firmDocumentRepository;
        this.documentOrganizationService = documentOrganizationService;
    }

    @Scheduled(fixedDelayString = "${app.documents.organization-poll-interval:2s}")
    public void processPendingDocuments() {
        if (!props.getFeatures().isDossierEnabled()) {
            return;
        }
        int batch = Math.max(1, props.getDocuments().getOrganizationBatchSize());
        List<FirmDocumentEntity> pending = firmDocumentRepository.findByProcessingStatusOrderByCreatedAtAsc(
                DocumentProcessingStatus.PENDING,
                PageRequest.of(0, batch)
        );
        for (FirmDocumentEntity doc : pending) {
            try {
                documentOrganizationService.organizeDocument(doc.getId());
            } catch (Exception e) {
                log.warn("Unexpected error organizing document {}: {}", doc.getId(), e.getMessage());
            }
        }
    }
}
