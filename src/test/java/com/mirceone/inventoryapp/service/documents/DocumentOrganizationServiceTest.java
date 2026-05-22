package com.mirceone.inventoryapp.service.documents;

import com.mirceone.inventoryapp.model.DocumentProcessingStatus;
import com.mirceone.inventoryapp.model.FirmDocumentEntity;
import com.mirceone.inventoryapp.model.OrganizationSource;
import com.mirceone.inventoryapp.repository.FirmDocumentRepository;
import com.mirceone.inventoryapp.service.documents.ai.DocumentFolderClassifier;
import com.mirceone.inventoryapp.service.documents.ai.FolderClassificationResult;
import com.mirceone.inventoryapp.service.documents.storage.DocumentStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentOrganizationServiceTest {

    @Mock
    private FirmDocumentRepository firmDocumentRepository;
    @Mock
    private DocumentStorage documentStorage;
    @Mock
    private DocumentFolderClassifier documentFolderClassifier;

    @InjectMocks
    private DocumentOrganizationService organizationService;

    @Test
    void organizesPendingDocumentIntoClassifiedFolder() throws Exception {
        UUID firmId = UUID.randomUUID();
        UUID dossierId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        String pendingKey = DocumentStorageKeys.pendingKey(firmId, dossierId, docId, ".png");
        FirmDocumentEntity doc = new FirmDocumentEntity(
                docId, firmId, dossierId, UUID.randomUUID(), "x.png", "image/png", 10, pendingKey, "abc"
        );

        when(firmDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(documentStorage.asResource(pendingKey)).thenReturn(new ByteArrayResource(new byte[0]));
        when(documentFolderClassifier.classify("x.png", "image/png"))
                .thenReturn(new FolderClassificationResult("Poze", OrganizationSource.RULE));

        organizationService.organizeDocument(docId);

        ArgumentCaptor<FirmDocumentEntity> saved = ArgumentCaptor.forClass(FirmDocumentEntity.class);
        verify(firmDocumentRepository).save(saved.capture());
        assertEquals(DocumentProcessingStatus.CLASSIFIED, saved.getValue().getProcessingStatus());
        assertEquals("Poze", saved.getValue().getFolderPath());
        verify(documentStorage).move(eq(pendingKey), any());
    }
}
