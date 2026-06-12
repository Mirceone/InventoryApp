package com.mirceone.inventoryapp.service.workorders.classification;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;
import com.mirceone.inventoryapp.repository.OpsEventRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRepository;
import com.mirceone.inventoryapp.service.ai.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MlxFolderClassifierTest {

    @Mock
    private AiService aiService;
    @Mock
    private OpsEventRepository opsEventRepository;
    @Mock
    private WorkOrderFolderRepository folderRepository;
    @Mock
    private ObjectProvider<com.mirceone.inventoryapp.service.ai.AiModelIdResolver> modelIdResolver;

    private MlxFolderClassifier classifier;
    private UUID workOrderId;
    private WorkOrderFolderEntity documents;
    private WorkOrderFolderEntity catchAll;

    @BeforeEach
    void setUp() {
        AppIntegrationProperties props = new AppIntegrationProperties();
        props.getFeatures().setOpsEnabled(false);
        WorkOrderFolderResolver folderResolver = new WorkOrderFolderResolver(
                folderRepository, new FileNameHeuristicClassifier());
        classifier = new MlxFolderClassifier(aiService, props, opsEventRepository, folderResolver, modelIdResolver);

        workOrderId = UUID.randomUUID();
        documents = new WorkOrderFolderEntity(workOrderId, null, "Documents", false, 0);
        catchAll = new WorkOrderFolderEntity(workOrderId, null, "Misc", true, 1);
        when(folderRepository.findAllByWorkOrderIdOrderBySortOrderAscNameAsc(workOrderId))
                .thenReturn(List.of(documents, catchAll));
    }

    @Test
    void promptListsFoldersAndParsesJsonResponse() {
        when(aiService.chatJson(anyString())).thenReturn("{\"folder\":\"Documents\"}");

        UUID folderId = classifier.classify(workOrderId, "notes.pdf", "application/pdf", null);

        assertEquals(documents.getId(), folderId);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).chatJson(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Allowed folders:"));
        assertTrue(prompt.contains("- Documents"));
        assertTrue(prompt.contains("- Misc"));
        assertTrue(prompt.contains("filename: notes.pdf"));
    }
}
