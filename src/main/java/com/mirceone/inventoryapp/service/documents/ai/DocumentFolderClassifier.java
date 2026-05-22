package com.mirceone.inventoryapp.service.documents.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.OrganizationSource;
import com.mirceone.inventoryapp.service.documents.DocumentFolderTaxonomy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DocumentFolderClassifier {

    private final RuleBasedFolderClassifier ruleBasedFolderClassifier;
    private final OllamaFolderClassifier ollamaFolderClassifier;
    private final AppIntegrationProperties props;

    public DocumentFolderClassifier(
            RuleBasedFolderClassifier ruleBasedFolderClassifier,
            OllamaFolderClassifier ollamaFolderClassifier,
            AppIntegrationProperties props
    ) {
        this.ruleBasedFolderClassifier = ruleBasedFolderClassifier;
        this.ollamaFolderClassifier = ollamaFolderClassifier;
        this.props = props;
    }

    public FolderClassificationResult classify(String originalFilename, String mimeType) {
        Optional<String> ruleFolder = ruleBasedFolderClassifier.classify(originalFilename, mimeType);

        if (ruleFolder.isPresent()) {
            return new FolderClassificationResult(ruleFolder.get(), OrganizationSource.RULE);
        }

        if (props.getFeatures().isDossierAiEnabled()) {
            String fromAi = ollamaFolderClassifier.classify(originalFilename, mimeType, null);
            return new FolderClassificationResult(fromAi, OrganizationSource.OLLAMA);
        }

        return new FolderClassificationResult(DocumentFolderTaxonomy.fallback(), OrganizationSource.RULE);
    }
}
