package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.WorkOrderFolderRuleEntity;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRuleRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Deterministic, synchronous extension-rule lookup: maps a file extension to the work order
 * folder configured for it, if any. Catch-all fallback and heuristic/AI classification are
 * handled by {@link com.mirceone.inventoryapp.service.workorders.classification.WorkOrderFileClassifier}.
 */
@Component
public class FileClassifier {

    private final WorkOrderFolderRuleRepository ruleRepository;

    public FileClassifier(WorkOrderFolderRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /**
     * @param extension normalized extension (lowercase, no dot); may be empty
     */
    public Optional<UUID> resolveByExtension(UUID workOrderId, String extension) {
        if (extension == null || extension.isEmpty()) {
            return Optional.empty();
        }
        return ruleRepository.findByWorkOrderIdAndExtension(workOrderId, extension)
                .map(WorkOrderFolderRuleEntity::getFolderId);
    }
}
