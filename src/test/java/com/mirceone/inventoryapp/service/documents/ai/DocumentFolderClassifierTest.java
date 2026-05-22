package com.mirceone.inventoryapp.service.documents.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.OrganizationSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentFolderClassifierTest {

    @Mock
    private RuleBasedFolderClassifier ruleBasedFolderClassifier;
    @Mock
    private OllamaFolderClassifier ollamaFolderClassifier;

    private AppIntegrationProperties props;
    private DocumentFolderClassifier classifier;

    @BeforeEach
    void setUp() {
        props = new AppIntegrationProperties();
        props.getFeatures().setDossierAiEnabled(false);
        classifier = new DocumentFolderClassifier(ruleBasedFolderClassifier, ollamaFolderClassifier, props);
    }

    @Test
    void usesRuleFolderWhenPresent() {
        when(ruleBasedFolderClassifier.classify("a.png", "image/png")).thenReturn(Optional.of("Poze"));

        FolderClassificationResult result = classifier.classify("a.png", "image/png");

        assertEquals("Poze", result.folderPath());
        assertEquals(OrganizationSource.RULE, result.source());
    }

    @Test
    void usesMiscWhenNoRuleAndAiDisabled() {
        when(ruleBasedFolderClassifier.classify(any(), any())).thenReturn(Optional.empty());

        FolderClassificationResult result = classifier.classify("data.zip", "application/zip");

        assertEquals("Misc", result.folderPath());
        assertEquals(OrganizationSource.RULE, result.source());
    }

    @Test
    void usesOllamaWhenNoRuleAndAiEnabled() {
        props.getFeatures().setDossierAiEnabled(true);
        when(ruleBasedFolderClassifier.classify(any(), any())).thenReturn(Optional.empty());
        when(ollamaFolderClassifier.classify(any(), any(), isNull())).thenReturn("Documente");

        FolderClassificationResult result = classifier.classify("data.zip", "application/zip");

        assertEquals("Documente", result.folderPath());
        assertEquals(OrganizationSource.OLLAMA, result.source());
    }
}
