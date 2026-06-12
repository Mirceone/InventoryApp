package com.mirceone.inventoryapp.service.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mlx", matchIfMissing = true)
public class MlxCommandResolver {

    private final AppIntegrationProperties props;
    private List<String> pythonPrefix = List.of("python3");
    private Path ensureModelScript = Path.of("scripts", "ensure_mlx_model.py");

    public MlxCommandResolver(AppIntegrationProperties props) {
        this.props = props;
    }

    @PostConstruct
    void resolveAtStartup() {
        pythonPrefix = resolvePython(props.getAi().getMlxPythonCommand());
        ensureModelScript = resolveScript(props.getAi().getEnsureModelScriptPath());
    }

    public List<String> pythonPrefix() {
        return pythonPrefix;
    }

    public Path ensureModelScript() {
        return ensureModelScript;
    }

    public Path modelCacheDir() {
        return resolveModelCacheDir(props.getAi());
    }

    /**
     * Model id sent to MLX OpenAI API. Must match the path used when starting mlx_vlm.server
     * to avoid reloading weights on every request.
     */
    public String resolvedApiModelId() {
        return resolveApiModelId(props.getAi(), modelCacheDir());
    }

    static String resolveApiModelId(AppIntegrationProperties.Ai ai, Path cacheDir) {
        String configured = ai.getModel();
        if (configured != null && !configured.isBlank()) {
            Path path = Path.of(configured.trim());
            if (path.isAbsolute() && Files.isDirectory(path)) {
                return path.toAbsolutePath().toString();
            }
        }
        return cacheDir.toAbsolutePath().toString();
    }

    public boolean isEnsureScriptAvailable() {
        return Files.isExecutable(Path.of(pythonPrefix.getFirst()))
                && Files.isRegularFile(ensureModelScript);
    }

    static List<String> resolvePython(String configured) {
        if (configured != null && !configured.isBlank() && !"python3".equals(configured.trim())) {
            return List.of(configured.trim());
        }
        Path venvPython = Path.of(System.getProperty("user.dir"), ".venv-mlx", "bin", "python3");
        if (Files.isExecutable(venvPython)) {
            return List.of(venvPython.toAbsolutePath().toString());
        }
        return List.of("python3");
    }

    static Path resolveScript(String configured) {
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim());
        }
        return Path.of(System.getProperty("user.dir"), "scripts", "ensure_mlx_model.py");
    }

    static Path resolveModelCacheDir(AppIntegrationProperties.Ai ai) {
        if (ai.getModelCacheDir() != null && !ai.getModelCacheDir().isBlank()) {
            return Path.of(ai.getModelCacheDir().trim());
        }
        String slug = ai.getHuggingfaceRepo().replace('/', '-');
        return Path.of(System.getProperty("user.dir"), ".mlx-models", slug);
    }

    static boolean probePythonModule(List<String> pythonPrefix, String module) {
        List<String> command = new ArrayList<>(pythonPrefix);
        command.add("-c");
        command.add("import " + module);
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
