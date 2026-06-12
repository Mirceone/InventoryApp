package com.mirceone.inventoryapp.service.workorders.classification;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.OpsEventEntity;
import com.mirceone.inventoryapp.repository.OpsEventRepository;
import com.mirceone.inventoryapp.service.ai.AiModelIdResolver;
import com.mirceone.inventoryapp.service.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MlxFolderClassifier {

    private static final Logger log = LoggerFactory.getLogger(MlxFolderClassifier.class);

    private final AiService aiService;
    private final AppIntegrationProperties props;
    private final OpsEventRepository opsEventRepository;
    private final WorkOrderFolderResolver folderResolver;
    private final ObjectProvider<AiModelIdResolver> modelIdResolver;

    public MlxFolderClassifier(
            AiService aiService,
            AppIntegrationProperties props,
            OpsEventRepository opsEventRepository,
            WorkOrderFolderResolver folderResolver,
            ObjectProvider<AiModelIdResolver> modelIdResolver
    ) {
        this.aiService = aiService;
        this.props = props;
        this.opsEventRepository = opsEventRepository;
        this.folderResolver = folderResolver;
        this.modelIdResolver = modelIdResolver;
    }

    public UUID classify(
            UUID workOrderId,
            String originalFilename,
            String mimeType,
            String ruleSuggestion
    ) {
        WorkOrderFolderResolver.FolderTree tree = folderResolver.loadTree(workOrderId);
        List<String> allowed = folderResolver.allowedPathsForPrompt(tree);
        String catchAllPath = folderResolver.catchAllPath(tree);
        String prompt = buildPrompt(originalFilename, mimeType, ruleSuggestion, allowed, catchAllPath);
        Optional<String> ruleHint = ruleSuggestion == null || ruleSuggestion.isBlank()
                ? Optional.empty()
                : Optional.of(ruleSuggestion);
        try {
            String raw = aiService.chatJson(prompt);
            logOps(prompt, raw);
            AiFolderSuggestion suggestion = AiFolderSuggestion.parse(raw);
            return folderResolver.resolveFinal(tree, suggestion.folder(), ruleHint);
        } catch (Exception e) {
            log.warn("MLX folder classification failed for {}: {}", originalFilename, e.getMessage());
            if (ruleHint.isPresent()) {
                return folderResolver.resolveFolderId(tree, ruleHint.get())
                        .orElseGet(() -> folderResolver.catchAllFolderId(tree));
            }
            return folderResolver.catchAllFolderId(tree);
        }
    }

    private String buildPrompt(
            String filename,
            String mimeType,
            String ruleSuggestion,
            List<String> allowed,
            String catchAllPath
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Classify this file into exactly one folder from the list below.
                Reply with JSON only: {"folder":"<path>"}
                Copy the folder path literally. Do not invent new folders.
                """);
        sb.append("Allowed folders:\n");
        for (String path : allowed) {
            sb.append("- ").append(path).append('\n');
        }
        sb.append("filename: ").append(filename).append('\n');
        sb.append("mimeType: ").append(mimeType != null ? mimeType : "unknown").append('\n');
        if (ruleSuggestion != null && !ruleSuggestion.isBlank()) {
            sb.append("ruleSuggestion: ").append(ruleSuggestion).append('\n');
        }
        sb.append("If unsure, use ").append(catchAllPath).append(".\n");
        return sb.toString();
    }

    private void logOps(String prompt, String response) {
        if (!props.getFeatures().isOpsEnabled()) {
            return;
        }
        AiModelIdResolver resolver = modelIdResolver.getIfAvailable();
        String model = resolver != null ? resolver.resolvedModelId() : props.getAi().getModel();
        try {
            opsEventRepository.save(new OpsEventEntity(
                    excerpt(prompt, 500),
                    excerpt(response, 500),
                    model
            ));
        } catch (Exception e) {
            log.debug("Failed to persist ops event: {}", e.getMessage());
        }
    }

    private static String excerpt(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
