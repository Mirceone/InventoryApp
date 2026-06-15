package com.mirceone.inventoryapp.service.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MlxModelEnsurerTest {

    @TempDir
    Path tempDir;

    @Test
    void isModelCompleteRequiresConfigAndSafetensors() throws Exception {
        AppIntegrationProperties props = new AppIntegrationProperties();
        MlxCommandResolver resolver = new MlxCommandResolver(props);
        resolver.resolveAtStartup();
        MlxModelEnsurer ensurer = new MlxModelEnsurer(props, resolver);

        assertFalse(ensurer.isModelComplete(tempDir));

        Files.writeString(tempDir.resolve("config.json"), "{}");
        assertFalse(ensurer.isModelComplete(tempDir));

        Files.writeString(tempDir.resolve("model.safetensors"), "x");
        assertTrue(ensurer.isModelComplete(tempDir));
    }

    @Test
    void resolveModelCacheDirUsesRepoSlugByDefault() {
        AppIntegrationProperties.Ai ai = new AppIntegrationProperties.Ai();
        ai.setHuggingfaceRepo("mlx-community/gemma-4-12B-it-qat-4bit");

        Path dir = MlxCommandResolver.resolveModelCacheDir(ai);

        assertTrue(dir.toString().contains(".mlx-models"));
        assertTrue(dir.toString().contains("mlx-community-gemma-4-12B-it-qat-4bit"));
    }
}
