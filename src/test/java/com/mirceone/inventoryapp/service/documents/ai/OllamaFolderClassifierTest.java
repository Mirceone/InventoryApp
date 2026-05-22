package com.mirceone.inventoryapp.service.documents.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.repository.OpsEventRepository;
import com.mirceone.inventoryapp.service.documents.FolderTaxonomyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OllamaFolderClassifierTest {

    @Mock
    private OllamaChatClient ollamaChatClient;
    @Mock
    private OpsEventRepository opsEventRepository;

    private OllamaFolderClassifier classifier;

    @BeforeEach
    void setUp() {
        AppIntegrationProperties props = new AppIntegrationProperties();
        props.getFeatures().setOpsEnabled(false);
        classifier = new OllamaFolderClassifier(
                ollamaChatClient,
                props,
                opsEventRepository,
                new FolderTaxonomyResolver()
        );
    }

    @Test
    void mapsInvalidAiFolderToMisc() throws Exception {
        when(ollamaChatClient.chatJson(anyString())).thenReturn("{\"folder\":\"Renderi\"}");
        when(ollamaChatClient.parseFolderSuggestion(anyString()))
                .thenReturn(new OllamaFolderSuggestion("Renderi"));

        String path = classifier.classify("x.png", "image/png", null);

        assertEquals("Renders", path);
    }

    @Test
    void returnsFinalPathWhenRuleAlreadyFinal() {
        String path = classifier.classify("model.skp", "application/octet-stream", "Documente");
        assertEquals("Documente", path);
    }
}
