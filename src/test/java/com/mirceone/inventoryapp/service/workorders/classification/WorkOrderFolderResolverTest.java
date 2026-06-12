package com.mirceone.inventoryapp.service.workorders.classification;

import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderFolderResolverTest {

    @Mock
    private WorkOrderFolderRepository folderRepository;

    private WorkOrderFolderResolver resolver;
    private UUID workOrderId;
    private WorkOrderFolderEntity documents;
    private WorkOrderFolderEntity catchAll;

    @BeforeEach
    void setUp() {
        resolver = new WorkOrderFolderResolver(folderRepository, new FileNameHeuristicClassifier());
        workOrderId = UUID.randomUUID();
        documents = new WorkOrderFolderEntity(workOrderId, null, "Documents", false, 0);
        catchAll = new WorkOrderFolderEntity(workOrderId, null, "Misc", true, 1);
        when(folderRepository.findAllByWorkOrderIdOrderBySortOrderAscNameAsc(workOrderId))
                .thenReturn(List.of(documents, catchAll));
    }

    @Test
    void resolvesFolderByName() {
        assertEquals(documents.getId(), resolver.resolveFolderId(workOrderId, "Documents").orElseThrow());
    }

    @Test
    void resolvesSynonymToExistingFolder() {
        assertEquals(documents.getId(), resolver.resolveFolderId(workOrderId, "documente").orElseThrow());
    }

    @Test
    void catchAllFallbackWhenUnknown() {
        WorkOrderFolderResolver.FolderTree tree = resolver.loadTree(workOrderId);
        assertEquals(catchAll.getId(), resolver.resolveFinal(tree, "Unknown", java.util.Optional.empty()));
    }
}
