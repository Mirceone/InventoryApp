package com.mirceone.inventoryapp.service.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mlx", matchIfMissing = true)
public class AiStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(AiStartupValidator.class);

    private final AppIntegrationProperties props;
    private final MlxModelEnsurer modelEnsurer;
    private final MlxServerManager serverManager;

    public AiStartupValidator(
            AppIntegrationProperties props,
            MlxModelEnsurer modelEnsurer,
            MlxServerManager serverManager
    ) {
        this.props = props;
        this.modelEnsurer = modelEnsurer;
        this.serverManager = serverManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapMlx() {
        AppIntegrationProperties.Ai ai = props.getAi();
        Path modelDir;

        try {
            modelDir = modelEnsurer.ensureModelPresent();
        } catch (Exception e) {
            log.warn("""
                    AI model is NOT ready (repo={}, cacheDir={}).
                    {}
                    Install deps: python3 -m venv .venv-mlx && .venv-mlx/bin/pip install huggingface_hub mlx-vlm
                    Or set APP_AI_PROVIDER=stub to disable MLX in this environment.
                    """,
                    ai.getHuggingfaceRepo(),
                    modelEnsurer.isModelComplete(modelEnsurer.modelCacheDir())
                            ? modelEnsurer.modelCacheDir().toAbsolutePath()
                            : MlxCommandResolver.resolveModelCacheDir(ai).toAbsolutePath(),
                    e.getMessage());
            return;
        }

        try {
            serverManager.startServerIfNeeded(modelDir);
        } catch (Exception e) {
            log.warn("""
                    MLX OpenAI server is NOT reachable at {} and auto-start failed: {}
                    Start manually, e.g.:
                      .venv-mlx/bin/python -m mlx_vlm.server --port {} --model {}
                    """,
                    ai.getBaseUrl(),
                    e.getMessage(),
                    MlxServerManager.resolvePort(ai),
                    modelDir.toAbsolutePath());
            return;
        }

        String modelsUrl = MlxServerManager.modelsUrl(ai);
        boolean ok = probeModels(modelsUrl, ai.getApiKey());
        if (!ok) {
            log.warn("""
                    AI MLX server is NOT reachable at {} (configured model: {}).
                    Verify:
                      curl -H "Authorization: Bearer {}" {}/models
                    Or set APP_AI_PROVIDER=stub to disable MLX in this environment.
                    """, ai.getBaseUrl(), ai.getModel(), ai.getApiKey(), modelsUrl);
        } else {
            log.info(
                    "AI MLX ready (baseUrl={}, model={}, weights={})",
                    ai.getBaseUrl(),
                    ai.getModel(),
                    modelDir.toAbsolutePath());
        }
    }

    static boolean probeModels(String modelsUrl, String apiKey) {
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(modelsUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .build();
            client.get().retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
