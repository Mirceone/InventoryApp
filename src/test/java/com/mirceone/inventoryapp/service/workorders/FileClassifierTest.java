package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.WorkOrderFolderRuleEntity;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileClassifierTest {

    @Mock
    private WorkOrderFolderRuleRepository ruleRepository;

    private FileClassifier classifier;

    private UUID workOrderId;
    private UUID rulesFolderId;

    @BeforeEach
    void setUp() {
        classifier = new FileClassifier(ruleRepository);
        workOrderId = UUID.randomUUID();
        rulesFolderId = UUID.randomUUID();
    }

    @Test
    void resolvesFolderByExtensionRule() {
        when(ruleRepository.findByWorkOrderIdAndExtension(workOrderId, "pdf"))
                .thenReturn(Optional.of(new WorkOrderFolderRuleEntity(workOrderId, rulesFolderId, "pdf")));

        assertEquals(Optional.of(rulesFolderId), classifier.resolveByExtension(workOrderId, "pdf"));
    }

    @Test
    void returnsEmptyWhenNoRuleMatches() {
        when(ruleRepository.findByWorkOrderIdAndExtension(workOrderId, "xyz")).thenReturn(Optional.empty());

        assertTrue(classifier.resolveByExtension(workOrderId, "xyz").isEmpty());
    }

    @Test
    void returnsEmptyForMissingExtension() {
        assertTrue(classifier.resolveByExtension(workOrderId, "").isEmpty());
        assertTrue(classifier.resolveByExtension(workOrderId, null).isEmpty());
    }
}
