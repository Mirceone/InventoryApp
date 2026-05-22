package com.mirceone.inventoryapp.service.documents.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.OpsEventEntity;
import com.mirceone.inventoryapp.repository.OpsEventRepository;
import com.mirceone.inventoryapp.service.documents.DocumentFolderTaxonomy;
import com.mirceone.inventoryapp.service.documents.FolderTaxonomyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OllamaFolderClassifier {

    private static final Logger log = LoggerFactory.getLogger(OllamaFolderClassifier.class);

    private final OllamaChatClient ollamaChatClient;
    private final AppIntegrationProperties props;
    private final OpsEventRepository opsEventRepository;
    private final FolderTaxonomyResolver taxonomyResolver;

    public OllamaFolderClassifier(
            OllamaChatClient ollamaChatClient,
            AppIntegrationProperties props,
            OpsEventRepository opsEventRepository,
            FolderTaxonomyResolver taxonomyResolver
    ) {
        this.ollamaChatClient = ollamaChatClient;
        this.props = props;
        this.opsEventRepository = opsEventRepository;
        this.taxonomyResolver = taxonomyResolver;
    }

    /**
     * @param ruleSuggestion optional hint from rules (one of the five folders), not a parent path
     */
    public String classify(String originalFilename, String mimeType, String ruleSuggestion) {
        List<String> allowed = DocumentFolderTaxonomy.allowedForPrompt();
        String prompt = buildPrompt(originalFilename, mimeType, ruleSuggestion, allowed);
        Optional<String> ruleHint = ruleSuggestion == null || ruleSuggestion.isBlank()
                ? Optional.empty()
                : Optional.of(ruleSuggestion);
        try {
            String raw = ollamaChatClient.chatJson(prompt);
            logOps(prompt, raw);
            OllamaFolderSuggestion suggestion = ollamaChatClient.parseFolderSuggestion(raw);
            return taxonomyResolver.resolveFinal(suggestion.folder(), ruleHint);
        } catch (Exception e) {
            log.warn("Ollama folder classification failed for {}: {}", originalFilename, e.getMessage());
            return ruleHint.filter(DocumentFolderTaxonomy::isFinalPath)
                    .orElse(DocumentFolderTaxonomy.fallback());
        }
    }

    private String buildPrompt(String filename, String mimeType, String ruleSuggestion, List<String> allowed) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Classify this file into exactly one of the five dossier folders below.
                Reply with JSON only: {"folder":"<name>"}
                Copy the folder name literally from the list. Do not invent subfolders or new names.
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
        sb.append("If unsure, use ").append(DocumentFolderTaxonomy.fallback()).append(".\n");
        return sb.toString();
    }

    private void logOps(String prompt, String response) {
        if (!props.getFeatures().isOpsEnabled()) {
            return;
        }
        String promptExcerpt = excerpt(prompt, 500);
        String responseExcerpt = excerpt(response, 500);
        try {
            opsEventRepository.save(new OpsEventEntity(
                    promptExcerpt,
                    responseExcerpt,
                    props.getOllama().getModel()
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
