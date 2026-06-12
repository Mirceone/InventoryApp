package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.WorkOrderFolderRuleEntity;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRuleRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Deterministic, synchronous classification: the work order's extension rules decide the
 * target folder; anything unmatched lands in the catch-all folder.
 */
@Component
public class FileClassifier {

    private final WorkOrderFolderRuleRepository ruleRepository;
    private final WorkOrderFolderRepository folderRepository;

    public FileClassifier(
            WorkOrderFolderRuleRepository ruleRepository,
            WorkOrderFolderRepository folderRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.folderRepository = folderRepository;
    }

    /**
     * @param extension normalized extension (lowercase, no dot); may be empty
     */
    public UUID resolveFolderId(UUID workOrderId, String extension) {
        if (extension != null && !extension.isEmpty()) {
            UUID byRule = ruleRepository.findByWorkOrderIdAndExtension(workOrderId, extension)
                    .map(WorkOrderFolderRuleEntity::getFolderId)
                    .orElse(null);
            if (byRule != null) {
                return byRule;
            }
        }
        return folderRepository.findByWorkOrderIdAndCatchAllTrue(workOrderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Work order " + workOrderId + " has no catch-all folder"))
                .getId();
    }
}
