package com.mirceone.inventoryapp.service.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MlxCommandResolverModelIdTest {

    @TempDir
    Path tempDir;

    @Test
    void usesLocalCacheDirWhenModelIsHfRepoId() {
        AppIntegrationProperties.Ai ai = new AppIntegrationProperties.Ai();
        ai.setModel("mlx-community/gemma-4-12B-it-qat-4bit");
        ai.setHuggingfaceRepo("mlx-community/gemma-4-12B-it-qat-4bit");
        ai.setModelCacheDir(tempDir.resolve("weights").toString());

        assertEquals(
                tempDir.resolve("weights").toAbsolutePath().toString(),
                MlxCommandResolver.resolveApiModelId(ai, Path.of(ai.getModelCacheDir()))
        );
    }

    @Test
    void usesAbsolutePathOverrideWhenConfigured() {
        AppIntegrationProperties.Ai ai = new AppIntegrationProperties.Ai();
        ai.setModel(tempDir.toAbsolutePath().toString());

        assertEquals(
                tempDir.toAbsolutePath().toString(),
                MlxCommandResolver.resolveApiModelId(ai, tempDir.resolve("ignored"))
        );
    }
}
