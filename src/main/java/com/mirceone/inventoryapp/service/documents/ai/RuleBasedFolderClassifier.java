package com.mirceone.inventoryapp.service.documents.ai;

import com.mirceone.inventoryapp.service.documents.DocumentFolderTaxonomy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RuleBasedFolderClassifier {

    /**
     * Returns one of the five canonical folders, or empty when AI should classify.
     */
    public Optional<String> classify(String originalFilename, String mimeType) {
        return DocumentFolderTaxonomy.ruleFolderHint(originalFilename, mimeType);
    }
}
