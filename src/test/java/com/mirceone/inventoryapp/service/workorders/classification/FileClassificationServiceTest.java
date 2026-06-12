package com.mirceone.inventoryapp.service.workorders.classification;

import com.mirceone.inventoryapp.model.FileClassificationSource;
import com.mirceone.inventoryapp.model.FileClassificationStatus;
import com.mirceone.inventoryapp.model.WorkOrderFileEntity;
import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import com.mirceone.inventoryapp.service.workorders.DisplayNameDeduplicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileClassificationServiceTest {

    @Mock
    private WorkOrderFileRepository fileRepository;
    @Mock
    private MlxFolderClassifier mlxFolderClassifier;
    @Mock
    private DisplayNameDeduplicator displayNameDeduplicator;

    private FileClassificationService service;
    private UUID workOrderId;
    private UUID targetFolderId;

    @BeforeEach
    void setUp() {
        service = new FileClassificationService(
                fileRepository,
                mlxFolderClassifier,
                new FileNameHeuristicClassifier(),
                displayNameDeduplicator
        );
        workOrderId = UUID.randomUUID();
        targetFolderId = UUID.randomUUID();
    }

    @Test
    void processPendingBatchUpdatesFolderAndStatus() {
        WorkOrderFileEntity pending = new WorkOrderFileEntity(
                UUID.randomUUID(), UUID.randomUUID(), workOrderId, UUID.randomUUID(), UUID.randomUUID(),
                "spec.pdf", "pdf", "application/pdf", 10, null, "key.pdf",
                FileClassificationStatus.PENDING, null);
        when(fileRepository.lockNextPendingBatch(5)).thenReturn(List.of(pending));
        when(mlxFolderClassifier.classify(eq(workOrderId), eq("spec.pdf"), eq("application/pdf"), any()))
                .thenReturn(targetFolderId);
        when(displayNameDeduplicator.uniqueName(targetFolderId, "spec.pdf")).thenReturn("spec.pdf");
        when(fileRepository.save(any(WorkOrderFileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        int processed = service.processPendingBatch(5);

        assertEquals(1, processed);
        assertEquals(targetFolderId, pending.getFolderId());
        assertEquals(FileClassificationStatus.CLASSIFIED, pending.getClassificationStatus());
        assertEquals(FileClassificationSource.AI, pending.getClassificationSource());
        verify(fileRepository).save(pending);
    }

    @Test
    void processFileSkipsWhenNotPending() {
        when(fileRepository.lockPendingById(any())).thenReturn(Optional.empty());

        service.processFile(UUID.randomUUID());

        verify(fileRepository).lockPendingById(any());
    }
}
