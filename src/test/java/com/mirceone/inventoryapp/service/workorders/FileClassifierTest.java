package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;
import com.mirceone.inventoryapp.model.WorkOrderFolderRuleEntity;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileClassifierTest {

    @Mock
    private WorkOrderFolderRuleRepository ruleRepository;
    @Mock
    private WorkOrderFolderRepository folderRepository;

    private FileClassifier classifier;

    private UUID workOrderId;
    private UUID rulesFolderId;
    private WorkOrderFolderEntity catchAll;

    @BeforeEach
    void setUp() {
        classifier = new FileClassifier(ruleRepository, folderRepository);
        workOrderId = UUID.randomUUID();
        rulesFolderId = UUID.randomUUID();
        catchAll = new WorkOrderFolderEntity(workOrderId, null, "Misc", true, 1);
    }

    @Test
    void resolvesFolderByExtensionRule() {
        when(ruleRepository.findByWorkOrderIdAndExtension(workOrderId, "pdf"))
                .thenReturn(Optional.of(new WorkOrderFolderRuleEntity(workOrderId, rulesFolderId, "pdf")));

        assertEquals(rulesFolderId, classifier.resolveFolderId(workOrderId, "pdf"));
    }

    @Test
    void fallsBackToCatchAllWhenNoRuleMatches() {
        when(ruleRepository.findByWorkOrderIdAndExtension(workOrderId, "xyz")).thenReturn(Optional.empty());
        when(folderRepository.findByWorkOrderIdAndCatchAllTrue(workOrderId)).thenReturn(Optional.of(catchAll));

        assertEquals(catchAll.getId(), classifier.resolveFolderId(workOrderId, "xyz"));
    }

    @Test
    void fallsBackToCatchAllForMissingExtension() {
        when(folderRepository.findByWorkOrderIdAndCatchAllTrue(workOrderId)).thenReturn(Optional.of(catchAll));

        assertEquals(catchAll.getId(), classifier.resolveFolderId(workOrderId, ""));
        assertEquals(catchAll.getId(), classifier.resolveFolderId(workOrderId, null));
    }

    @Test
    void failsLoudlyWhenCatchAllMissing() {
        when(folderRepository.findByWorkOrderIdAndCatchAllTrue(workOrderId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> classifier.resolveFolderId(workOrderId, ""));
    }
}
