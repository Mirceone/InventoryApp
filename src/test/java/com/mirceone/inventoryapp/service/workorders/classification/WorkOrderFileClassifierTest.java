package com.mirceone.inventoryapp.service.workorders.classification;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.FileClassificationSource;
import com.mirceone.inventoryapp.model.FileClassificationStatus;
import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;
import com.mirceone.inventoryapp.model.WorkOrderFolderRuleEntity;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRuleRepository;
import com.mirceone.inventoryapp.service.workorders.FileClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderFileClassifierTest {

    @Mock
    private WorkOrderFolderRuleRepository ruleRepository;
    @Mock
    private WorkOrderFolderRepository folderRepository;

    private AppIntegrationProperties props;
    private WorkOrderFileClassifier classifier;
    private UUID workOrderId;
    private UUID rulesFolderId;
    private WorkOrderFolderEntity catchAll;

    @BeforeEach
    void setUp() {
        props = new AppIntegrationProperties();
        FileClassifier extensionClassifier = new FileClassifier(ruleRepository);
        classifier = new WorkOrderFileClassifier(
                props,
                extensionClassifier,
                new FileNameHeuristicClassifier(),
                new WorkOrderFolderResolver(folderRepository, new FileNameHeuristicClassifier())
        );
        workOrderId = UUID.randomUUID();
        rulesFolderId = UUID.randomUUID();
        catchAll = new WorkOrderFolderEntity(workOrderId, null, "Misc", true, 1);
    }

    @Test
    void extensionRuleSkipsAi() {
        when(ruleRepository.findByWorkOrderIdAndExtension(workOrderId, "pdf"))
                .thenReturn(Optional.of(new WorkOrderFolderRuleEntity(workOrderId, rulesFolderId, "pdf")));

        UploadClassification result = classifier.classifyOnUpload(
                workOrderId, "doc.pdf", "application/pdf", "pdf");

        assertEquals(rulesFolderId, result.folderId());
        assertEquals(FileClassificationStatus.CLASSIFIED, result.status());
        assertEquals(FileClassificationSource.RULE, result.source());
    }

    @Test
    void noRuleWithAiEnabledIsPending() {
        props.getFeatures().setWorkOrderAiEnabled(true);
        when(folderRepository.findByWorkOrderIdAndCatchAllTrue(workOrderId)).thenReturn(Optional.of(catchAll));

        UploadClassification result = classifier.classifyOnUpload(
                workOrderId, "notes.xyz", "application/octet-stream", "xyz");

        assertEquals(catchAll.getId(), result.folderId());
        assertEquals(FileClassificationStatus.PENDING, result.status());
    }

    @Test
    void noRuleWithAiDisabledIsClassifiedToCatchAll() {
        props.getFeatures().setWorkOrderAiEnabled(false);
        when(folderRepository.findByWorkOrderIdAndCatchAllTrue(workOrderId)).thenReturn(Optional.of(catchAll));

        UploadClassification result = classifier.classifyOnUpload(
                workOrderId, "notes.xyz", "application/octet-stream", "xyz");

        assertEquals(catchAll.getId(), result.folderId());
        assertEquals(FileClassificationStatus.CLASSIFIED, result.status());
        assertEquals(FileClassificationSource.RULE, result.source());
    }
}
