package com.mirceone.inventoryapp.service.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mlx", matchIfMissing = true)
public class MlxModelEnsurer {

    private static final Logger log = LoggerFactory.getLogger(MlxModelEnsurer.class);

    private final AppIntegrationProperties props;
    private final MlxCommandResolver commandResolver;

    public MlxModelEnsurer(AppIntegrationProperties props, MlxCommandResolver commandResolver) {
        this.props = props;
        this.commandResolver = commandResolver;
    }

    /**
     * Ensures weights are on disk. Downloads from Hugging Face when missing and enabled in config.
     *
     * @return resolved local model directory
     */
    public Path ensureModelPresent() throws IOException {
        AppIntegrationProperties.Ai ai = props.getAi();
        Path cacheDir = commandResolver.modelCacheDir();

        if (isModelComplete(cacheDir)) {
            log.info("AI model already present at {}", cacheDir.toAbsolutePath());
            return cacheDir;
        }

        if (!ai.isAutoDownloadModel()) {
            throw new IOException(
                    "AI model not found at " + cacheDir.toAbsolutePath()
                            + " and app.ai.auto-download-model is disabled");
        }

        if (!commandResolver.isEnsureScriptAvailable()) {
            throw new IOException(
                    "Cannot download AI model: missing python or scripts/ensure_mlx_model.py. "
                            + "Create .venv-mlx and pip install huggingface_hub");
        }

        if (!MlxCommandResolver.probePythonModule(commandResolver.pythonPrefix(), "huggingface_hub")) {
            throw new IOException(
                    "Python at " + commandResolver.pythonPrefix().getFirst()
                            + " is missing huggingface_hub. Run: pip install huggingface_hub");
        }

        log.info(
                "AI model not found locally; downloading {} to {} (this may take a while)",
                ai.getHuggingfaceRepo(),
                cacheDir.toAbsolutePath());

        runEnsureScript(ai.getHuggingfaceRepo(), cacheDir, false);

        if (!isModelComplete(cacheDir)) {
            throw new IOException("Model download finished but directory is still incomplete: " + cacheDir);
        }

        log.info("AI model download complete at {}", cacheDir.toAbsolutePath());
        return cacheDir;
    }

    public Path modelCacheDir() {
        return commandResolver.modelCacheDir();
    }

    public boolean isModelComplete(Path modelDir) {
        if (!Files.isDirectory(modelDir)) {
            return false;
        }
        if (!Files.isRegularFile(modelDir.resolve("config.json"))) {
            return false;
        }
        try (var stream = Files.list(modelDir)) {
            return stream.anyMatch(path -> path.getFileName().toString().endsWith(".safetensors"));
        } catch (IOException e) {
            return false;
        }
    }

    void runEnsureScript(String repo, Path localDir, boolean checkOnly) throws IOException {
        Duration timeout = props.getAi().getModelDownloadTimeout();
        List<String> command = new ArrayList<>(commandResolver.pythonPrefix());
        command.add(commandResolver.ensureModelScript().toAbsolutePath().toString());
        command.add("--repo");
        command.add(repo);
        command.add("--local-dir");
        command.add(localDir.toAbsolutePath().toString());
        if (checkOnly) {
            command.add("--check-only");
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IOException("Failed to start model ensure script: " + String.join(" ", command), e);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[mlx-model] {}", line);
            }
        }

        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Model download interrupted", e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Model download timed out after " + timeout);
        }

        if (process.exitValue() != 0) {
            throw new IOException("Model ensure script failed with exit code " + process.exitValue());
        }
    }
}
