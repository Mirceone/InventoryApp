package com.mirceone.inventoryapp.service.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mlx", matchIfMissing = true)
public class MlxServerManager {

    private static final Logger log = LoggerFactory.getLogger(MlxServerManager.class);

    private final AppIntegrationProperties props;
    private final MlxCommandResolver commandResolver;
    private Process serverProcess;

    public MlxServerManager(AppIntegrationProperties props, MlxCommandResolver commandResolver) {
        this.props = props;
        this.commandResolver = commandResolver;
    }

    public boolean isManagedProcessRunning() {
        return serverProcess != null && serverProcess.isAlive();
    }

    /**
     * Starts mlx_vlm.server when enabled and not already reachable.
     */
    public void startServerIfNeeded(Path modelDir) throws IOException, InterruptedException {
        AppIntegrationProperties.Ai ai = props.getAi();
        if (!ai.isAutoStartServer()) {
            return;
        }

        String modelsUrl = modelsUrl(ai);
        if (AiStartupValidator.probeModels(modelsUrl, ai.getApiKey())) {
            log.info("MLX OpenAI server already reachable at {}", ai.getBaseUrl());
            return;
        }

        if (!MlxCommandResolver.probePythonModule(commandResolver.pythonPrefix(), "mlx_vlm")) {
            throw new IOException(
                    "Cannot start MLX server: mlx_vlm not installed for "
                            + commandResolver.pythonPrefix().getFirst()
                            + ". Run: pip install mlx-vlm");
        }

        int port = resolvePort(ai);
        List<String> command = new ArrayList<>(commandResolver.pythonPrefix());
        command.add("-m");
        command.add("mlx_vlm.server");
        command.add("--port");
        command.add(String.valueOf(port));
        command.add("--model");
        command.add(modelDir.toAbsolutePath().toString());

        log.info("Starting MLX OpenAI server: {}", String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        try {
            serverProcess = builder.start();
        } catch (IOException e) {
            throw new IOException("Failed to start MLX server: " + String.join(" ", command), e);
        }

        waitUntilReachable(modelsUrl, ai.getApiKey(), ai.getServerStartTimeout());
        log.info("MLX OpenAI server is ready at {} (model={})", ai.getBaseUrl(), ai.getModel());
    }

    @PreDestroy
    void shutdownManagedServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            log.info("Stopping managed MLX OpenAI server (pid={})", serverProcess.pid());
            serverProcess.destroy();
            try {
                if (!serverProcess.waitFor(15, TimeUnit.SECONDS)) {
                    serverProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                serverProcess.destroyForcibly();
            }
        }
    }

    static String modelsUrl(AppIntegrationProperties.Ai ai) {
        return ai.getBaseUrl().replaceAll("/$", "") + "/models";
    }

    static int resolvePort(AppIntegrationProperties.Ai ai) {
        if (ai.getServerPort() > 0) {
            return ai.getServerPort();
        }
        try {
            URI uri = URI.create(ai.getBaseUrl());
            if (uri.getPort() > 0) {
                return uri.getPort();
            }
            return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        } catch (Exception e) {
            return 8000;
        }
    }

    static void waitUntilReachable(String modelsUrl, String apiKey, Duration timeout)
            throws InterruptedException, IOException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (AiStartupValidator.probeModels(modelsUrl, apiKey)) {
                return;
            }
            TimeUnit.SECONDS.sleep(2);
        }
        throw new IOException("MLX server did not become reachable within " + timeout);
    }
}
