package com.mirceone.inventoryapp.service.workorders.classification;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.FileClassificationSource;
import com.mirceone.inventoryapp.model.FileClassificationStatus;
import com.mirceone.inventoryapp.service.workorders.FileClassifier;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class WorkOrderFileClassifier {

    private final AppIntegrationProperties props;
    private final FileClassifier extensionRuleClassifier;
    private final FileNameHeuristicClassifier heuristicClassifier;
    private final WorkOrderFolderResolver folderResolver;

    public WorkOrderFileClassifier(
            AppIntegrationProperties props,
            FileClassifier extensionRuleClassifier,
            FileNameHeuristicClassifier heuristicClassifier,
            WorkOrderFolderResolver folderResolver
    ) {
        this.props = props;
        this.extensionRuleClassifier = extensionRuleClassifier;
        this.heuristicClassifier = heuristicClassifier;
        this.folderResolver = folderResolver;
    }

    public UploadClassification classifyOnUpload(
            UUID workOrderId,
            String filename,
            String mimeType,
            String extension
    ) {
        Optional<UUID> byExtension = extensionRuleClassifier.resolveByExtension(workOrderId, extension);
        if (byExtension.isPresent()) {
            return new UploadClassification(
                    byExtension.get(),
                    FileClassificationStatus.CLASSIFIED,
                    FileClassificationSource.RULE
            );
        }

        Optional<String> hint = heuristicClassifier.hint(filename, mimeType);
        if (hint.isPresent()) {
            Optional<UUID> byHeuristic = folderResolver.resolveFolderId(workOrderId, hint.get());
            if (byHeuristic.isPresent()) {
                return new UploadClassification(
                        byHeuristic.get(),
                        FileClassificationStatus.CLASSIFIED,
                        FileClassificationSource.RULE
                );
            }
        }

        UUID catchAll = folderResolver.catchAllFolderId(workOrderId);
        if (props.getFeatures().isWorkOrderAiEnabled()) {
            return new UploadClassification(catchAll, FileClassificationStatus.PENDING, null);
        }
        // AI disabled and nothing matched: the deterministic engine routes the file to the
        // catch-all folder. This is reported as RULE (the contracted value for deterministic,
        // non-AI, non-manual placement, which includes the catch-all default) rather than a
        // separate source value.
        return new UploadClassification(
                catchAll,
                FileClassificationStatus.CLASSIFIED,
                FileClassificationSource.RULE
        );
    }
}
